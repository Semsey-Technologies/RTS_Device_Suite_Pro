package com.semseytech.rtsdevicesuitepro.prereset.logic

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.semseytech.rtsdevicesuitepro.prereset.model.PreResetGuideData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PreResetPdfGenerator(private val context: Context) {

    fun generatePdf(data: PreResetGuideData): Uri? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.DKGRAY
        }
        val textPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 12f
            color = Color.BLACK
        }
        val warningPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.RED
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        // Title
        canvas.drawText("Pre-Reset Safety Guide", 50f, y, titlePaint)
        y += 30f
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        canvas.drawText("Generated: ${sdf.format(Date(data.timestamp))}", 50f, y, textPaint)
        y += 20f
        canvas.drawText("Device: ${data.deviceModel} (Android ${data.androidVersion})", 50f, y, textPaint)
        y += 40f

        // Accounts Section
        canvas.drawText("1. REGISTERED ACCOUNTS", 50f, y, headerPaint)
        y += 25f
        data.accounts.forEach { account ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
            canvas.drawText("- ${account.name} (${account.type})", 70f, y, textPaint)
            y += 20f
        }
        y += 20f

        // Critical Apps Section
        canvas.drawText("2. CRITICAL APPS (2FA / Security)", 50f, y, headerPaint)
        y += 25f
        val criticalApps = data.apps.filter { it.isTwoFactorApp || it.category == com.semseytech.rtsdevicesuitepro.prereset.model.AppCategory.BANKING }
        criticalApps.forEach { app ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
            canvas.drawText("- ${app.name} (${app.packageName})", 70f, y, warningPaint)
            y += 20f
            canvas.drawText("  ACTION: Ensure you have backup codes or have transferred this app to another device.", 90f, y, textPaint)
            y += 20f
        }
        y += 20f

        // Local Data Section
        canvas.drawText("3. APPS WITH LOCAL DATA", 50f, y, headerPaint)
        y += 25f
        val localDataApps = data.apps.filter { it.storesLocalData }
        localDataApps.forEach { app ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
            canvas.drawText("- ${app.name}", 70f, y, textPaint)
            y += 20f
            if (app.requiresManualExport) {
                canvas.drawText("  ACTION: REQUIRES MANUAL EXPORT (Check app settings)", 90f, y, warningPaint)
                y += 20f
            }
        }

        pdfDocument.finishPage(page)

        val fileName = "PreResetGuide_${System.currentTimeMillis()}.pdf"
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                uri = Uri.fromFile(file)
            }

            outputStream?.let {
                pdfDocument.writeTo(it)
                it.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri = null
        } finally {
            pdfDocument.close()
        }

        return uri
    }
}
