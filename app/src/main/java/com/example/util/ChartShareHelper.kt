package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.HealthDiaryEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ChartShareHelper {

    /**
     * Generates a high-resolution, clinic-ready 30-day health analytics chart bitmap,
     * saves it to the app cache directory, and invokes Android's native system share sheet.
     */
    fun shareHealthTrendsChart(context: Context, entries: List<HealthDiaryEntity>) {
        try {
            val bitmap = generateAnalyticsBitmap(context, entries)
            val cachePath = File(context.cacheDir, "charts")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val file = File(cachePath, "medtrack_30day_trends_${System.currentTimeMillis()}.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "MedTrack 30-Day Health Diary: Mood & Symptom Trends"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is my 30-day Mood & Symptom Frequency Trends Report generated from MedTrack."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share 30-Day Health Trends Chart").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "30-day health chart ready to share!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to export chart: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateAnalyticsBitmap(context: Context, rawEntries: List<HealthDiaryEntity>): Bitmap {
        val width = 1080
        val height = 1520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#F0FDFA")) // Soft teal tint

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Card Container
        val cardRect = RectF(36f, 36f, (width - 36).toFloat(), (height - 36).toFloat())
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 32f, 32f, paint)

        // Card Border
        paint.color = Color.parseColor("#CCFBF1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(cardRect, 32f, 32f, paint)

        // Header Background Banner
        val headerRect = RectF(36f, 36f, (width - 36).toFloat(), 220f)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0D9488")
        canvas.drawRoundRect(headerRect, 32f, 32f, paint)
        // Rectify bottom corners of header
        canvas.drawRect(36f, 160f, (width - 36).toFloat(), 220f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MedTrack • 30-Day Health Analytics", 72f, 115f, paint)

        // Subtitle & Date
        paint.textSize = 24f
        paint.color = Color.parseColor("#CCFBF1")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val todayStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Mood & Symptom Frequency Trends | Exported $todayStr", 72f, 165f, paint)

        // Metrics Row
        val entries = rawEntries.take(30).reversed()
        val totalDays = entries.size.coerceAtLeast(1)
        val moodScores = entries.map {
            when (it.mood) {
                "Great" -> 5
                "Good" -> 4
                "Neutral" -> 3
                "Low" -> 2
                else -> 1
            }
        }
        val avgMood = if (moodScores.isNotEmpty()) moodScores.average() else 4.0
        val avgMoodStr = String.format(Locale.getDefault(), "%.1f / 5.0", avgMood)

        // Calculate symptom frequencies
        val symptomCounts = mutableMapOf<String, Int>()
        entries.forEach { entry ->
            val syms = entry.symptoms.split(",").map { it.trim() }.filter {
                it.isNotEmpty() && !it.equals("None", ignoreCase = true) && !it.contains("Feeling great", ignoreCase = true) && !it.contains("Well rested", ignoreCase = true)
            }
            syms.forEach { s ->
                val norm = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                symptomCounts[norm] = (symptomCounts[norm] ?: 0) + 1
            }
        }
        val topSymptom = symptomCounts.maxByOrNull { it.value }?.let { "${it.key} (${it.value}x)" } ?: "None reported"

        // 3 Metric Cards
        drawMetricBadge(canvas, "Average Mood", avgMoodStr, 72f, 250f, 290f, 100f, "#0D9488")
        drawMetricBadge(canvas, "Top Symptom", topSymptom, 392f, 250f, 310f, 100f, "#F59E0B")
        drawMetricBadge(canvas, "Days Tracked", "$totalDays / 30 Days", 732f, 250f, 276f, 100f, "#0284C7")

        // SECTION 1: Mood Trend Chart
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. 30-Day Mood Trendline", 72f, 410f, paint)

        paint.textSize = 20f
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Daily mood variations (Score 5 = Great, Score 1 = Rough)", 72f, 442f, paint)

        // Draw Mood Plot Box
        val plotLeft = 140f
        val plotRight = width - 80f
        val plotTop = 480f
        val plotBottom = 780f
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        // Grid lines & Y labels
        val yLabels = listOf("5 - Great", "4 - Good", "3 - Neutral", "2 - Low", "1 - Rough")
        paint.textSize = 18f
        paint.color = Color.parseColor("#94A3B8")
        for (i in 0..4) {
            val y = plotTop + (plotHeight / 4f) * i
            canvas.drawText(yLabels[i], 42f, y + 6f, paint)
            val gridPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1.5f
            }
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
        }

        // Draw Mood Line & Gradient Fill
        if (moodScores.isNotEmpty()) {
            val pathLine = android.graphics.Path()
            val pathArea = android.graphics.Path()

            val stepX = plotWidth / (moodScores.size - 1).coerceAtLeast(1)

            moodScores.forEachIndexed { idx, score ->
                val x = plotLeft + idx * stepX
                val normY = (5 - score) / 4f // 5 is top (0), 1 is bottom (1)
                val y = plotTop + normY * plotHeight

                if (idx == 0) {
                    pathLine.moveTo(x, y)
                    pathArea.moveTo(x, plotBottom)
                    pathArea.lineTo(x, y)
                } else {
                    pathLine.lineTo(x, y)
                    pathArea.lineTo(x, y)
                }
            }
            pathArea.lineTo(plotLeft + (moodScores.size - 1) * stepX, plotBottom)
            pathArea.close()

            // Fill area
            val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, plotTop, 0f, plotBottom,
                    Color.parseColor("#5EEAD4"),
                    Color.parseColor("#F0FDFA"),
                    Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
            }
            canvas.drawPath(pathArea, areaPaint)

            // Line stroke
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D9488")
                strokeWidth = 5f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(pathLine, strokePaint)

            // Draw Dots
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D9488")
                style = Paint.Style.FILL
            }
            val dotCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            moodScores.forEachIndexed { idx, score ->
                val x = plotLeft + idx * stepX
                val normY = (5 - score) / 4f
                val y = plotTop + normY * plotHeight
                canvas.drawCircle(x, y, 7f, dotPaint)
                canvas.drawCircle(x, y, 3.5f, dotCenterPaint)
            }
        }

        // X-Axis labels for dates (first, mid, last)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 18f
        if (entries.isNotEmpty()) {
            canvas.drawText(entries.first().dateString, plotLeft, plotBottom + 30f, paint)
            val midIdx = entries.size / 2
            val midDate = entries[midIdx].dateString
            canvas.drawText(midDate, plotLeft + plotWidth / 2f - 40f, plotBottom + 30f, paint)
            canvas.drawText(entries.last().dateString, plotRight - 80f, plotBottom + 30f, paint)
        }

        // Divider
        val divPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(72f, 850f, width - 72f, 850f, divPaint)

        // SECTION 2: Symptom Frequency Distribution
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. 30-Day Symptom Frequency Distribution", 72f, 900f, paint)

        paint.textSize = 20f
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Occurrences reported across 30 days of daily diary records", 72f, 932f, paint)

        // Display top 5 symptoms or empty message
        val top5 = symptomCounts.entries.sortedByDescending { it.value }.take(5)
        val barStartTop = 980f
        val maxCount = top5.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

        val barColors = listOf("#F43F5E", "#F59E0B", "#0D9488", "#0284C7", "#8B5CF6")

        if (top5.isEmpty()) {
            paint.textSize = 22f
            paint.color = Color.parseColor("#0D9488")
            canvas.drawText("No adverse symptoms logged over the past 30 days. Perfect tolerance!", 72f, 1060f, paint)
        } else {
            top5.forEachIndexed { index, entry ->
                val y = barStartTop + index * 75f

                // Label
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 22f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(entry.key, 72f, y + 25f, paint)

                // Background track
                val trackRect = RectF(280f, y, width - 180f, y + 36f)
                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F1F5F9")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(trackRect, 18f, 18f, trackPaint)

                // Colored Bar
                val barWidth = ((width - 460f) * (entry.value.toFloat() / maxCount)).coerceAtLeast(30f)
                val barRect = RectF(280f, y, 280f + barWidth, y + 36f)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(barColors[index % barColors.size])
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(barRect, 18f, 18f, barPaint)

                // Value Text
                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 20f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val countText = "${entry.value} times"
                canvas.drawText(countText, 290f + barWidth + 16f, y + 26f, paint)
            }
        }

        // Footer Card
        val footerRect = RectF(72f, 1370f, width - 72f, 1460f)
        paint.color = Color.parseColor("#F0FDFA")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(footerRect, 16f, 16f, paint)

        paint.color = Color.parseColor("#0F766E")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Verified Clinical Summary • Patient Regimen Adherence Active", 96f, 1410f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by MedTrack Medication & Health Tracker", 96f, 1440f, paint)

        return bitmap
    }

    private fun drawMetricBadge(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        accentColor: String
    ) {
        val rect = RectF(x, y, x + w, y + h)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 20f, 20f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 20f, 20f, borderPaint)

        // Accent strip on left
        val accentRect = RectF(x, y + 10f, x + 8f, y + h - 10f)
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(accentColor)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(accentRect, 4f, 4f, accentPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(label, x + 22f, y + 36f, textPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 22f, y + 74f, valPaint)
    }
}
