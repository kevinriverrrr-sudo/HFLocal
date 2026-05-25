package com.hflocal.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(nav: NavController) {
    val viewModel: AuthViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    // Navigate to catalog on successful login
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            nav.navigate(Screen.Catalog.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // App title
        Text(
            "HF Local",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = HFColors.OnBackground
        )

        Spacer(Modifier.height(32.dp))

        // Tabs
        TabRow(
            selectedTabIndex = state.activeTab,
            containerColor = HFColors.Surface,
            contentColor = HFColors.Primary,
            divider = {}
        ) {
            Tab(
                selected = state.activeTab == 0,
                onClick = { viewModel.setTab(0) }
            ) {
                Text(
                    "Token",
                    color = if (state.activeTab == 0) HFColors.Primary
                           else HFColors.OnSurfaceMuted
                )
            }
            Tab(
                selected = state.activeTab == 1,
                onClick = { viewModel.setTab(1) }
            ) {
                Text(
                    "Browser",
                    color = if (state.activeTab == 1) HFColors.Primary
                           else HFColors.OnSurfaceMuted
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (state.activeTab == 0) {
            TokenLoginTab(
                state = state,
                onTokenChange = { viewModel.updateToken(it) },
                onToggleShowToken = { viewModel.toggleShowToken() },
                onLogin = { viewModel.login() }
            )
        } else {
            BrowserLoginTab(onContinue = {
                nav.navigate(Screen.Catalog.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }

        Spacer(Modifier.weight(1f))

        // Continue without account
        TextButton(onClick = {
            nav.navigate(Screen.Catalog.route) {
                popUpTo(0) { inclusive = true }
            }
        }) {
            Text(
                "Continue without account",
                color = HFColors.OnSurfaceMuted,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenLoginTab(
    state: AuthUiState,
    onTokenChange: (String) -> Unit,
    onToggleShowToken: () -> Unit,
    onLogin: () -> Unit
) {
    OutlinedTextField(
        value = state.token,
        onValueChange = onTokenChange,
        label = { Text("HF Access Token") },
        placeholder = { Text("hf_xxx...") },
        visualTransformation = if (state.showToken) VisualTransformation.None
                               else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShowToken) {
                Icon(
                    if (state.showToken) Icons.Default.VisibilityOff
                    else Icons.Default.Visibility,
                    contentDescription = if (state.showToken) "Hide token" else "Show token",
                    tint = HFColors.OnSurfaceMuted
                )
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HFColors.OnBackground,
            unfocusedTextColor = HFColors.OnBackground,
            focusedBorderColor = HFColors.Primary,
            unfocusedBorderColor = HFColors.Divider,
            focusedContainerColor = HFColors.Surface,
            unfocusedContainerColor = HFColors.Surface
        ),
        enabled = !state.isLoading
    )

    // Error message
    if (state.error != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            state.error,
            color = HFColors.Error,
            fontSize = 12.sp
        )
    }

    // Success state
    if (state.user != null) {
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = HFColors.Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Logged in as ${state.user!!.name}",
                        color = HFColors.OnBackground,
                        fontWeight = FontWeight.Medium
                    )
                    if (state.user!!.fullname != null) {
                        Text(
                            state.user!!.fullname!!,
                            color = HFColors.OnSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = onLogin,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HFColors.Primary),
        enabled = !state.isLoading && state.token.isNotBlank()
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = HFColors.OnBackground,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Login")
        }
    }
}

@Composable
private fun BrowserLoginTab(onContinue: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.OpenInBrowser,
                contentDescription = null,
                tint = HFColors.Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Login via HuggingFace",
                color = HFColors.OnBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Go to huggingface.co, create a token, and paste it in the Token tab.",
                color = HFColors.OnSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HFColors.Primary)
            ) {
                Text("Continue")
            }
        }
    }
}
