package com.avinal.memos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
) {
    val viewModel = viewModel { LoginViewModel(deps.authRepository) }
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp),
    ) {
        Text(
            text = "sign in",
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(36.dp))

        TextField(
            value = uiState.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("server url", fontSize = 13.sp) },
            placeholder = { Text("memos.example.com", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
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

        Spacer(Modifier.height(18.dp))

        TextField(
            value = uiState.token,
            onValueChange = viewModel::updateToken,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("access token", fontSize = 13.sp) },
            placeholder = { Text("memos_pat_...", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
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
            Spacer(Modifier.height(12.dp))
            Text(
                uiState.error!!,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (uiState.isLoading) accent.copy(alpha = 0.6f) else accent)
                .then(if (!uiState.isLoading) Modifier.clickable(onClick = viewModel::login) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("sign in", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
