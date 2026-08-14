package com.neytron.sshcommander

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.ui.*
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import kotlinx.coroutines.flow.first

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            RequestNotificationPermissionIfNeeded()

            // Using the unified system font as requested (removing custom font logic)
            SSHCommanderTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SSHCommanderApp(this)
                }
            }
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

@Composable
fun SSHCommanderApp(activity: AppCompatActivity) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val biometricLockEnabled by settingsManager.biometricLock.collectAsState(initial = false)

    // Biometric app lock: lock on first launch and re-lock whenever the app
    // returns from the background. Overlaid on top of the nav graph so the
    // user's place is preserved across an unlock.
    var isLocked by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // On cold start, lock if the feature is enabled (waits for the real
    // DataStore value rather than the async "false" initial).
    LaunchedEffect(Unit) {
        if (settingsManager.biometricLock.first()) {
            isLocked = true
        }
    }

    // When the user turns the feature off from settings, unlock immediately.
    LaunchedEffect(biometricLockEnabled) {
        if (!biometricLockEnabled) isLocked = false
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, biometricLockEnabled) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && biometricLockEnabled) {
                isLocked = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
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
                activity = activity,
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
                onAboutClick = { navController.navigate("about") }
            )
        }
        composable("about") {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("manage_commands") {
            ManageCommandsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("widget_settings") {
            WidgetSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        }

        // Biometric lock overlay sits on top of everything.
        if (isLocked) {
            BiometricLockScreen(activity = activity, onUnlocked = { isLocked = false })
        }
    }
}
