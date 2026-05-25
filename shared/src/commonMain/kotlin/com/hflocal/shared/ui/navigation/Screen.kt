package com.hflocal.shared.ui.navigation

/**
 * Simple percent-encoder that handles slashes in model IDs.
 * Used instead of java.net.URLEncoder to stay compatible with Kotlin Multiplatform commonMain.
 */
private fun encodeRouteParam(value: String): String =
    value.replace("/", "%2F").replace("?", "%3F").replace("#", "%23")

sealed class Screen(val route: String) {
    data object Splash : Screen("splash"); data object Auth : Screen("auth"); data object Catalog : Screen("catalog")
    data object ModelDetail : Screen("model_detail?modelId={modelId}") {
        fun createRoute(id: String) = "model_detail?modelId=${encodeRouteParam(id)}"
    }
    data object MyModels : Screen("my_models"); data object Downloads : Screen("downloads")
    data object Chat : Screen("chat?modelId={modelId}") {
        fun createRoute(id: String) = "chat?modelId=${encodeRouteParam(id)}"
    }
    data object Settings : Screen("settings"); data object DeviceInfo : Screen("device_info")
}
