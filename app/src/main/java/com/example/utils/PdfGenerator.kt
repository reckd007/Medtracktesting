package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.model.HealthDiaryEntity
import com.example.data.model.MedicationEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfGenerator {

    fun generateDiaryReport(
        context: Context,
        diaryEntries: List<HealthDiaryEntity>,
        medications: List<MedicationEntity>
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 50f
        
        paint.textSize = 20f
        canvas.drawText("Health Diary and Medication Report", 50f, y, paint)
        y += 40f

        paint.textSize = 14f
        canvas.drawText("Medications:", 50f, y, paint)
        y += 30f
        medications.forEach { med ->
            canvas.drawText("- ${med.name}: ${med.dosage} (${med.form})", 70f, y, paint)
            y += 20f
        }
        y += 20f

        canvas.drawText("Diary Entries (Recent):", 50f, y, paint)
        y += 30f
        diaryEntries.take(10).forEach { entry ->
            canvas.drawText("- ${entry.dateString}: ${entry.mood} | ${entry.notes}", 70f, y, paint)
            y += 20f
            if (y > 800) { // Very simple page break handling
                document.finishPage(page)
                // Add new page logic here if needed
                return@forEach
            }
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "HealthReport.pdf")
        try {
            document.writeTo(FileOutputStream(file))
            return file
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }
}
