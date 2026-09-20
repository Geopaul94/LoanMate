package com.loanmate.utils

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.loanmate.data.local.LoanEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Generates loan statements and amortization schedules in PDF format.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val LOAN_CARD_HEIGHT = 130f

    fun export(
        context: Context,
        loans: List<LoanEntity>,
        outFileName: String = defaultFileName("Report")
    ): File {
        val pdf = PdfDocument()
        val paints = createPaints()

        val dateLabel = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(java.util.Date())
        val totalOutstanding = loans.sumOf { it.outstandingAmount }
        val totalMonthlyEmi = loans.filter { it.status == com.loanmate.data.model.LoanStatus.ACTIVE }
            .sumOf { it.monthlyEmi }

        var pageNum = 1
        var page: PdfDocument.Page = pdf.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        )
        var canvas = page.canvas
        var y = MARGIN + 20f

        // --- Header ---
        canvas.drawText("LoanMate", MARGIN, y, paints.title)
        canvas.drawText("Generated $dateLabel", MARGIN, y + 16f, paints.muted)
        y += 40f

        // --- Summary ---
        canvas.drawText("Summary", MARGIN, y, paints.h2)
        y += 18f
        canvas.drawText("Total loans: ${loans.size}", MARGIN, y, paints.body)
        y += 14f
        canvas.drawText("Total outstanding: ${CurrencyUtils.format(totalOutstanding)}", MARGIN, y, paints.body)
        y += 14f
        canvas.drawText("Total monthly EMI: ${CurrencyUtils.format(totalMonthlyEmi)}", MARGIN, y, paints.body)
        y += 24f

        // --- Loans list ---
        canvas.drawText("Loans", MARGIN, y, paints.h2)
        y += 14f

        for (loan in loans) {
            if (y + LOAN_CARD_HEIGHT > PAGE_HEIGHT - MARGIN) {
                pdf.finishPage(page)
                pageNum += 1
                page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                )
                canvas = page.canvas
                y = MARGIN
            }
            drawLoanCard(
                canvas = canvas,
                loan = loan,
                top = y,
                paints = paints
            )
            y += LOAN_CARD_HEIGHT + 8f
        }

        pdf.finishPage(page)
        return savePdf(pdf, context, outFileName)
    }

    fun exportAmortization(
        context: Context,
        loan: LoanEntity,
        schedule: List<AmortizationCalculator.AmortizationMonth>
    ): File {
        val pdf = PdfDocument()
        val paints = createPaints()

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN + 20f

        // Header
        canvas.drawText("Amortization Schedule", MARGIN, y, paints.title)
        y += 24f
        canvas.drawText("${loan.loanName} · ${loan.bankName}", MARGIN, y, paints.h2)
        y += 16f
        canvas.drawText("Principal: ${CurrencyUtils.format(loan.principalAmount)} · Rate: ${loan.interestRate}%", MARGIN, y, paints.muted)
        y += 32f

        // Table Header
        val colWidths = listOf(50f, 130f, 130f, 150f)
        val colPositions = mutableListOf<Float>()
        var currentX = MARGIN
        colWidths.forEach { pos -> 
            colPositions.add(currentX)
            currentX += pos
        }

        canvas.drawText("Month", colPositions[0], y, paints.h2)
        canvas.drawText("Principal", colPositions[1], y, paints.h2)
        canvas.drawText("Interest", colPositions[2], y, paints.h2)
        canvas.drawText("Balance", colPositions[3], y, paints.h2)
        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.cardBorder)
        y += 20f

        for (month in schedule) {
            if (y > PAGE_HEIGHT - MARGIN - 20f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                y = MARGIN + 20f
            }

            canvas.drawText(month.monthNumber.toString(), colPositions[0], y, paints.body)
            canvas.drawText(CurrencyUtils.formatShort(month.principal), colPositions[1], y, paints.body)
            canvas.drawText(CurrencyUtils.formatShort(month.interest), colPositions[2], y, paints.body)
            canvas.drawText(CurrencyUtils.formatShort(month.remainingBalance), colPositions[3], y, paints.accent)
            y += 18f
        }

        pdf.finishPage(page)
        return savePdf(pdf, context, defaultFileName("Schedule_${loan.loanName.replace(" ", "_")}"))
    }

    private data class PdfPaints(
        val title: Paint,
        val h2: Paint,
        val body: Paint,
        val muted: Paint,
        val accent: Paint,
        val cardBorder: Paint
    )

    private fun createPaints() = PdfPaints(
        title = Paint().apply {
            color = AndroidColor.parseColor("#1565C0")
            textSize = 22f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        h2 = Paint().apply {
            color = AndroidColor.parseColor("#212121")
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        body = Paint().apply {
            color = AndroidColor.parseColor("#424242")
            textSize = 11f
            isAntiAlias = true
        },
        muted = Paint().apply {
            color = AndroidColor.parseColor("#757575")
            textSize = 10f
            isAntiAlias = true
        },
        accent = Paint().apply {
            color = AndroidColor.parseColor("#2E7D32")
            textSize = 11f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        cardBorder = Paint().apply {
            color = AndroidColor.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
    )

    private fun drawLoanCard(
        canvas: android.graphics.Canvas,
        loan: LoanEntity,
        top: Float,
        paints: PdfPaints
    ) {
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        canvas.drawRoundRect(
            RectF(left, top, right, top + LOAN_CARD_HEIGHT), 8f, 8f, paints.cardBorder
        )
        var ty = top + 18f
        canvas.drawText("${loan.loanType.emoji} ${loan.loanName}", left + 12f, ty, paints.h2)
        ty += 14f
        canvas.drawText("${loan.bankName} · ${loan.loanType.displayName}", left + 12f, ty, paints.muted)
        ty += 18f

        val col1 = left + 12f
        val col2 = left + 200f
        val col3 = left + 380f
        canvas.drawText("Principal", col1, ty, paints.muted)
        canvas.drawText("Outstanding", col2, ty, paints.muted)
        canvas.drawText("EMI", col3, ty, paints.muted)
        ty += 14f
        canvas.drawText(CurrencyUtils.format(loan.principalAmount), col1, ty, paints.body)
        canvas.drawText(CurrencyUtils.format(loan.outstandingAmount), col2, ty, paints.accent)
        canvas.drawText(CurrencyUtils.format(loan.monthlyEmi), col3, ty, paints.body)
        ty += 16f
        canvas.drawText("Rate", col1, ty, paints.muted)
        canvas.drawText("EMIs paid", col2, ty, paints.muted)
        canvas.drawText("Status", col3, ty, paints.muted)
        ty += 14f
        canvas.drawText("${loan.interestRate}% (${loan.interestType.displayName})", col1, ty, paints.body)
        canvas.drawText("${loan.completedEmis} / ${loan.totalEmis}", col2, ty, paints.body)
        canvas.drawText(loan.status.name, col3, ty, paints.body)
    }

    private fun savePdf(pdf: PdfDocument, context: Context, fileName: String): File {
        val outDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val outFile = File(outDir, fileName)
        try {
            FileOutputStream(outFile).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return outFile
    }

    private fun defaultFileName(prefix: String): String {
        val ts = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
            .format(java.util.Date())
        return "LoanMate_${prefix}_$ts.pdf"
    }
}
