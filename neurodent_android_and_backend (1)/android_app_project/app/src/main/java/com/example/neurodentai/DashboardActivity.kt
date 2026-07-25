package com.example.neurodentai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    private val client = NetworkConfig.client
    private var doctorId: Int = -1
    private var doctorName: String = "Dr. Dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()
        
        // Get user data from intent
        doctorId = intent.getIntExtra("DOCTOR_ID", -1)
        val nameExtra = intent.getStringExtra("DOCTOR_NAME")
        if (nameExtra != null && nameExtra.isNotEmpty()) {
            doctorName = nameExtra
        }
        
        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        tvDoctorName.text = "Dr. $doctorName"

        val uploadCard = findViewById<LinearLayout>(R.id.uploadCard)
        uploadCard.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
        }

        val navUpload = findViewById<LinearLayout>(R.id.navUpload)
        navUpload.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
        }

        val navReports = findViewById<LinearLayout>(R.id.navReports)
        navReports.setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
        }

        val navProfile = findViewById<LinearLayout>(R.id.navProfile)
        navProfile.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("DOCTOR_ID", doctorId)
                putExtra("DOCTOR_NAME", doctorName)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (doctorId != -1) {
            fetchDashboardData()
        }
    }

    private fun fetchDashboardData() {
        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/dashboard/$doctorId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "Failed to load dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val json = JSONObject(responseData)
                            val totalPatients = json.getInt("total_patients")
                            val todaysPatients = json.getInt("todays_patients")
                            val recentActivity = json.getJSONArray("recent_activity")
                            
                            findViewById<TextView>(R.id.tvTotalPatients).text = totalPatients.toString()
                            findViewById<TextView>(R.id.tvTodaysPatients).text = todaysPatients.toString()
                            
                            val layoutRecent = findViewById<LinearLayout>(R.id.layoutRecentActivity)
                            layoutRecent.removeAllViews()
                            
                            for (i in 0 until recentActivity.length()) {
                                val item = recentActivity.getJSONObject(i)
                                val view = LayoutInflater.from(this@DashboardActivity)
                                    .inflate(R.layout.item_recent_activity, layoutRecent, false)
                                
                                val tvPatientId = view.findViewById<TextView>(R.id.tvPatientId)
                                val tvNerveDamage = view.findViewById<TextView>(R.id.tvNerveDamage)
                                val tvRiskLevel = view.findViewById<TextView>(R.id.tvRiskLevel)
                                val iconContainer = view.findViewById<FrameLayout>(R.id.iconContainer)
                                val iconImage = view.findViewById<ImageView>(R.id.iconImage)
                                
                                tvPatientId.text = item.optString("patient_id_display", "Patient")
                                tvNerveDamage.text = "Nerve damage: ${item.optDouble("nerve_damage", 0.0)}%"
                                
                                val riskLevel = item.optString("risk_level", "Low Risk")
                                tvRiskLevel.text = riskLevel
                                
                                if (riskLevel == "High Risk") {
                                    tvRiskLevel.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                                    tvRiskLevel.setBackgroundResource(R.drawable.bg_badge_red)
                                    iconContainer.setBackgroundResource(R.drawable.bg_icon_red)
                                    iconImage.setImageResource(R.drawable.ic_warning)
                                } else if (riskLevel == "Medium Risk") {
                                    tvRiskLevel.setTextColor(android.graphics.Color.parseColor("#D97706"))
                                    tvRiskLevel.setBackgroundResource(R.drawable.bg_badge_red) // Use yellow if available, red for now
                                    iconContainer.setBackgroundResource(R.drawable.bg_icon_red)
                                    iconImage.setImageResource(R.drawable.ic_warning)
                                } else {
                                    tvRiskLevel.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                                    tvRiskLevel.setBackgroundResource(R.drawable.bg_badge_green)
                                    iconContainer.setBackgroundResource(R.drawable.bg_icon_blue)
                                    iconImage.setImageResource(R.drawable.ic_people)
                                }
                                
                                val btnDelete = view.findViewById<android.widget.ImageButton>(R.id.btnDelete)
                                
                                view.setOnClickListener {
                                    val intent = Intent(this@DashboardActivity, ResultActivity::class.java).apply {
                                        putExtra("PERCENTAGE", item.optDouble("nerve_damage", 0.0))
                                        putExtra("PATIENT_NAME", item.optString("name", "Anonymous"))
                                        putExtra("PATIENT_AGE", item.optInt("age", 0))
                                        putExtra("PATIENT_GENDER", item.optString("gender", "N/A"))
                                        putExtra("PATIENT_DISPLAY_ID", item.optString("patient_id_display", ""))
                                        putExtra("PATIENT_DB_ID", item.optInt("id", -1))
                                    }
                                    startActivity(intent)
                                }
                                
                                btnDelete.setOnClickListener {
                                    deletePatient(item.optInt("id", -1))
                                }
                                
                                layoutRecent.addView(view)
                            }
                            
                        } catch (e: Exception) {
                            Toast.makeText(this@DashboardActivity, "Error parsing data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun deletePatient(patientId: Int) {
        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/patients/$patientId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DashboardActivity, "Patient deleted", Toast.LENGTH_SHORT).show()
                        fetchDashboardData() // Refresh dashboard
                    } else {
                        Toast.makeText(this@DashboardActivity, "Error deleting patient", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
