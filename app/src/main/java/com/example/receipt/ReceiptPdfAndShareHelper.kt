package com.example.receipt

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.localization.AppStrings
import com.example.localization.BiText
import com.example.model.Customer
import com.example.model.LanguageMode
import com.example.model.ShalwarKameezMeasurement
import com.example.model.ShopProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object ReceiptPdfAndShareHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun buildSuggestedFileName(customer: Customer): String {
        val safeName = customer.fullName
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "Customer" }
        val safeCode = customer.customerCode
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .ifBlank { "TB" }
        return "TailorBook_${safeCode}_${safeName}.pdf"
    }

    fun generateReceiptPdfBytes(
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        drawReceiptPage(
            canvas = page.canvas,
            customer = customer,
            measurement = measurement,
            measurementDate = measurementDate,
            notes = notes,
            shopProfile = shopProfile,
            languageMode = languageMode
        )
        document.finishPage(page)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    fun writePdfToUri(
        context: Context,
        uri: Uri,
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ): Result<Unit> = runCatching {
        val bytes = generateReceiptPdfBytes(
            customer = customer,
            measurement = measurement,
            measurementDate = measurementDate,
            notes = notes,
            shopProfile = shopProfile,
            languageMode = languageMode
        )
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(bytes)
            stream.flush()
        } ?: error("Unable to open output stream for selected file.")
    }

    fun printMeasurementReceipt(
        context: Context,
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ): Result<Unit> = runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: error("Print service unavailable on this device.")
        val jobName = "TailorBook_${customer.customerCode}_${customer.fullName}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(buildSuggestedFileName(customer))
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    val bytes = generateReceiptPdfBytes(
                        customer = customer,
                        measurement = measurement,
                        measurementDate = measurementDate,
                        notes = notes,
                        shopProfile = shopProfile,
                        languageMode = languageMode
                    )
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message ?: "Print failed")
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    fun sharePdfViaWhatsApp(
        context: Context,
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ): Result<Unit> = runCatching {
        val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(receiptsDir, buildSuggestedFileName(customer))
        val bytes = generateReceiptPdfBytes(
            customer = customer,
            measurement = measurement,
            measurementDate = measurementDate,
            notes = notes,
            shopProfile = shopProfile,
            languageMode = languageMode
        )
        FileOutputStream(file).use { it.write(bytes) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val summary = buildWhatsAppReceiptText(
            customer = customer,
            measurement = measurement,
            measurementDate = measurementDate,
            notes = notes,
            shopProfile = shopProfile,
            languageMode = languageMode
        )

        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_SUBJECT, "${customer.fullName} (${customer.customerCode}) - Shalwar Kameez Receipt")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val pm = context.packageManager
        val whatsAppPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            val testIntent = Intent(baseIntent).setPackage(pkg)
            testIntent.resolveActivity(pm) != null
        }

        if (whatsAppPackage != null) {
            baseIntent.setPackage(whatsAppPackage)
            context.startActivity(baseIntent)
        } else {
            context.startActivity(
                Intent.createChooser(baseIntent, "Share Measurement PDF Receipt")
            )
        }
    }

    fun shareTextViaWhatsApp(
        context: Context,
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ): Result<Unit> = runCatching {
        val text = buildWhatsAppReceiptText(
            customer = customer,
            measurement = measurement,
            measurementDate = measurementDate,
            notes = notes,
            shopProfile = shopProfile,
            languageMode = languageMode
        )
        val digitsOnly = customer.mobileNumber.filter { it.isDigit() }
        val pm = context.packageManager

        // If mobile number looks complete (10+ digits), open direct WhatsApp chat if installed
        if (digitsOnly.length >= 10) {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val waIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly&text=$encoded")
            )
            if (waIntent.resolveActivity(pm) != null) {
                try {
                    context.startActivity(waIntent)
                    return@runCatching
                } catch (_: ActivityNotFoundException) {
                    // Fall through to ACTION_SEND
                }
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val whatsAppPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            Intent(sendIntent).setPackage(pkg).resolveActivity(pm) != null
        }
        if (whatsAppPackage != null) {
            sendIntent.setPackage(whatsAppPackage)
            context.startActivity(sendIntent)
        } else {
            context.startActivity(Intent.createChooser(sendIntent, "Share Measurements"))
        }
    }

    fun buildWhatsAppReceiptText(
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ): String {
        val shopName = shopProfile?.shopName?.ifBlank { "TailorBook" } ?: "TailorBook"
        val unitLabel = AppStrings.unitShort(measurement.unit, languageMode)

        fun line(bi: BiText, value: String): String? {
            if (value.isBlank()) return null
            return "• ${bi.inline(languageMode)}: *$value $unitLabel*"
        }

        val kameezLines = listOfNotNull(
            line(AppStrings.kameezLength, measurement.kameezLength),
            line(AppStrings.shoulder, measurement.shoulder),
            line(AppStrings.chest, measurement.chest),
            line(AppStrings.kameezWaist, measurement.kameezWaist),
            line(AppStrings.kameezHip, measurement.kameezHip),
            line(AppStrings.sleeveLength, measurement.sleeveLength),
            line(AppStrings.armhole, measurement.armhole),
            line(AppStrings.neck, measurement.neck),
            line(AppStrings.cuff, measurement.cuff),
            line(AppStrings.frontLength, measurement.frontLength),
            line(AppStrings.backLength, measurement.backLength),
            line(AppStrings.collar, measurement.collar)
        )

        val shalwarLines = listOfNotNull(
            line(AppStrings.shalwarLength, measurement.shalwarLength),
            line(AppStrings.shalwarWaist, measurement.shalwarWaist),
            line(AppStrings.shalwarHip, measurement.shalwarHip),
            line(AppStrings.thigh, measurement.thigh),
            line(AppStrings.knee, measurement.knee),
            line(AppStrings.bottomPaicha, measurement.bottomPaicha),
            if (measurement.shalwarStyle.isNotBlank()) {
                "• ${AppStrings.shalwarStyle.inline(languageMode)}: *${AppStrings.formatShalwarStyle(measurement.shalwarStyle, languageMode)}*"
            } else null
        )

        return buildString {
            appendLine("*$shopName*")
            shopProfile?.shopPhone?.takeIf { it.isNotBlank() }?.let { appendLine("Tel: $it") }
            appendLine("━━━━━━━━━━━━━━━")
            appendLine("*${AppStrings.fullName.inline(languageMode)}:* ${customer.fullName}")
            appendLine("*${AppStrings.customerId.inline(languageMode)}:* ${customer.customerCode}")
            appendLine("*${AppStrings.mobileNumber.inline(languageMode)}:* ${customer.mobileNumber}")
            appendLine("*${AppStrings.measurementDate.inline(languageMode)}:* $measurementDate")
            if (customer.address.isNotBlank()) {
                appendLine("*${AppStrings.address.inline(languageMode)}:* ${customer.address}")
            }
            appendLine()
            appendLine("*${AppStrings.kameezSection.inline(languageMode)} ($unitLabel)*")
            if (kameezLines.isEmpty()) appendLine("• —")
            else kameezLines.forEach { appendLine(it) }
            appendLine()
            appendLine("*${AppStrings.shalwarSection.inline(languageMode)} ($unitLabel)*")
            if (shalwarLines.isEmpty()) appendLine("• —")
            else shalwarLines.forEach { appendLine(it) }
            if (notes.isNotBlank()) {
                appendLine()
                appendLine("*${AppStrings.notes.inline(languageMode)}:* $notes")
            }
        }.trim()
    }

    private fun drawReceiptPage(
        canvas: Canvas,
        customer: Customer,
        measurement: ShalwarKameezMeasurement,
        measurementDate: String,
        notes: String,
        shopProfile: ShopProfile?,
        languageMode: LanguageMode
    ) {
        val margin = 32f
        val contentWidth = PAGE_WIDTH - (margin * 2)

        val emeraldPaint = Paint().apply { color = Color.rgb(20, 69, 50) }
        val goldPaint = Paint().apply { color = Color.rgb(184, 134, 11) }
        val cardBgPaint = Paint().apply { color = Color.rgb(248, 245, 238) }
        val borderPaint = Paint().apply {
            color = Color.rgb(212, 204, 190)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rowDividerPaint = Paint().apply {
            color = Color.rgb(230, 224, 212)
            strokeWidth = 0.8f
        }

        // Background
        canvas.drawColor(Color.WHITE)

        // Header banner
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 96f, emeraldPaint)
        canvas.drawRect(0f, 96f, PAGE_WIDTH.toFloat(), 100f, goldPaint)

        val shopTitle = shopProfile?.shopName?.ifBlank { "TailorBook Bespoke Studio" } ?: "TailorBook Bespoke Studio"
        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(228, 238, 232)
            textSize = 10.5f
        }
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(251, 230, 181)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText(shopTitle, margin, 40f, headerTitlePaint)
        val contactLine = listOfNotNull(
            shopProfile?.shopPhone?.takeIf { it.isNotBlank() },
            shopProfile?.shopAddress?.takeIf { it.isNotBlank() }
        ).joinToString("  •  ").ifBlank { "Shalwar Kameez Measurement Receipt" }
        canvas.drawText(contactLine, margin, 62f, headerSubPaint)
        canvas.drawText("MEASUREMENT RECEIPT", PAGE_WIDTH - margin, 40f, badgePaint)
        canvas.drawText("پیمائش کی رسید", PAGE_WIDTH - margin, 60f, badgePaint)

        var y = 118f

        // Customer Info Card
        val infoBoxRect = RectF(margin, y, PAGE_WIDTH - margin, y + 92f)
        canvas.drawRoundRect(infoBoxRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(infoBoxRect, 8f, 8f, borderPaint)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 92, 85)
            textSize = 9.5f
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 28, 25)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val monoValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 69, 50)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val col1X = margin + 14f
        val col2X = margin + contentWidth * 0.52f

        canvas.drawText(AppStrings.fullName.inline(languageMode), col1X, y + 20f, labelPaint)
        canvas.drawText(customer.fullName, col1X, y + 36f, valuePaint)

        canvas.drawText(AppStrings.customerId.inline(languageMode), col2X, y + 20f, labelPaint)
        canvas.drawText(customer.customerCode, col2X, y + 36f, monoValuePaint)

        canvas.drawText(AppStrings.mobileNumber.inline(languageMode), col1X, y + 54f, labelPaint)
        canvas.drawText(customer.mobileNumber, col1X, y + 70f, valuePaint)

        canvas.drawText(AppStrings.measurementDate.inline(languageMode), col2X, y + 54f, labelPaint)
        canvas.drawText(measurementDate, col2X, y + 70f, valuePaint)

        if (customer.address.isNotBlank()) {
            canvas.drawText(
                "${AppStrings.address.primary(languageMode)}: ${customer.address}",
                col1X,
                y + 84f,
                labelPaint
            )
        }

        y += 108f

        // Unit & Shalwar Style Pill Line
        val unitText = AppStrings.unitShort(measurement.unit, languageMode)
        val styleText = AppStrings.formatShalwarStyle(measurement.shalwarStyle, languageMode)
        val metaTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 69, 50)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "${AppStrings.unitLabel.inline(languageMode)}: $unitText    |    ${AppStrings.shalwarStyle.inline(languageMode)}: $styleText",
            margin,
            y + 12f,
            metaTextPaint
        )
        y += 26f

        // Two-column Measurement Tables
        val colGap = 16f
        val tableW = (contentWidth - colGap) / 2f
        val leftX = margin
        val rightX = margin + tableW + colGap

        val kameezRows = listOf(
            AppStrings.kameezLength to measurement.kameezLength,
            AppStrings.shoulder to measurement.shoulder,
            AppStrings.chest to measurement.chest,
            AppStrings.kameezWaist to measurement.kameezWaist,
            AppStrings.kameezHip to measurement.kameezHip,
            AppStrings.sleeveLength to measurement.sleeveLength,
            AppStrings.armhole to measurement.armhole,
            AppStrings.neck to measurement.neck,
            AppStrings.cuff to measurement.cuff,
            AppStrings.frontLength to measurement.frontLength,
            AppStrings.backLength to measurement.backLength,
            AppStrings.collar to measurement.collar
        )

        val shalwarRows = listOf(
            AppStrings.shalwarLength to measurement.shalwarLength,
            AppStrings.shalwarWaist to measurement.shalwarWaist,
            AppStrings.shalwarHip to measurement.shalwarHip,
            AppStrings.thigh to measurement.thigh,
            AppStrings.knee to measurement.knee,
            AppStrings.bottomPaicha to measurement.bottomPaicha
        )

        val leftEndY = drawMeasurementTable(
            canvas = canvas,
            x = leftX,
            startY = y,
            width = tableW,
            title = AppStrings.kameezSection.inline(languageMode),
            rows = kameezRows,
            unitSuffix = unitText,
            languageMode = languageMode,
            headerPaint = emeraldPaint,
            borderPaint = borderPaint,
            dividerPaint = rowDividerPaint
        )

        val rightEndY = drawMeasurementTable(
            canvas = canvas,
            x = rightX,
            startY = y,
            width = tableW,
            title = AppStrings.shalwarSection.inline(languageMode),
            rows = shalwarRows,
            unitSuffix = unitText,
            languageMode = languageMode,
            headerPaint = emeraldPaint,
            borderPaint = borderPaint,
            dividerPaint = rowDividerPaint
        )

        y = maxOf(leftEndY, rightEndY) + 18f

        // Notes Box
        if (notes.isNotBlank()) {
            val notesRect = RectF(margin, y, PAGE_WIDTH - margin, y + 90f)
            canvas.drawRoundRect(notesRect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(notesRect, 8f, 8f, borderPaint)

            val noteTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(20, 69, 50)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(AppStrings.notes.inline(languageMode), margin + 12f, y + 20f, noteTitlePaint)

            val noteBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(40, 42, 38)
                textSize = 10.5f
            }
            val layout = StaticLayout.Builder
                .obtain(notes, 0, notes.length, noteBodyPaint, (contentWidth - 24f).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(4)
                .build()
            canvas.save()
            canvas.translate(margin + 12f, y + 28f)
            layout.draw(canvas)
            canvas.restore()
        }

        // Footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 110)
            textSize = 9.5f
        }
        canvas.drawLine(margin, PAGE_HEIGHT - 44f, PAGE_WIDTH - margin, PAGE_HEIGHT - 44f, rowDividerPaint)
        canvas.drawText("Generated by TailorBook", margin, PAGE_HEIGHT - 26f, footerPaint)
        val rightFooter = TextPaint(footerPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("${customer.customerCode}  •  $measurementDate", PAGE_WIDTH - margin, PAGE_HEIGHT - 26f, rightFooter)
    }

    private fun drawMeasurementTable(
        canvas: Canvas,
        x: Float,
        startY: Float,
        width: Float,
        title: String,
        rows: List<Pair<BiText, String>>,
        unitSuffix: String,
        languageMode: LanguageMode,
        headerPaint: Paint,
        borderPaint: Paint,
        dividerPaint: Paint
    ): Float {
        val headerH = 28f
        val rowH = 30f
        val totalH = headerH + rows.size * rowH

        val tableRect = RectF(x, startY, x + width, startY + totalH)
        canvas.drawRoundRect(tableRect, 6f, 6f, Paint().apply { color = Color.WHITE })
        canvas.drawRect(x, startY, x + width, startY + headerH, headerPaint)
        canvas.drawRoundRect(tableRect, 6f, 6f, borderPaint)

        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, x + 10f, startY + 18f, sectionPaint)

        val fieldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 58, 52)
            textSize = 10f
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 22, 18)
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        var rowY = startY + headerH
        rows.forEachIndexed { idx, (label, value) ->
            if (idx > 0) {
                canvas.drawLine(x + 8f, rowY, x + width - 8f, rowY, dividerPaint)
            }
            val displayVal = if (value.isBlank()) "—" else "$value $unitSuffix"
            canvas.drawText(label.inline(languageMode), x + 10f, rowY + 19f, fieldPaint)
            canvas.drawText(displayVal, x + width - 10f, rowY + 19f, valPaint)
            rowY += rowH
        }
        return startY + totalH
    }
}
