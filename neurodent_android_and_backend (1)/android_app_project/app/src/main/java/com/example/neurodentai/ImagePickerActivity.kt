package com.example.neurodentai

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImagePickerActivity : AppCompatActivity() {

    private val client = NetworkConfig.client
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    
    private var doctorId: Int = -1
    private var patientName: String = ""
    private var patientAge: Int = 0
    private var patientGender: String = ""

    // Gallery picker
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showPreview(it) }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { showPreview(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)
        supportActionBar?.hide()

        doctorId = intent.getIntExtra("DOCTOR_ID", -1)
        patientName = intent.getStringExtra("PATIENT_NAME") ?: ""
        patientAge = intent.getStringExtra("PATIENT_AGE")?.toIntOrNull() ?: 0
        patientGender = intent.getStringExtra("PATIENT_GENDER") ?: ""

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val cameraButton = findViewById<LinearLayout>(R.id.cameraButton)
        val galleryButton = findViewById<LinearLayout>(R.id.galleryButton)
        val changeCameraButton = findViewById<LinearLayout>(R.id.changeCameraButton)
        val changeGalleryButton = findViewById<LinearLayout>(R.id.changeGalleryButton)
        val analyseButton = findViewById<Button>(R.id.analyseButton)

        backButton.setOnClickListener { finish() }

        cameraButton.setOnClickListener { openCamera() }
        galleryButton.setOnClickListener { galleryLauncher.launch("image/*") }

        changeCameraButton.setOnClickListener { openCamera() }
        changeGalleryButton.setOnClickListener { galleryLauncher.launch("image/*") }

        analyseButton.setOnClickListener {
            selectedImageUri?.let {
                Toast.makeText(this, "Analysing X-Ray...", Toast.LENGTH_SHORT).show()
                uploadImage(it)
            }
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
            return
        }

        val photoFile = createImageFile()
        cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        cameraLauncher.launch(cameraImageUri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("OPG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun showPreview(uri: Uri) {
        selectedImageUri = uri

        val noImageState = findViewById<LinearLayout>(R.id.noImageState)
        val previewState = findViewById<LinearLayout>(R.id.previewState)
        val analyseContainer = findViewById<FrameLayout>(R.id.analyseButtonContainer)
        val previewImageView = findViewById<ImageView>(R.id.previewImageView)
        val previewFileName = findViewById<TextView>(R.id.previewFileName)

        // Show preview, hide empty state
        noImageState.visibility = View.GONE
        previewState.visibility = View.VISIBLE
        analyseContainer.visibility = View.VISIBLE

        previewImageView.setImageURI(uri)

        // Show file name
        val fileName = uri.lastPathSegment ?: "xray_image.jpg"
        previewFileName.text = fileName
    }

    private fun uploadImage(uri: Uri) {
        val file = getFileFromUri(uri) ?: run {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/predict")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ImagePickerActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                
                if (response.isSuccessful && responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val percentage = json.getDouble("percentage")
                        
                        // Proceed to save the patient
                        savePatientToBackend(percentage, uri)
                        
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ImagePickerActivity, "Error parsing response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorMsg = try {
                        val errorJson = JSONObject(responseData ?: "")
                        errorJson.optString("detail", "Server error on prediction")
                    } catch (e: Exception) {
                        "Server error on prediction"
                    }
                    runOnUiThread {
                        Toast.makeText(this@ImagePickerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun savePatientToBackend(percentage: Double, uri: Uri) {
        if (doctorId == -1) {
            // Can't save without doctor ID, just go to result
            navigateToResult(percentage, uri, -1, "", patientAge, patientGender)
            return
        }

        val jsonObj = JSONObject().apply {
            put("doctor_id", doctorId)
            put("name", patientName)
            put("age", patientAge)
            put("gender", patientGender)
            put("nerve_damage", percentage)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonObj.toString()
        )

        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/patients")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ImagePickerActivity, "Failed to save patient", Toast.LENGTH_SHORT).show()
                    navigateToResult(percentage, uri, -1, "", patientAge, patientGender)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                var patientDbId = -1
                var patientDisplayId = ""
                if (response.isSuccessful && bodyStr != null) {
                    try {
                        val json = JSONObject(bodyStr)
                        patientDbId = json.optInt("id", -1)
                        patientDisplayId = json.optString("display_id", "")
                    } catch (e: Exception) {}
                }
                val finalDbId = patientDbId
                val finalDisplayId = patientDisplayId
                runOnUiThread {
                    navigateToResult(percentage, uri, finalDbId, finalDisplayId, patientAge, patientGender)
                }
            }
        })
    }
    
    private fun navigateToResult(percentage: Double, uri: Uri, dbId: Int, displayId: String, age: Int, gender: String) {
        val intent = Intent(this@ImagePickerActivity, ResultActivity::class.java).apply {
            putExtra("IMAGE_URI", uri.toString())
            putExtra("PERCENTAGE", percentage)
            putExtra("PATIENT_NAME", patientName)
            putExtra("PATIENT_AGE", age)
            putExtra("PATIENT_GENDER", gender)
            putExtra("PATIENT_DISPLAY_ID", displayId)
            putExtra("PATIENT_DB_ID", dbId)
        }
        startActivity(intent)
        finish()
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("opg_analyse", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { output -> inputStream?.copyTo(output) }
            inputStream?.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
