package com.neytron.sshcommander

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neytron.sshcommander.data.ExportImportManager
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.data.TerminalScreenStore
import com.neytron.sshcommander.ui.*
import com.neytron.sshcommander.ui.theme.ModernAndroidEra
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import com.neytron.sshcommander.ui.theme.Windows9xEra
import com.neytron.sshcommander.ui.theme.Windows10Era
import com.neytron.sshcommander.ui.theme.Windows10DarkEra
import com.neytron.sshcommander.ui.AboutScreen
import com.neytron.sshcommander.ui.platformToast
import com.neytron.sshcommander.ui.mascotPainter
import androidx.compose.ui.zIndex
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SshCommanderApplication
        val serverRepository = app.serverRepository
        val settingsManager = SettingsManager(this)

        // Widget "Open" actions pass a serverId so the app opens that server directly.
        val initialServerId = intent?.getIntExtra("serverId", -1)?.takeIf { it > 0 }

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState(initial = "system")
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            RequestNotificationPermissionIfNeeded()

            val versionName = remember { packageManager.getPackageInfo(packageName, 0).versionName ?: "" }

            val authManager = remember { com.neytron.sshcommander.sync.createAuthManager(this) }
            val httpClient = remember {
                HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
                    }
                }
            }

            val uiStyleName by settingsManager.uiStyle.collectAsState(initial = "modern")
            val era = when (uiStyleName) {
                "win9x" -> Windows9xEra
                "win10" -> Windows10Era
                "win10dark" -> Windows10DarkEra
                else -> ModernAndroidEra
            }

            CompositionLocalProvider(
                LocalAppDeps provides AppDeps(
                    repository = serverRepository,
                    settings = settingsManager,
                    biometric = AndroidBiometricAuthenticator(this),
                    authManager = authManager,
                    httpClient = httpClient
                )
            ) {
                SSHCommanderTheme(darkTheme = darkTheme, era = era) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = if (era is Windows9xEra || era is Windows10Era || era is Windows10DarkEra) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PhoneApp(
                            initialServerId = initialServerId,
                            appVersion = versionName
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneApp(
    initialServerId: Int?,
    appVersion: String
) {
    val deps = LocalAppDeps.current
    val settings = deps.settings
    val navController = rememberNavController()

    val biometricLockEnabled by settings.biometricLock.collectAsState(initial = false)

    // Biometric app lock: lock on first launch and re-lock whenever the app
    // returns from the background. Overlaid on top of the nav graph so the
    // user's place is preserved across an unlock.
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var lockTrigger by remember { mutableIntStateOf(0) }
    var showWebbyAnimation by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sync the string catalog with the persisted language.
    val language by settings.language.collectAsState(initial = "en")
    LaunchedEffect(language) {
        AppStrings.language = language
    }

    // On cold start, lock if the feature is enabled (waits for the real
    // DataStore value rather than the async "false" initial).
    LaunchedEffect(Unit) {
        if (settings.biometricLock.first()) {
            isLocked = true
        }
    }

    // When the user turns the feature off from settings, unlock immediately.
    LaunchedEffect(biometricLockEnabled) {
        if (!biometricLockEnabled) isLocked = false
    }

    DisposableEffect(lifecycleOwner, biometricLockEnabled) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (biometricLockEnabled) isLocked = true
                }
                // Re-trigger the biometric prompt every time the app returns to
                // the foreground (screen off/on). Android dismisses the prompt
                // when the activity stops, so we must re-show it on resume.
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (isLocked) lockTrigger++
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "server_list") {
            composable("server_list") {
                ServerListScreen(
                    onAddServer = { navController.navigate("add_edit_server") },
                    onEditServer = { serverId -> navController.navigate("add_edit_server?serverId=$serverId") },
                    onServerClick = { serverId ->
                        // Reuse an open session for the server, or start a new one.
                        val sessionId = TerminalScreenStore.findForServer(serverId)
                            ?: TerminalScreenStore.createSession(serverId)
                        navController.navigate("server_control/$serverId/$sessionId") {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable(
                route = "add_edit_server?serverId={serverId}",
                arguments = listOf(navArgument("serverId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                AddEditServerScreen(
                    serverId = if (serverId == -1) null else serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onManageLogins = if (serverId != -1) {
                        { navController.navigate("manage_logins/$serverId") }
                    } else null
                )
            }
            composable(
                route = "server_control/{serverId}/{sessionId}",
                arguments = listOf(
                    navArgument("serverId") { type = NavType.IntType },
                    navArgument("sessionId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                val sessionId = backStackEntry.arguments?.getInt("sessionId") ?: -1
                ServerControlScreen(
                    serverId = serverId,
                    sessionId = sessionId,
                    onNavigateBack = { navController.popBackStack() },
                    onManageCommands = { navController.navigate("manage_commands") },
                    onManageLogins = { navController.navigate("manage_logins/$serverId") },
                    onNavigateToSftp = { navController.navigate("sftp_explorer/$serverId/$sessionId") },
                    onSwitchSession = { sid ->
                        val targetServerId = TerminalScreenStore.serverOf(sid)
                        if (targetServerId != null) {
                            navController.navigate("server_control/$targetServerId/$sid") {
                                launchSingleTop = true
                                // Don't pop up to inclusive here, let the NavHost handle it
                                // or use a better strategy to avoid destroying VMs of background tabs
                            }
                        }
                    },
                    onAddSession = { targetServerId ->
                        val newSessionId = TerminalScreenStore.createSession(targetServerId)
                        navController.navigate("server_control/$targetServerId/$newSessionId") {
                            launchSingleTop = true
                        }
                    },
                    onCloseSession = { sid ->
                        // Managed by SshViewModel and SessionManager
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "manage_logins/{serverId}",
                arguments = listOf(navArgument("serverId") { type = NavType.IntType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                ManageLoginsScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "sftp_explorer/{serverId}/{sessionId}",
                arguments = listOf(
                    navArgument("serverId") { type = NavType.IntType },
                    navArgument("sessionId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                val sessionId = backStackEntry.arguments?.getInt("sessionId") ?: -1
                SftpExplorerScreen(
                    serverId = serverId,
                    sessionId = sessionId,
                    onNavigateBack = { navController.popBackStack() },
                    onManageLogins = { navController.navigate("manage_logins/$serverId") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onWidgetSettingsClick = { navController.navigate("widget_settings") },
                    onSshKeysClick = { navController.navigate("ssh_keys") },
                    onAboutClick = { navController.navigate("about") },
                    onScriptMarketClick = { navController.navigate("script_market") },
                    appVersion = appVersion
                )
            }
            composable("ssh_keys") {
                SshKeyManagerScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    appVersion = appVersion,
                    onMascotTrigger = {
                        showWebbyAnimation = true
                        platformToast("Webby is coming!")
                    }
                )
            }
            composable("manage_commands") {
                ManageCommandsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("widget_settings") {
                WidgetSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("script_market") {
                ScriptMarketScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExecuteScript = { cmd ->
                        // This logic needs to handle jump-to-server or current session
                        // For now, let's just toast or logic to be refined
                        navController.popBackStack()
                    }
                )
            }
        }

        // Widget "open server" intent: jump straight to the control screen.
        LaunchedEffect(initialServerId) {
            if (initialServerId != null) {
                val sessionId = TerminalScreenStore.findForServer(initialServerId)
                    ?: TerminalScreenStore.createSession(initialServerId)
                navController.navigate("server_control/$initialServerId/$sessionId") {
                    launchSingleTop = true
                }
            }
        }

        // Biometric lock overlay sits on top of everything.
        if (isLocked) {
            BiometricLockScreen(
                triggerKey = lockTrigger,
                onUnlocked = { isLocked = false }
            )
        }

        // First-run guide: welcome → language → tab tour (or JSON import).
        val scope = rememberCoroutineScope()
        val importPicker = rememberUploadPicker { files ->
            files.firstOrNull()?.let { f ->
                scope.launch {
                    try {
                        val text = f.openInput()?.let { readText(it) } ?: ""
                        deps.repository.let { ExportImportManager(it).importJson(text) }
                        platformToast(AppStrings.importSuccess)
                    } catch (e: Exception) {
                        platformToast(String.format(AppStrings.errorPrefix, e.message ?: ""))
                    }
                }
            }
        }
        OnboardingGate(
            settings = settings,
            tourSteps = androidTourSteps(),
            onImportJson = { importPicker() }
        )

        if (showWebbyAnimation) {
            Box(modifier = Modifier.fillMaxSize().zIndex(999f)) {
                WebbyAnimationOverlay(onFinished = { showWebbyAnimation = false })
            }
        }
    }
}

@Composable
fun WebbyAnimationOverlay(onFinished: () -> Unit) {
    val painter = mascotPainter()
    if (painter == null) {
        onFinished()
        return
    }

    val spiderCount = 15
    val duration = 4000
    
    // LaunchedEffect to dismiss after time
    LaunchedEffect(Unit) {
        delay(duration.toLong())
        onFinished()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        
        repeat(spiderCount) { index ->
            WebbySpider(screenWidth, screenHeight, painter)
        }
    }
}

@Composable
private fun WebbySpider(screenWidth: Dp, screenHeight: Dp, painter: Painter) {
    val animProgress = remember { Animatable(0f) }
    val startSide = remember { (0..3).random() } // 0: Top, 1: Right, 2: Bottom, 3: Left
    
    val startX = remember(startSide, screenWidth) {
        val w = screenWidth.value.toInt().coerceAtLeast(1)
        when(startSide) {
            1 -> screenWidth
            3 -> 0.dp
            else -> (0..w).random().dp
        }
    }
    val startY = remember(startSide, screenHeight) {
        val h = screenHeight.value.toInt().coerceAtLeast(1)
        when(startSide) {
            0 -> 0.dp
            2 -> screenHeight
            else -> (0..h).random().dp
        }
    }
    
    val endX = remember(screenWidth) {
        val w = screenWidth.value.toInt().coerceAtLeast(1)
        (0..w).random().dp
    }
    val endY = remember(screenHeight) {
        val h = screenHeight.value.toInt().coerceAtLeast(1)
        (0..h).random().dp
    }
    
    LaunchedEffect(Unit) {
        delay((0..1000).random().toLong())
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = (2000..4000).random(),
                easing = LinearEasing
            )
        )
    }

    val currentX = lerp(startX, endX, animProgress.value)
    val currentY = lerp(startY, endY, animProgress.value)
    
    // Вычисляем угол направления движения паука для Android
    val rotation = remember(startX, endX, startY, endY) {
        val dx = endX.value - startX.value
        val dy = endY.value - startY.value
        val angleInDegrees = atan2(dy, dx) * (180 / PI)
        (angleInDegrees + 90).toFloat() // +90 градусов коррекции для взгляда вверх
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .offset(currentX, currentY)
            .size(48.dp)
            .rotate(rotation)
    )
}

/** Reads the whole [PlatformInputStream] into a UTF-8 string and closes it. */
private fun readText(input: PlatformInputStream): String {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (true) {
        val n = input.read(chunk, 0, chunk.size)
        if (n <= 0) break
        buffer.write(chunk, 0, n)
    }
    input.close()
    return String(buffer.toByteArray(), Charsets.UTF_8)
}

@Composable
private fun RequestNotificationPermissionIfNeeded() {
    val context = LocalContext.current
    var hasRequested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result ignored */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED &&
            !hasRequested
        ) {
            hasRequested = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
