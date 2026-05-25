package com.hflocal.android.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(nav: NavController) {
    // Fade-in animation for the logo
    val logoAlpha = remember { Animatable(0f) }
    val progress = remember { mutableFloatStateOf(0f) }
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Fade in logo over 800ms
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        // Animate progress bar
        for (i in 1..100) {
            progress.floatValue = i / 100f
            delay(12)
        }

        // Ensure minimum 1.5s splash duration
        delay(300)

        // Go directly to catalog — no auth required
        if (!hasNavigated) {
            hasNavigated = true
            nav.navigate(Screen.Catalog.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo with fade-in animation
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "HF Local Logo",
                tint = HFColors.Primary,
                modifier = Modifier
                    .size(96.dp)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "HF Local",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = HFColors.OnBackground,
                modifier = Modifier.alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Local AI on your device",
                fontSize = 14.sp,
                color = HFColors.OnSurfaceMuted,
                modifier = Modifier.alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Version
            Text(
                text = "v1.4.0",
                fontSize = 12.sp,
                color = HFColors.OnSurfaceMuted,
                modifier = Modifier.alpha(logoAlpha.value)
            )
        }

        // Thin progress bar at the bottom
        LinearProgressIndicator(
            progress = { progress.floatValue },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            color = HFColors.Primary,
            trackColor = HFColors.SurfaceVariant,
        )
    }
}
