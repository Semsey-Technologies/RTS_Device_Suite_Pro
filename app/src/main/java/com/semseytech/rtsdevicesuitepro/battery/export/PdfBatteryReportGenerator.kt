package com.semseytech.rtsdevicesuitepro.battery.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleBatteryStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfBatteryReportGenerator(private val context: Context) {

    fun generateReport(statuses: List<ModuleBatteryStatus>): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 50f

        // Title
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("RTS Device Suite Pro - Battery Report", 50f, y, paint)
        y += 40f

        // Date
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $date", 50f, y, paint)
        y += 30f

        // Header
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Module Name", 50f, y, paint)
        canvas.drawText("Usage (%)", 300f, y, paint)
        canvas.drawText("Estimated mAh", 450f, y, paint)
        y += 20f
        canvas.drawLine(50f, y, 550f, y, paint)
        y += 20f

        // Data
        paint.textSize = 12f
        paint.isFakeBoldText = false
        statuses.forEach { status ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                // In a real app, handle multiple pages here
                return@forEach 
            }
            canvas.drawText(status.name, 50f, y, paint)
            canvas.drawText("${"%.2f".format(status.batteryPercent)}%", 300f, y, paint)
            canvas.drawText("${"%.0f".format(status.estimatedMah)}", 450f, y, paint)
            y += 20f
            
            paint.textSize = 10f
            paint.color = Color.GRAY
            canvas.drawText(status.explanation, 60f, y, paint)
            paint.color = Color.BLACK
            paint.textSize = 12f
            y += 25f
        }

        pdfDocument.finishPage(page)

        val fileName = "BatteryReport_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
