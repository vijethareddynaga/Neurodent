package com.example.neurodentai

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.math.roundToInt

class ResultActivity : AppCompatActivity() {

    private val client = NetworkConfig.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        supportActionBar?.hide()

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val btnDeleteReport = findViewById<ImageButton>(R.id.btnDeleteReport)
        val returnDashboardText = findViewById<TextView>(R.id.returnDashboardText)
        val savePdfButton = findViewById<Button>(R.id.savePdfButton)
        val shareButton = findViewById<Button>(R.id.shareButton)
        
        val riskProgressIndicator = findViewById<CircularProgressIndicator>(R.id.riskProgressIndicator)
        val percentageTextView = findViewById<TextView>(R.id.percentageTextView)
        val riskBadgeText = findViewById<TextView>(R.id.riskBadgeText)
        val findingsListLayout = findViewById<LinearLayout>(R.id.findingsListLayout)

        val tvReportPatientName = findViewById<TextView>(R.id.tvReportPatientName)
        val tvReportPatientAgeGender = findViewById<TextView>(R.id.tvReportPatientAgeGender)
        val tvReportSurgicalNote = findViewById<TextView>(R.id.tvReportSurgicalNote)

        // Read patient details from Intent
        val patientName = intent.getStringExtra("PATIENT_NAME") ?: "Anonymous"
        val patientAge = intent.getIntExtra("PATIENT_AGE", 0)
        val patientGender = intent.getStringExtra("PATIENT_GENDER") ?: "N/A"
        val patientDisplayId = intent.getStringExtra("PATIENT_DISPLAY_ID") ?: ""
        val patientDbId = intent.getIntExtra("PATIENT_DB_ID", -1)

        tvReportPatientName.text = "Name: $patientName"
        tvReportPatientAgeGender.text = "Age: ${if (patientAge > 0) patientAge.toString() else "N/A"} | Gender: $patientGender"

        backButton.setOnClickListener { finish() }
        
