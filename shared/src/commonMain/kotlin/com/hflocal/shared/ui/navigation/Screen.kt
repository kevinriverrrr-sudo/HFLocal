package com.hflocal.shared.ui.navigation
sealed class Screen(val route: String) {
    data object Splash : Screen("splash"); data object Auth : Screen("auth"); data object Catalog : Screen("catalog")
    data object ModelDetail : Screen("model_detail/{modelId}") { fun createRoute(id: String)="model_detail/$id" }
    data object MyModels : Screen("my_models"); data object Downloads : Screen("downloads")
    data object Chat : Screen("chat/{modelId}") { fun createRoute(id: String)="chat/$id" }
    data object Settings : Screen("settings"); data object DeviceInfo : Screen("device_info")
}
