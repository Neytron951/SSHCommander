package com.neytron.sshcommander

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.ui.*
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import kotlinx.coroutines.flow.first

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

            CompositionLocalProvider(
                LocalAppDeps provides AppDeps(
                    repository = serverRepository,
                    settings = settingsManager,
                    biometric = AndroidBiometricAuthenticator(this)
                )
            ) {
                SSHCommanderTheme(darkTheme = darkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
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
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sync the string catalog with the persisted language.
    LaunchedEffect(Unit) {
        AppStrings.language = settings.language.first()
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
                    onServerClick = { serverId -> navController.navigate("server_control/$serverId") },
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
                route = "server_control/{serverId}",
                arguments = listOf(navArgument("serverId") { type = NavType.IntType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                ServerControlScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onManageCommands = { navController.navigate("manage_commands") },
                    onManageLogins = { navController.navigate("manage_logins/$serverId") },
                    onNavigateToSftp = { navController.navigate("sftp_explorer/$serverId") }
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
                route = "sftp_explorer/{serverId}",
                arguments = listOf(navArgument("serverId") { type = NavType.IntType })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getInt("serverId") ?: -1
                SftpExplorerScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() },
                    onManageLogins = { navController.navigate("manage_logins/$serverId") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onWidgetSettingsClick = { navController.navigate("widget_settings") },
                    onAboutClick = { navController.navigate("about") },
                    appVersion = appVersion
                )
            }
            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    appVersion = appVersion
                )
            }
            composable("manage_commands") {
                ManageCommandsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("widget_settings") {
                WidgetSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        // Widget "open server" intent: jump straight to the control screen.
        LaunchedEffect(initialServerId) {
            if (initialServerId != null) {
                navController.navigate("server_control/$initialServerId") {
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
    }
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