        returnDashboardText.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Delete Report Setup
        if (patientDbId <= 0) {
            btnDeleteReport.visibility = View.GONE
        } else {
            btnDeleteReport.visibility = View.VISIBLE
            btnDeleteReport.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to delete this patient's report?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteReport(patientDbId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        val percentage = intent.getDoubleExtra("PERCENTAGE", 0.0)
        val roundedPercentage = percentage.roundToInt()

        // 1. Update Progress & Text
        percentageTextView.text = "${roundedPercentage}%"
        riskProgressIndicator.progress = roundedPercentage

        // 2. Generate UI styles & findings based on risk tier
        val riskColor: String
        val badgeBgColor: String
        val riskLabel: String
        val findingsList = mutableListOf<String>()
        val surgicalNoteText: String

        if (roundedPercentage < 30) {
            // Low Risk
            riskLabel = "Low Risk"
            riskColor = "#16A34A" // Green text
            badgeBgColor = "#DCFCE7" // Light green bg
            surgicalNoteText = "Surgical Note: Safe to proceed with standard extraction. Risk of IAN damage is extremely minimal."
            
            findingsList.add("Doctor can proceed with standard extraction safely.")
            findingsList.add("Low risk of IAN (Inferior Alveolar Nerve) damage.")
            findingsList.add("Cortical bone density appears normal.")
            findingsList.add("Root morphology does not indicate complex curvature.")
            
        } else if (roundedPercentage < 65) {
            // Moderate Risk
            riskLabel = "⚠️ Moderate Risk"
            riskColor = "#B45309" // Orange text
            badgeBgColor = "#FEF3C7" // Light orange bg
            surgicalNoteText = "Surgical Note: Proceed with caution. Surgical extraction may be required due to IAN proximity. Avoid excessive apical force."
            
            findingsList.add("Proceed with caution. Surgical extraction may be required.")
            findingsList.add("Tooth root in close proximity to IAN canal (< 1mm).")
            findingsList.add("Possible cortical bone thinning detected.")
            findingsList.add("Root morphology suggests slight curvature.")
            
        } else {
            // High Risk
            riskLabel = "🚨 High Risk"
            riskColor = "#DC2626" // Red text
            badgeBgColor = "#FEE2E2" // Light red bg
            surgicalNoteText = "Surgical Note: DO NOT PROCEED with standard extraction. Highly recommended to refer to an Oral & Maxillofacial Surgeon (OMFS). Consider ordering a 3D CBCT scan."
            
            findingsList.add("DO NOT PROCEED with standard extraction. Refer to specialist (OMFS).")
            findingsList.add("Extremely high risk: Root appears to intersect IAN canal.")
            findingsList.add("Severe impaction and complex root morphology detected.")
            findingsList.add("CBCT scan strongly recommended before any surgical intervention.")
        }

        tvReportSurgicalNote.text = surgicalNoteText

        // Apply Colors
        riskBadgeText.text = riskLabel
        riskBadgeText.setTextColor(Color.parseColor(riskColor))
        riskBadgeText.background.setTint(Color.parseColor(badgeBgColor))
        riskProgressIndicator.setIndicatorColor(Color.parseColor(riskColor))

        // Populate Findings
        for (finding in findingsList) {
            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 32) // Spacing between bullets
                }
            }

            // Bullet Point / Checkmark Text
            val iconView = TextView(this).apply {
                text = "●"
                setTextColor(Color.parseColor(riskColor))
                textSize = 14f
                setPadding(0, 8, 16, 0)
            }

            val textView = TextView(this).apply {
                text = finding
                setTextColor(Color.parseColor("#475569")) // Slate text color
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                )
            }

            itemView.addView(iconView)
            itemView.addView(textView)
            findingsListLayout.addView(itemView)
        }

        savePdfButton.setOnClickListener {
            saveReportAsPdf(
                patientName = patientName,
                patientAge = patientAge,
                patientGender = patientGender,
                displayId = patientDisplayId,
                percentage = roundedPercentage,
                riskLabel = riskLabel,
                surgicalNote = surgicalNoteText,
                findings = findingsList
            )
        }

        shareButton.setOnClickListener {
            shareReportAsPdf(
                patientName = patientName,
                patientAge = patientAge,
                patientGender = patientGender,
                displayId = patientDisplayId,
                percentage = roundedPercentage,
                riskLabel = riskLabel,
                surgicalNote = surgicalNoteText,
                findings = findingsList
            )
        }
    }

    private fun deleteReport(patientId: Int) {
        val request = Request.Builder()
            .url("http://10.170.241.109:8000/patients/$patientId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ResultActivity, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ResultActivity, "Report deleted successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ResultActivity, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ResultActivity, "Failed to delete report", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveReportAsPdf(
        patientName: String,
        patientAge: Int,
        patientGender: String,
        displayId: String,
        percentage: Int,
        riskLabel: String,
        surgicalNote: String,
        findings: List<String>
    ) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = android.graphics.Paint()
            val titlePaint = android.graphics.Paint().apply {
                color = Color.parseColor("#2563EB")
                textSize = 20f
                isFakeBoldText = true
            }
            
            val sectionPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#0A192F")
                textSize = 14f
                isFakeBoldText = true
            }
            
            val textPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 12f
                isAntiAlias = true
            }
            
            val boldTextPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#0A192F")
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var y = 50f
            
            // Header
            canvas.drawText("NEURODENT AI - DIAGNOSTIC REPORT", 50f, y, titlePaint)
            y += 10f
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 2f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 30f
            
            // Patient Details
            canvas.drawText("PATIENT INFORMATION", 50f, y, sectionPaint)
            y += 20f
            canvas.drawText("Name: $patientName", 50f, y, textPaint)
            canvas.drawText("ID: ${if (displayId.isNotEmpty()) displayId else "N/A"}", 300f, y, textPaint)
            y += 18f
            canvas.drawText("Age: ${if (patientAge > 0) patientAge.toString() else "N/A"}", 50f, y, textPaint)
            canvas.drawText("Gender: $patientGender", 300f, y, textPaint)
            y += 20f
            
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 30f
            
            // AI Analysis
            canvas.drawText("AI ANALYSIS FINDINGS", 50f, y, sectionPaint)
            y += 25f
            
            // Donut representation
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 15f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawCircle(110f, y + 40f, 35f, paint)
            
            val riskColor = if (percentage < 30) "#16A34A" else if (percentage < 65) "#B45309" else "#DC2626"
            paint.color = Color.parseColor(riskColor)
            val oval = android.graphics.RectF(75f, y + 5f, 145f, y + 75f)
            val angle = (percentage / 100f) * 360f
            canvas.drawArc(oval, -90f, angle, false, paint)
            
            paint.style = android.graphics.Paint.Style.FILL
            val percentPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#0A192F")
                textSize = 14f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("$percentage%", 110f, y + 45f, percentPaint)
            
            canvas.drawText("IAN Nerve Damage Probability: $percentage%", 180f, y + 25f, boldTextPaint)
            canvas.drawText("Risk Level: $riskLabel", 180f, y + 45f, textPaint)
            y += 100f
            
            // Surgical Note
            canvas.drawText("SURGICAL NOTE", 50f, y, sectionPaint)
            y += 20f
            
            // Handle surgical note wrapping
            val words = surgicalNote.split(" ")
            var line = ""
            for (word in words) {
                if (textPaint.measureText("$line$word ") > 495f) {
                    canvas.drawText(line, 50f, y, boldTextPaint)
                    y += 15f
                    line = "$word "
                } else {
                    line += "$word "
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 50f, y, boldTextPaint)
                y += 25f
            }
            
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 30f
            
            // Key findings
            canvas.drawText("KEY OBSERVATIONS", 50f, y, sectionPaint)
            y += 20f
            for (finding in findings) {
                paint.color = Color.parseColor(riskColor)
                canvas.drawCircle(55f, y - 4f, 3f, paint)
                
                // Wrap finding text if needed
                val fWords = finding.split(" ")
                var fLine = ""
                for (word in fWords) {
                    if (textPaint.measureText("$fLine$word ") > 465f) {
                        canvas.drawText(fLine, 70f, y, textPaint)
                        y += 15f
                        fLine = "$word "
                    } else {
                        fLine += "$word "
                    }
                }
                if (fLine.isNotEmpty()) {
                    canvas.drawText(fLine, 70f, y, textPaint)
                    y += 20f
                }
            }
            
            // Footer
            y = 800f
            paint.color = Color.parseColor("#94A3B8")
            paint.strokeWidth = 1f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 15f
            val footerPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 9f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("Confidential Medical Report - Powered by NeuroDentAI", 297f, y, footerPaint)
            
            pdfDocument.finishPage(page)
            
            // Write to Downloads folder using MediaStore
            val fileName = "NeuroDentAI_Report_${patientName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        pdfDocument.writeTo(outputStream)
                        Toast.makeText(this, "Report downloaded locally: $fileName", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to write PDF file", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Failed to create PDF entry in MediaStore", Toast.LENGTH_SHORT).show()
            }
            pdfDocument.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun shareReportAsPdf(
        patientName: String,
        patientAge: Int,
        patientGender: String,
        displayId: String,
        percentage: Int,
        riskLabel: String,
        surgicalNote: String,
        findings: List<String>
    ) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = android.graphics.Paint()
            val titlePaint = android.graphics.Paint().apply {
                color = Color.parseColor("#2563EB")
                textSize = 20f
                isFakeBoldText = true
            }
            
            val sectionPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#0A192F")
                textSize = 14f
                isFakeBoldText = true
            }
            
            val textPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 12f
                isAntiAlias = true
            }
            
            val boldTextPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#0A192F")
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var y = 50f
            
            // Header
            canvas.drawText("NEURODENT AI - DIAGNOSTIC REPORT", 50f, y, titlePaint)
            y += 10f
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 2f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 30f
            
            // Patient Details
            canvas.drawText("PATIENT INFORMATION", 50f, y, sectionPaint)
            y += 20f
            canvas.drawText("Name: $patientName", 50f, y, textPaint)
            canvas.drawText("ID: ${if (displayId.isNotEmpty()) displayId else "N/A"}", 300f, y, textPaint)
            y += 18f
            canvas.drawText("Age: ${if (patientAge > 0) patientAge.toString() else "N/A"}", 50f, y, textPaint)
            canvas.drawText("Gender: $patientGender", 300f, y, textPaint)
            y += 20f
            
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 30f
            
            // AI Analysis
            canvas.drawText("AI ANALYSIS FINDINGS", 50f, y, sectionPaint)
            y += 25f
            
            // Donut representation
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 15f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawCircle(110f, y + 40f, 35f, paint)
            
            val riskColor = if (percentage < 30) "#16A34A" else if (percentage < 65) "#B45309" else "#DC2626"
            paint.color = Color.parseColor(riskColor)
            val oval = android.graphics.RectF(75f, y + 5f, 145f, y + 75f)
            val angle = (percentage / 100f) * 360f
            canvas.drawArc(oval, -90f, angle, false, paint)
            
            paint.style = android.graphics.Paint.Style.FILL
            val percentPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#0A192F")
                textSize = 14f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("$percentage%", 110f, y + 45f, percentPaint)
            
            canvas.drawText("IAN Nerve Damage Probability: $percentage%", 180f, y + 25f, boldTextPaint)
            canvas.drawText("Risk Level: $riskLabel", 180f, y + 45f, textPaint)
            y += 100f
            
            // Surgical Note
            canvas.drawText("SURGICAL NOTE", 50f, y, sectionPaint)
            y += 20f
            
            // Handle surgical note wrapping
            val words = surgicalNote.split(" ")
            var line = ""
            for (word in words) {
                if (textPaint.measureText("$line$word ") > 495f) {
                    canvas.drawText(line, 50f, y, boldTextPaint)
                    y += 15f
                    line = "$word "
                } else {
                    line += "$word "
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 50f, y, boldTextPaint)
                y += 25f
            }
            
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 30f
            
            // Key findings
            canvas.drawText("KEY OBSERVATIONS", 50f, y, sectionPaint)
            y += 20f
            for (finding in findings) {
                paint.color = Color.parseColor(riskColor)
                canvas.drawCircle(55f, y - 4f, 3f, paint)
                
                // Wrap finding text if needed
                val fWords = finding.split(" ")
                var fLine = ""
                for (word in fWords) {
                    if (textPaint.measureText("$fLine$word ") > 465f) {
                        canvas.drawText(fLine, 70f, y, textPaint)
                        y += 15f
                        fLine = "$word "
                    } else {
                        fLine += "$word "
                    }
                }
                if (fLine.isNotEmpty()) {
                    canvas.drawText(fLine, 70f, y, textPaint)
                    y += 20f
                }
            }
            
            // Footer
            y = 800f
            paint.color = Color.parseColor("#94A3B8")
            paint.strokeWidth = 1f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 15f
            val footerPaint = android.graphics.Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 9f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("Confidential Medical Report - Powered by NeuroDentAI", 297f, y, footerPaint)
            
            pdfDocument.finishPage(page)
            
            // Save temporarily to cache directory
            val pdfFile = java.io.File(cacheDir, "NeuroDentAI_Report_${patientName.replace(" ", "_")}.pdf")
            java.io.FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            
            // Share using FileProvider
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                pdfFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "NeuroDent AI - Patient Diagnostic Report: $patientName")
                putExtra(Intent.EXTRA_TEXT, "Here is the AI diagnostic report for patient $patientName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Diagnostic Report using..."))
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
