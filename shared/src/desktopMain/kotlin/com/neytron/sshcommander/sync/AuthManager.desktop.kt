package com.neytron.sshcommander.sync

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.URI

@Serializable
private data class TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val scope: String? = null,
    val token_type: String
)

@Serializable
private data class UserInfo(
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null
)

class DesktopAuthManager(
    private val secureStorage: com.neytron.sshcommander.data.SecureStorage? = null
) : AuthManager {
    // These values are injected from local.properties during build via generated Secrets object
    private val clientId = com.neytron.sshcommander.Secrets.GOOGLE_CLIENT_ID
    private val clientSecret = com.neytron.sshcommander.Secrets.GOOGLE_CLIENT_SECRET
    private val redirectUri = "http://localhost:5757"
    private val scope = "https://www.googleapis.com/auth/drive.appdata email profile"

    private val _userEmail = MutableStateFlow<String?>(null)
    override val userEmail: StateFlow<String?> = _userEmail

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized

    private var accessToken: String? = null
    private var refreshToken: String? = null
    
    private val httpClient = HttpClient(io.ktor.client.engine.java.Java) {
        install(ContentNegotiation) { 
            json(Json { ignoreUnknownKeys = true }) 
        }
    }

    init {
        // Try to restore session from secure storage
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            val savedEmail = secureStorage?.get("google_user_email")
            val savedRefreshToken = secureStorage?.get("google_refresh_token")
            
            if (savedEmail != null && savedRefreshToken != null) {
                _userEmail.value = savedEmail
                refreshToken = savedRefreshToken
                _isAuthorized.value = true
                
                // Try to refresh token immediately to verify
                refreshAccessToken()
            }
        }
    }

    override suspend fun signIn(): Boolean {
        val authCodeDeferred = CompletableDeferred<String>()
        
        val server = embeddedServer(Netty, port = 5757) {
            routing {
                get("/") {
                    val code = call.request.queryParameters["code"]
                    val error = call.request.queryParameters["error"]
                    if (code != null) {
                        authCodeDeferred.complete(code)
                        call.respondText("Authentication successful! You can close this window.")
                    } else {
                        val errorMsg = error ?: "Unknown error"
                        authCodeDeferred.completeExceptionally(Exception(errorMsg))
                        call.respondText("Authentication failed: $errorMsg. You can close this window.")
                    }
                }
            }
        }.start(wait = false)

        try {
            val encodedScope = java.net.URLEncoder.encode(scope, "UTF-8")
            val encodedRedirect = java.net.URLEncoder.encode(redirectUri, "UTF-8")
            
            // Add access_type=offline to get a refresh_token
            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$clientId&" +
                    "redirect_uri=$encodedRedirect&" +
                    "response_type=code&" +
                    "scope=$encodedScope&" +
                    "access_type=offline&" +
                    "prompt=consent"
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                Runtime.getRuntime().exec("xdg-open $authUrl")
            }
            
            // Wait for code with a 3-minute timeout
            val code = kotlinx.coroutines.withTimeout(180_000) {
                authCodeDeferred.await()
            }
            return exchangeCodeForToken(code)
        } catch (e: Exception) {
            println("Sign-in process failed or cancelled: ${e.message}")
            return false
        } finally {
            // Give Ktor a moment to send the response before stopping
            kotlinx.coroutines.delay(1000)
            server.stop(500, 1000)
        }
    }

    private suspend fun exchangeCodeForToken(code: String): Boolean {
        return try {
            val response: TokenResponse = httpClient.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("code", code)
                    append("grant_type", "authorization_code")
                    append("redirect_uri", redirectUri)
                }))
            }.body()
            
            accessToken = response.access_token
            if (response.refresh_token != null) {
                refreshToken = response.refresh_token
                secureStorage?.put("google_refresh_token", response.refresh_token)
            }
            
            fetchUserEmail(response.access_token)
            _isAuthorized.value = true
            true
        } catch (e: Exception) {
            println("Token exchange failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun refreshAccessToken(): Boolean {
        val currentRefreshToken = refreshToken ?: return false
        return try {
            val response: TokenResponse = httpClient.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("refresh_token", currentRefreshToken)
                    append("grant_type", "refresh_token")
                }))
            }.body()
            
            accessToken = response.access_token
            // Some providers return a new refresh token, update if present
            if (response.refresh_token != null) {
                refreshToken = response.refresh_token
                secureStorage?.put("google_refresh_token", response.refresh_token)
            }
            
            _isAuthorized.value = true
            true
        } catch (e: Exception) {
            println("Token refresh failed: ${e.message}")
            // If refresh fails, we might need to sign in again
            _isAuthorized.value = false
            false
        }
    }

    private suspend fun fetchUserEmail(token: String) {
        try {
            val response: UserInfo = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            val email = response.email ?: "Google User"
            _userEmail.value = email
            secureStorage?.put("google_user_email", email)
        } catch (e: Exception) {
            println("Failed to fetch user email: ${e.message}")
            if (_userEmail.value == null) _userEmail.value = "Google User"
        }
    }

    override suspend fun signOut() {
        accessToken = null
        refreshToken = null
        secureStorage?.remove("google_refresh_token")
        secureStorage?.remove("google_user_email")
        _isAuthorized.value = false
        _userEmail.value = null
    }

    override suspend fun getAccessToken(): String? {
        if (accessToken == null && refreshToken != null) {
            refreshAccessToken()
        }
        return accessToken
    }
}

actual fun createAuthManager(
    context: Any?,
    secureStorage: com.neytron.sshcommander.data.SecureStorage?
): AuthManager = DesktopAuthManager(secureStorage)
