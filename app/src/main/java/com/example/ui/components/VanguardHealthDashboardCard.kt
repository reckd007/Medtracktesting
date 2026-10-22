package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

/**
 * Embedded Vanguard Health Web Suite WebView Component.
 * Loads the standalone single-page web app (vanguard_health.html) featuring
 * multi-profile management, Web Audio custom ringtones, stockpile exhaustion warnings,
 * interactive canvas adherence graph, and daily vitals logging in a warm earthy palette.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VanguardHealthDashboardCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vanguard_health_web_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Vanguard Health Suite",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2C2522)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF4EDE2),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4))
                            )
                        ) {
                            Text(
                                text = "Earthy Palette",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB87355),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Full web application suite with multi-profile, Web Audio ringtones, and vitals",
                        fontSize = 12.sp,
                        color = Color(0xFF6B5D56)
                    )
                }
            }

            // Embedded WebView Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFDFBF7))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            webViewClient = WebViewClient()
                            loadUrl("file:///android_asset/vanguard_health.html")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
