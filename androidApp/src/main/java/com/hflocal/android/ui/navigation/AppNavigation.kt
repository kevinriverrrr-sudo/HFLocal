package com.hflocal.android.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hflocal.android.ui.screens.auth.AuthScreen
import com.hflocal.android.ui.screens.catalog.CatalogScreen
import com.hflocal.android.ui.screens.catalog.ModelDetailScreen
import com.hflocal.android.ui.screens.chat.ChatScreen
import com.hflocal.android.ui.screens.device.DeviceInfoScreen
import com.hflocal.android.ui.screens.downloads.DownloadsScreen
import com.hflocal.android.ui.screens.models.MyModelsScreen
import com.hflocal.android.ui.screens.settings.SettingsScreen
import com.hflocal.android.ui.screens.splash.SplashScreen
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Catalog.route,
        Screen.MyModels.route,
        Screen.Downloads.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = HFColors.Surface) {
                    val tabs = listOf(
                        Triple(Screen.Catalog, "Catalog", Icons.Default.Explore),
                        Triple(Screen.MyModels, "Models", Icons.Default.ModelTraining),
                        Triple(Screen.Downloads, "Downloads", Icons.Default.Downloading),
                        Triple(Screen.Settings, "Settings", Icons.Default.Settings)
                    )

                    tabs.forEach { (screen, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                nav.navigate(screen.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HFColors.Primary,
                                selectedTextColor = HFColors.Primary,
                                unselectedIconColor = HFColors.OnSurfaceMuted,
                                unselectedTextColor = HFColors.OnSurfaceMuted,
                                indicatorColor = HFColors.Primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = HFColors.Background
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(nav)
            }
            composable(Screen.Auth.route) {
                AuthScreen(nav)
            }
            composable(Screen.Catalog.route) {
                CatalogScreen(nav)
            }
            composable(
                route = Screen.ModelDetail.route,
                arguments = listOf(navArgument("modelId") { type = NavType.StringType })
            ) { backStackEntry ->
                val modelId = Uri.decode(backStackEntry.arguments?.getString("modelId") ?: "")
                if (modelId.isBlank()) {
                    Text(
                        text = "Model not found",
                        modifier = Modifier.padding(16.dp),
                        color = HFColors.OnBackground
                    )
                } else {
                    ModelDetailScreen(nav, modelId)
                }
            }
            composable(Screen.MyModels.route) {
                MyModelsScreen(nav)
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen()
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("modelId") { type = NavType.StringType })
            ) { backStackEntry ->
                val modelId = Uri.decode(backStackEntry.arguments?.getString("modelId") ?: "")
                ChatScreen(nav, modelId)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(nav)
            }
            composable(Screen.DeviceInfo.route) {
                DeviceInfoScreen(nav)
            }
        }
    }
}
