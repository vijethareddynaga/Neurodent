package com.example.neurodentai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UploadActivity : AppCompatActivity() {

    private var selectedGender: String = "Male" // Default gender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        supportActionBar?.hide()

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val saveContinueButton = findViewById<Button>(R.id.saveContinueButton)
        val genderMale = findViewById<TextView>(R.id.genderMale)
        val genderFemale = findViewById<TextView>(R.id.genderFemale)

        backButton.setOnClickListener { finish() }

        // Gender selection logic
        genderMale.setOnClickListener {
            selectedGender = "Male"
            genderMale.setTextColor(android.graphics.Color.parseColor("#2563EB"))
            genderMale.setBackgroundResource(R.drawable.bg_gender_selected)
            genderMale.setTypeface(null, android.graphics.Typeface.BOLD)

            genderFemale.setTextColor(android.graphics.Color.parseColor("#64748B"))
            genderFemale.setBackgroundResource(0)
            genderFemale.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        genderFemale.setOnClickListener {
            selectedGender = "Female"
            genderFemale.setTextColor(android.graphics.Color.parseColor("#2563EB"))
            genderFemale.setBackgroundResource(R.drawable.bg_gender_selected)
            genderFemale.setTypeface(null, android.graphics.Typeface.BOLD)

            genderMale.setTextColor(android.graphics.Color.parseColor("#64748B"))
            genderMale.setBackgroundResource(0)
            genderMale.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        saveContinueButton.setOnClickListener {
            val patientName = findViewById<android.widget.EditText>(R.id.patientNameInput).text.toString().trim()
            val age = findViewById<android.widget.EditText>(R.id.ageInput).text.toString().trim()
            val surgeonNotes = findViewById<android.widget.EditText>(R.id.surgeonNotesInput).text.toString().trim()

            if (patientName.isEmpty() || age.isEmpty()) {
                Toast.makeText(this, "Please fill in patient name and age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to image picker / analysis screen
            val intent = Intent(this, ImagePickerActivity::class.java).apply {
                putExtra("DOCTOR_ID", intent.getIntExtra("DOCTOR_ID", -1))
                putExtra("PATIENT_NAME", patientName)
                putExtra("PATIENT_AGE", age)
                putExtra("PATIENT_GENDER", selectedGender)
                putExtra("SURGEON_NOTES", surgeonNotes)
            }
            startActivity(intent)
        }
    }
}
