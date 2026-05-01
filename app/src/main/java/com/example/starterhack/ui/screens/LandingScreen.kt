package com.example.starterhack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.starterhack.ui.ModelVariant
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.starterhack.navigation.Routes
import com.example.starterhack.ui.RedactionUiState
import com.example.starterhack.ui.RedactionViewModel
import com.example.starterhack.ui.components.HudBox
import com.example.starterhack.ui.components.ZeroTrustBadge
import com.example.starterhack.ui.theme.ShieldBackground
import com.example.starterhack.ui.theme.ShieldCyan
import com.example.starterhack.ui.theme.ShieldSurface

@Composable
fun LandingScreen(viewModel: RedactionViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val lastSuccess = uiState as? RedactionUiState.Success
    val selectedVariant by viewModel.selectedVariant.collectAsState()
    var showModelMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShieldBackground)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // Wordmark
            Text(
                text = "ShieldText",
                style = MaterialTheme.typography.displaySmall,
                color = ShieldCyan,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Zero-trust on-device PII redaction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(48.dp))

            // Mode cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ModeCard(
                    icon = Icons.AutoMirrored.Filled.TextSnippet,
                    title = "Text",
                    subtitle = "Paste or type content",
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.TEXT) },
                )
                ModeCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Image",
                    subtitle = "Scan documents",
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.SCANNER) },
                )
            }

            Spacer(Modifier.height(24.dp))

            // Model variant selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Model:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showModelMenu = true },
                        label = {
                            Text(
                                text = selectedVariant.label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShieldCyan.copy(alpha = 0.15f),
                            selectedLabelColor = ShieldCyan,
                        ),
                    )
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                    ) {
                        ModelVariant.entries.forEach { variant ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(variant.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            variant.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectModelVariant(variant)
                                    showModelMenu = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ZeroTrustBadge(
                backend = (uiState as? RedactionUiState.Idle)?.let { "" }
                    ?: lastSuccess?.backend ?: "",
            )

            // HUD with last run metrics if available
            lastSuccess?.metrics?.let { metrics ->
                Spacer(Modifier.height(12.dp))
                HudBox(
                    metrics = metrics,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .aspectRatio(0.88f)
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = ShieldSurface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = ShieldCyan.copy(alpha = 0.10f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ShieldCyan,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
