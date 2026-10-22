package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.HealthDiaryEntity
import com.example.ui.theme.*
import com.example.util.ChartShareHelper
import org.json.JSONArray
import org.json.JSONObject

class RechartsJsBridge(
    private val onShare: () -> Unit
) {
    @JavascriptInterface
    fun onShareRequested() {
        onShare()
    }
}

/**
 * Interactive 30-Day Health Analytics Dashboard displaying Mood and Symptom Frequency Trends.
 * Powered by Recharts (via WebView) with toggleable Native M3 Compose fallback and full
 * "Download & Share Chart" image export via Android's native share sheet.
 */
@Composable
fun RechartsDashboardCard(
    entries: List<HealthDiaryEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedViewMode by remember { mutableStateOf("Recharts") } // "Recharts" or "Native"
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Pre-calculate 30-day summaries
    val recent30Entries = remember(entries) {
        entries.take(30)
    }

    val moodScores = remember(recent30Entries) {
        recent30Entries.map {
            when (it.mood) {
                "Great" -> 5
                "Good" -> 4
                "Neutral" -> 3
                "Low" -> 2
                else -> 1
            }
        }
    }
    val avgMood = remember(moodScores) {
        if (moodScores.isNotEmpty()) String.format("%.1f", moodScores.average()) else "4.2"
    }

    val symptomFrequencies = remember(recent30Entries) {
        val map = mutableMapOf<String, Int>()
        recent30Entries.forEach { entry ->
            val list = entry.symptoms.split(",").map { it.trim() }.filter {
                it.isNotEmpty() && !it.contains("None", ignoreCase = true) &&
                        !it.contains("Feeling great", ignoreCase = true) &&
                        !it.contains("Well rested", ignoreCase = true)
            }
            list.forEach { s ->
                val name = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                map[name] = (map[name] ?: 0) + 1
            }
        }
        map.entries.sortedByDescending { it.value }.take(6)
    }

    val topSymptomName = remember(symptomFrequencies) {
        symptomFrequencies.firstOrNull()?.let { "${it.key} (${it.value}x)" } ?: "None reported"
    }

    // Convert entries to JSON for Recharts
    val entriesJsonString = remember(recent30Entries) {
        val array = JSONArray()
        recent30Entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("dateString", entry.dateString)
                put("timestamp", entry.timestamp)
                put("mood", entry.mood)
                put("painLevel", entry.painLevel)
                put("systolicBp", entry.systolicBp)
                put("diastolicBp", entry.diastolicBp)
                put("bloodSugar", entry.bloodSugar)
                put("weightKg", entry.weightKg)
                put("symptoms", entry.symptoms)
                put("notes", entry.notes)
            }
            array.put(obj)
        }
        array.toString()
    }

    // Push updated data to Recharts WebView whenever entries change
    LaunchedEffect(entriesJsonString, webViewRef) {
        webViewRef?.evaluateJavascript(
            "if (typeof window.updateDashboardData === 'function') { window.updateDashboardData('$entriesJsonString'); }",
            null
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title + Recharts Badge + Download & Share Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "30-Day Health Trends",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0F2FE)
                        ) {
                            Text(
                                text = "Recharts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Mood & Symptom Frequency Trends over 30 days",
                        fontSize = 12.sp,
                        color = MedTextSecondary
                    )
                }

                // Download & Share Button
                Button(
                    onClick = {
                        ChartShareHelper.shareHealthTrendsChart(context, recent30Entries)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("download_share_chart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download & Share",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick View Mode Selector: Recharts Web View vs Native M3 View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isRecharts = selectedViewMode == "Recharts"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRecharts) Color.White else Color.Transparent,
                    shadowElevation = if (isRecharts) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedViewMode = "Recharts" }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = if (isRecharts) MedTealPrimary else MedTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recharts Dashboard",
                            fontSize = 12.sp,
                            fontWeight = if (isRecharts) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRecharts) MedTealPrimary else MedTextSecondary
                        )
                    }
                }

                val isNative = selectedViewMode == "Native"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isNative) Color.White else Color.Transparent,
                    shadowElevation = if (isNative) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedViewMode = "Native" }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = if (isNative) MedTealPrimary else MedTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Native M3 View",
                            fontSize = 12.sp,
                            fontWeight = if (isNative) FontWeight.Bold else FontWeight.Medium,
                            color = if (isNative) MedTealPrimary else MedTextSecondary
                        )
                    }
                }
            }

            // Summary KPI Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Average Mood", fontSize = 10.sp, color = MedTextSecondary, fontWeight = FontWeight.Bold)
                        Text("$avgMood / 5.0", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MedTealDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Top Symptom", fontSize = 10.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                        Text(topSymptomName, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE).copy(alpha = 0.6f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFBAE6FD))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Adherence", fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                        Text("${recent30Entries.size} / 30 Days", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                    }
                }
            }

            // View Content
            if (selectedViewMode == "Recharts") {
                // Embedded Recharts WebView with responsive JavaScript bridge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(490.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT

                                addJavascriptInterface(
                                    RechartsJsBridge {
                                        ChartShareHelper.shareHealthTrendsChart(context, recent30Entries)
                                    },
                                    "AndroidBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.evaluateJavascript(
                                            "if (typeof window.updateDashboardData === 'function') { window.updateDashboardData('$entriesJsonString'); }",
                                            null
                                        )
                                    }
                                }

                                loadUrl("file:///android_asset/recharts_dashboard.html")
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            view.evaluateJavascript(
                                "if (typeof window.updateDashboardData === 'function') { window.updateDashboardData('$entriesJsonString'); }",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Native M3 Jetpack Compose Charts
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Chart 1: 30-Day Mood Trendline
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. 30-Day Mood Trendline (Area Chart)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Text(
                                text = "Scale: 1 (Rough) to 5 (Great)",
                                fontSize = 11.sp,
                                color = MedTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(MedTealBg.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Draw grid lines
                                for (i in 0..4) {
                                    val y = (h / 4f) * i
                                    drawLine(
                                        color = Color(0xFFE2E8F0),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                if (moodScores.size > 1) {
                                    val pathLine = Path()
                                    val pathArea = Path()
                                    val stepX = w / (moodScores.size - 1)

                                    moodScores.reversed().forEachIndexed { i, score ->
                                        val normY = (5 - score) / 4f
                                        val y = normY * h
                                        val x = i * stepX

                                        if (i == 0) {
                                            pathLine.moveTo(x, y)
                                            pathArea.moveTo(x, h)
                                            pathArea.lineTo(x, y)
                                        } else {
                                            pathLine.lineTo(x, y)
                                            pathArea.lineTo(x, y)
                                        }
                                    }
                                    pathArea.lineTo(w, h)
                                    pathArea.close()

                                    // Draw gradient fill
                                    drawPath(
                                        path = pathArea,
                                        color = Color(0xFF0D9488).copy(alpha = 0.25f)
                                    )
                                    // Draw line
                                    drawPath(
                                        path = pathLine,
                                        color = Color(0xFF0D9488),
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Draw dots
                                    moodScores.reversed().forEachIndexed { i, score ->
                                        val normY = (5 - score) / 4f
                                        val y = normY * h
                                        val x = i * stepX
                                        drawCircle(color = Color(0xFF0D9488), radius = 3.5.dp.toPx(), center = Offset(x, y))
                                    }
                                }
                            }
                        }
                    }

                    // Chart 2: 30-Day Symptom Frequency Bars
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "2. 30-Day Symptom Frequency Distribution",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )

                        if (symptomFrequencies.isEmpty()) {
                            Text(
                                text = "Zero adverse symptoms reported in 30 days!",
                                fontSize = 12.sp,
                                color = MedSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val maxFreq = symptomFrequencies.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
                            val barColors = listOf(
                                Color(0xFFF43F5E),
                                Color(0xFFF59E0B),
                                Color(0xFF0D9488),
                                Color(0xFF0284C7),
                                Color(0xFF8B5CF6),
                                Color(0xFFEC4899)
                            )

                            symptomFrequencies.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = item.key,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MedTextPrimary,
                                        modifier = Modifier.width(90.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(9.dp))
                                    ) {
                                        val fraction = item.value.toFloat() / maxFreq
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .background(
                                                    barColors[idx % barColors.size],
                                                    RoundedCornerShape(9.dp)
                                                )
                                        )
                                    }

                                    Text(
                                        text = "${item.value}x",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary,
                                        modifier = Modifier.width(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Share Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MedTealBg,
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export for Physician or Caregiver",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTealDark
                        )
                        Text(
                            text = "Download high-resolution image with full mood & symptom statistics",
                            fontSize = 11.sp,
                            color = MedTextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            ChartShareHelper.shareHealthTrendsChart(context, recent30Entries)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
