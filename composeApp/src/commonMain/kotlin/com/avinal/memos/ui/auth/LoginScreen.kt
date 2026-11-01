package com.avinal.memos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.ui.theme.LocalAccentColor

@Composable
fun LoginScreen(
    deps: AppDependencies,
    onLoginSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val viewModel = viewModel { LoginViewModel(deps.authRepository) }
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "sign in",
            fontSize = 42.sp,
            fontWeight = FontWeight.Thin,
            color = textColor,
            letterSpacing = (-1).sp,
        )

        Spacer(Modifier.height(32.dp))

        TextField(
            value = uiState.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("server url", fontSize = 13.sp) },
            placeholder = {
                Text("memos.example.com", fontSize = 15.sp, color = subtleColor.copy(alpha = 0.4f))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = accent,
            ),
        )

        Spacer(Modifier.height(16.dp))

        TextField(
            value = uiState.token,
            onValueChange = viewModel::updateToken,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("access token", fontSize = 13.sp) },
            placeholder = {
                Text("memos_pat_...", fontSize = 15.sp, color = subtleColor.copy(alpha = 0.4f))
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = accent,
            ),
        )

        if (uiState.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(uiState.error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Text(
                    "back",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Light,
                    color = subtleColor,
                    modifier = Modifier.clickable { onBack() },
                )
            }

            Spacer(Modifier.weight(1f))

            if (uiState.isLoading) {
                CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
            } else {
                Text(
                    "login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = if (uiState.serverUrl.isNotBlank() && uiState.token.isNotBlank()) accent else subtleColor,
                    modifier = Modifier.then(
                        if (uiState.serverUrl.isNotBlank() && uiState.token.isNotBlank())
                            Modifier.clickable { viewModel.login() }
                        else Modifier,
                    ),
                )
            }
        }
    }
}
