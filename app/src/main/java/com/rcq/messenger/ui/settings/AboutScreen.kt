package com.rcq.messenger.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rcq.messenger.BuildConfig
import com.rcq.messenger.R
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.RCQMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val rcq = LocalRCQColors.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About RCQ", color = rcq.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = rcq.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = rcq.bgPrimary)
            )
        },
        containerColor = rcq.bgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(RCQMetrics.screenHPad * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.rcq_logo),
                contentDescription = "RCQ",
                modifier = Modifier.size(RCQMetrics.avatarLg * 2.2f)
            )
            Spacer(Modifier.height(RCQMetrics.rowHPad * 2))
            Text(
                "RCQ Messenger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = rcq.textPrimary
            )
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                color = rcq.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(RCQMetrics.rowHPad * 3))
            Text(
                "Privacy-first messaging.\nNo phone number required.\nEnd-to-end encrypted.",
                textAlign = TextAlign.Center,
                color = rcq.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(RCQMetrics.rowHPad * 3))
            OutlinedButton(
                onClick = { uriHandler.openUri("https://rcq.app") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("rcq.app", color = rcq.accent)
            }
            Spacer(Modifier.height(RCQMetrics.rowHPad))
            OutlinedButton(
                onClick = { uriHandler.openUri("https://github.com/rcq-messenger") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GitHub", color = rcq.accent)
            }
        }
    }
}
