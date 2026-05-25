package com.hflocal.shared.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
object HFColors {
    val Background=Color(0xFF0D0F18); val Surface=Color(0xFF13161F); val SurfaceVariant=Color(0xFF1E2030)
    val SurfaceElevated=Color(0xFF252839); val Primary=Color(0xFF7B6FEB); val PrimaryVariant=Color(0xFF5C4ED9)
    val Secondary=Color(0xFF4FC3F7); val OnBackground=Color(0xFFE8EAF0); val OnSurface=Color(0xFFC8CBD6)
    val OnSurfaceMuted=Color(0xFF7B7E8A); val UserMessageBg=Color(0xFF2C2F3E); val AssistantMessageBg=Color(0xFF1E2030)
    val Divider=Color(0xFF2A2D3E); val Error=Color(0xFFFF5370); val Success=Color(0xFF42FF71); val Warning=Color(0xFFFFB347)
}
private val Dark = darkColorScheme(primary=HFColors.Primary, onPrimary=Color.White, secondary=HFColors.Secondary, background=HFColors.Background, onBackground=HFColors.OnBackground, surface=HFColors.Surface, onSurface=HFColors.OnSurface, surfaceVariant=HFColors.SurfaceVariant, outline=HFColors.Divider, error=HFColors.Error)
@Composable fun HFLocalTheme(dark: Boolean = true, content: @Composable () -> Unit) { MaterialTheme(colorScheme = Dark, content = content) }
