package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicationEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class DayDoseStatus {
    ALL_TAKEN,
    PARTIAL,
    MISSED,
    UPCOMING,
    NO_DOSES
}

data class CalendarDayDoseInfo(
    val date: Date,
    val dayNumber: Int,
    val dateString: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isPast: Boolean,
    val isFuture: Boolean,
    val status: DayDoseStatus,
    val takenDoses: List<String>,
    val missedDoses: List<String>,
    val upcomingDoses: List<String>
)

object CalendarImageExportHelper {

    /**
     * Downloads and saves the monthly medication dose calendar bitmap to the device's
     * Pictures/Gallery directory, and prompts the user with an option to view or share it.
     */
    fun exportAndDownloadCalendarImage(
        context: Context,
        calendarMonth: Calendar,
        days: List<CalendarDayDoseInfo>,
        profileName: String,
        medications: List<MedicationEntity>,
        onSuccess: (Uri) -> Unit = {}
    ) {
        try {
            val bitmap = generateCalendarBitmap(context, calendarMonth, days, profileName, medications)
            val monthFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
            val monthName = monthFormat.format(calendarMonth.time)
            val fileName = "Vanguard_Dose_Calendar_${monthName}_${System.currentTimeMillis()}.png"

            val uri = saveBitmapToGallery(context, bitmap, fileName)

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Calendar image downloaded successfully to Gallery!",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (uri != null) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Failed to download calendar image: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shares the calendar image directly via Android's native system share sheet.
     */
    fun shareCalendarImage(
        context: Context,
        calendarMonth: Calendar,
        days: List<CalendarDayDoseInfo>,
        profileName: String,
        medications: List<MedicationEntity>
    ) {
        try {
            val bitmap = generateCalendarBitmap(context, calendarMonth, days, profileName, medications)
            val cachePath = File(context.cacheDir, "calendar_exports")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val monthFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
            val monthName = monthFormat.format(calendarMonth.time)
            val file = File(cachePath, "Vanguard_Dose_Calendar_${monthName}.png")
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
                    "Medication Dose Calendar - ${monthFormat.format(calendarMonth.time)}"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is my medication dose schedule & adherence calendar for ${monthFormat.format(calendarMonth.time)} generated from Vanguard Health."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Medication Dose Calendar").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Error sharing calendar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        var outputStream: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VanguardHealth")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/VanguardHealth"
                val dir = File(imagesDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, fileName)
                outputStream = FileOutputStream(file)
                imageUri = Uri.fromFile(file)
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to cache directory
            val cacheFile = File(context.cacheDir, fileName)
            val fallbackStream = FileOutputStream(cacheFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fallbackStream)
            fallbackStream.flush()
            fallbackStream.close()
            imageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        }

        return imageUri
    }

    private fun generateCalendarBitmap(
        context: Context,
        calendarMonth: Calendar,
        days: List<CalendarDayDoseInfo>,
        profileName: String,
        medications: List<MedicationEntity>
    ): Bitmap {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Palette Colors matching Vanguard Health warm earthy aesthetic
        val bgSand = Color.parseColor("#FDFBF7")
        val cardTaupe = Color.parseColor("#F4EDE2")
        val borderTaupe = Color.parseColor("#DCD2C4")
        val textEspresso = Color.parseColor("#2C2522")
        val textMuted = Color.parseColor("#6B5D56")
        val terracotta = Color.parseColor("#B87355")
        val greenSuccess = Color.parseColor("#4D7C0F")
        val greenBg = Color.parseColor("#ECFCCB")
        val redMissed = Color.parseColor("#B91C1C")
        val redBg = Color.parseColor("#FEE2E2")
        val amberPartial = Color.parseColor("#D97706")
        val amberBg = Color.parseColor("#FEF3C7")
        val tealUpcoming = Color.parseColor("#0D9488")
        val tealBg = Color.parseColor("#CCFBF1")

        // 1. Background
        canvas.drawColor(bgSand)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 2. Card Container
        val margin = 40f
        val cardRect = RectF(margin, margin, width - margin, height - margin)
        paint.color = cardTaupe
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 28f, 28f, paint)

        // Card Border
        paint.color = borderTaupe
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(cardRect, 28f, 28f, paint)

        // 3. Header Banner
        val headerHeight = 200f
        val headerRect = RectF(margin, margin, width - margin, margin + headerHeight)
        paint.style = Paint.Style.FILL
        paint.color = terracotta
        canvas.drawRoundRect(headerRect, 28f, 28f, paint)
        canvas.drawRect(margin, margin + 40f, width - margin, margin + headerHeight, paint)

        // Brand Label
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText("VANGUARD HEALTH", margin + 40f, margin + 55f, paint)

        // Title
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val monthTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthTitle = monthTitleFormat.format(calendarMonth.time).uppercase(Locale.getDefault())
        canvas.drawText("Medication Dose & Adherence Calendar", margin + 40f, margin + 115f, paint)

        // Subtitle (Month & Profile)
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Month: $monthTitle • Patient Profile: $profileName", margin + 40f, margin + 160f, paint)

        // 4. Summary Stats Row
        val currentMonthDays = days.filter { it.isCurrentMonth }
        val takenCount = currentMonthDays.count { it.status == DayDoseStatus.ALL_TAKEN }
        val missedCount = currentMonthDays.count { it.status == DayDoseStatus.MISSED }
        val partialCount = currentMonthDays.count { it.status == DayDoseStatus.PARTIAL }
        val upcomingCount = currentMonthDays.count { it.status == DayDoseStatus.UPCOMING }

        val pastEvaluated = takenCount + missedCount + partialCount
        val adherenceRate = if (pastEvaluated > 0) {
            Math.round(((takenCount.toDouble() + (partialCount * 0.5)) / pastEvaluated) * 100).toInt()
        } else 100

        val statsY = margin + headerHeight + 35f
        val statBoxWidth = (width - margin * 2 - 80f) / 4f
        val statBoxHeight = 85f

        val stats = listOf(
            Triple("TAKEN DAYS", "$takenCount Days", greenSuccess),
            Triple("MISSED DAYS", "$missedCount Days", redMissed),
            Triple("UPCOMING", "$upcomingCount Days", tealUpcoming),
            Triple("ADHERENCE", "$adherenceRate%", terracotta)
        )

        stats.forEachIndexed { i, stat ->
            val boxLeft = margin + 30f + (i * (statBoxWidth + 15f))
            val boxRect = RectF(boxLeft, statsY, boxLeft + statBoxWidth, statsY + statBoxHeight)

            paint.style = Paint.Style.FILL
            paint.color = bgSand
            canvas.drawRoundRect(boxRect, 16f, 16f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = borderTaupe
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(boxRect, 16f, 16f, paint)

            paint.style = Paint.Style.FILL
            paint.color = textMuted
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(stat.first, boxLeft + 16f, statsY + 30f, paint)

            paint.color = stat.third
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(stat.second, boxLeft + 16f, statsY + 65f, paint)
        }

        // 5. Day Headers (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
        val dayHeadersY = statsY + statBoxHeight + 45f
        val gridMarginLeft = margin + 30f
        val gridWidth = width - margin * 2 - 60f
        val colWidth = gridWidth / 7f

        val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = textEspresso

        dayNames.forEachIndexed { col, name ->
            val colX = gridMarginLeft + col * colWidth
            canvas.drawText(name, colX + colWidth / 2f - 18f, dayHeadersY, paint)
        }

        // 6. Calendar Days Grid (6 rows x 7 cols)
        val gridTop = dayHeadersY + 15f
        val rowHeight = 115f

        days.take(42).forEachIndexed { index, dayInfo ->
            val row = index / 7
            val col = index % 7

            val cellLeft = gridMarginLeft + col * colWidth + 4f
            val cellTop = gridTop + row * rowHeight + 4f
            val cellRight = cellLeft + colWidth - 8f
            val cellBottom = cellTop + rowHeight - 8f
            val cellRect = RectF(cellLeft, cellTop, cellRight, cellBottom)

            // Cell Background
            paint.style = Paint.Style.FILL
            paint.color = if (dayInfo.isToday) {
                Color.parseColor("#FFFBEB") // light warm amber for today
            } else if (dayInfo.isCurrentMonth) {
                bgSand
            } else {
                Color.parseColor("#EAE1D3") // dimmed for non-current month
            }
            canvas.drawRoundRect(cellRect, 14f, 14f, paint)

            // Cell Border
            paint.style = Paint.Style.STROKE
            paint.color = if (dayInfo.isToday) terracotta else borderTaupe
            paint.strokeWidth = if (dayInfo.isToday) 2.5f else 1f
            canvas.drawRoundRect(cellRect, 14f, 14f, paint)

            if (dayInfo.isCurrentMonth) {
                // Date Number
                paint.style = Paint.Style.FILL
                paint.color = if (dayInfo.isToday) terracotta else textEspresso
                paint.textSize = 19f
                paint.typeface = Typeface.create(Typeface.DEFAULT, if (dayInfo.isToday) Typeface.BOLD else Typeface.NORMAL)
                canvas.drawText("${dayInfo.dayNumber}", cellLeft + 12f, cellTop + 24f, paint)

                if (dayInfo.isToday) {
                    paint.textSize = 11f
                    paint.color = terracotta
                    canvas.drawText("TODAY", cellRight - 45f, cellTop + 22f, paint)
                }

                // Status Badge Pill
                val badgeY = cellTop + 40f
                val badgeHeight = 32f
                val badgeRect = RectF(cellLeft + 8f, badgeY, cellRight - 8f, badgeY + badgeHeight)

                var badgeBg = tealBg
                var badgeTextColor = tealUpcoming
                var badgeText = "UPCOMING"

                when (dayInfo.status) {
                    DayDoseStatus.ALL_TAKEN -> {
                        badgeBg = greenBg
                        badgeTextColor = greenSuccess
                        badgeText = "TAKEN"
                    }
                    DayDoseStatus.MISSED -> {
                        badgeBg = redBg
                        badgeTextColor = redMissed
                        badgeText = "MISSED"
                    }
                    DayDoseStatus.PARTIAL -> {
                        badgeBg = amberBg
                        badgeTextColor = amberPartial
                        badgeText = "PARTIAL"
                    }
                    DayDoseStatus.UPCOMING -> {
                        badgeBg = tealBg
                        badgeTextColor = tealUpcoming
                        badgeText = "UPCOMING"
                    }
                    DayDoseStatus.NO_DOSES -> {
                        badgeBg = Color.parseColor("#F1F5F9")
                        badgeTextColor = textMuted
                        badgeText = "NO DOSES"
                    }
                }

                paint.style = Paint.Style.FILL
                paint.color = badgeBg
                canvas.drawRoundRect(badgeRect, 8f, 8f, paint)

                paint.color = badgeTextColor
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val textWidth = paint.measureText(badgeText)
                canvas.drawText(badgeText, badgeRect.centerX() - (textWidth / 2f), badgeRect.centerY() + 4f, paint)

                // Sub-label (e.g. "2 doses" or info)
                val doseCount = dayInfo.takenDoses.size + dayInfo.missedDoses.size + dayInfo.upcomingDoses.size
                if (doseCount > 0) {
                    paint.color = textMuted
                    paint.textSize = 11f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val countLabel = "$doseCount planned"
                    canvas.drawText(countLabel, cellLeft + 12f, cellBottom - 10f, paint)
                }
            } else {
                // Out of month date number
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#A89680")
                paint.textSize = 17f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${dayInfo.dayNumber}", cellLeft + 12f, cellTop + 24f, paint)
            }
        }

        // 7. Legend & Footer
        val legendY = height - margin - 80f
        paint.style = Paint.Style.FILL
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val legendItems = listOf(
            Pair("Taken", greenSuccess),
            Pair("Missed", redMissed),
            Pair("Partial", amberPartial),
            Pair("Upcoming", tealUpcoming)
        )

        val legendSpacing = 220f
        val legendStartX = margin + 60f

        legendItems.forEachIndexed { i, item ->
            val lx = legendStartX + i * legendSpacing
            paint.color = item.second
            canvas.drawCircle(lx, legendY, 8f, paint)

            paint.color = textEspresso
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(item.first, lx + 16f, legendY + 5f, paint)
        }

        // Footer timestamp and legal/clinic note
        paint.color = textMuted
        paint.textSize = 12f
        val exportTime = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on $exportTime by Vanguard Health • Dose Adherence Tracking Suite", margin + 60f, height - margin - 35f, paint)

        return bitmap
    }
}
