package com.neytron.sshcommander.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScriptMarketService(private val httpClient: HttpClient) {
    
    private val MARKET_URL = "https://raw.githubusercontent.com/Neytron951/SSHC_Scripts/main/market.json"

    suspend fun fetchScripts(): Result<List<MarketScript>> = withContext(Dispatchers.Default) {
        try {
            val randomTag = (1..1000000).random()
            val urlWithNoCache = "$MARKET_URL?nocache=$randomTag"
            
            val response = httpClient.get(urlWithNoCache) {
                header("User-Agent", "SSHCommander-App")
                header("Accept", "application/json")
            }
            val jsonString = response.bodyAsText()
            
            if (jsonString.isBlank()) return@withContext Result.success(emptyList())
            
            val json = Json { 
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }
            val scripts = json.decodeFromString<List<MarketScript>>(jsonString)
            Result.success(scripts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
