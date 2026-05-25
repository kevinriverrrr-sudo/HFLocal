package com.hflocal.android.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hflocal.shared.domain.model.PerformanceTier
import com.hflocal.shared.domain.repository.IDeviceRepository
import com.hflocal.shared.ui.theme.HFColors
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(nav: NavController) {
    val context = LocalContext.current
    val deviceRepository: IDeviceRepository = koinInject()

    val viewModel: DeviceInfoViewModel = viewModel(
        factory = remember(deviceRepository) {
            DeviceInfoViewModelFactory(deviceRepository, context.applicationContext)
        }
    )
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Device Info",
                    color = HFColors.OnBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HFColors.OnBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (state.isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = HFColors.Primary)
            }
        } else if (state.error != null) {
            // Error state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = HFColors.Error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Detection Failed",
                    color = HFColors.OnBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error ?: "Unknown error",
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.refreshDevice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HFColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Rescan Device")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Performance Tier Card
                TierCard(
                    tier = state.tier,
                    maxModelSize = state.maxModelSize,
                    supportsVulkan = state.supportsVulkan
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CPU Info Card
                InfoCard(
                    title = "CPU",
                    icon = Icons.Default.Speed,
                    items = listOf(
                        "SoC" to state.deviceProfile.socModel,
                        "CPU" to state.cpuInfo,
                        "Architecture" to state.deviceProfile.cpuArch
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Memory Info Card
                InfoCard(
                    title = "Memory",
                    icon = Icons.Default.Memory,
                    items = listOf(
                        "Total RAM" to state.totalRam,
                        "Available RAM" to state.availableRam,
                        "Free Storage" to state.freeStorage,
                        "Total Storage" to state.totalStorage
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // GPU Info Card
                InfoCard(
                    title = "Graphics",
                    icon = Icons.Default.SdStorage,
                    items = listOf(
                        "GPU Renderer" to state.gpuRenderer,
                        "Vulkan" to if (state.supportsVulkan) "Supported" else "Not supported"
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Android Info Card
                InfoCard(
                    title = "Android",
                    icon = Icons.Default.Security,
                    items = listOf(
                        "Version" to state.androidVersion
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Rescan Button
                Button(
                    onClick = { viewModel.refreshDevice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HFColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Rescan Device")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TierCard(
    tier: PerformanceTier,
    maxModelSize: String,
    supportsVulkan: Boolean
) {
    val (tierLabel, tierColor, tierDescription) = when (tier) {
        PerformanceTier.TIER_1_HIGH_END -> Triple(
            "TIER 1 · HIGH-END",
            HFColors.Success,
            "Full support for large models"
        )
        PerformanceTier.TIER_2_MID_RANGE -> Triple(
            "TIER 2 · MID-RANGE",
            HFColors.Secondary,
            "Good support for medium models"
        )
        PerformanceTier.TIER_3_BUDGET -> Triple(
            "TIER 3 · BUDGET",
            HFColors.Warning,
            "Limited to smaller models"
        )
        PerformanceTier.TIER_4_UNSUPPORTED -> Triple(
            "TIER 4 · UNSUPPORTED",
            HFColors.Error,
            "This device may not run models well"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = tierColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Performance Tier",
                tint = tierColor,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tierLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tierColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tierDescription,
                color = HFColors.OnSurfaceMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Max model size: $maxModelSize",
                color = HFColors.OnSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (supportsVulkan) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (supportsVulkan) HFColors.Success else HFColors.Error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Vulkan ${if (supportsVulkan) "Supported" else "Not Available"}",
                    color = HFColors.OnSurfaceMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HFColors.Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HFColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = HFColors.Secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = key,
                        color = HFColors.OnSurfaceMuted,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = value,
                        color = HFColors.OnBackground,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}
