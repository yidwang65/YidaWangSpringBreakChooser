package com.example.yidawangspringbreakchooser


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.random.Random


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private val SHAKE_THRESHOLD = 600

    private lateinit var editText: EditText
    private lateinit var listView: ListView
    private lateinit var mediaPlayer: MediaPlayer
    val RecordAudioRequestCode = 1
    private var speechRecognizer: SpeechRecognizer? = null

    private var last_x = 0f
    private var last_y = 0f
    private var last_z = 0f



    private var language =""
    private val audioMap: Map<String, Int> = mapOf(
        "Spanish" to R.raw.spanish_audio,
        "French" to R.raw.french_audio,
        "Chinese" to R.raw.chinese_audio
    )
    private val spainishlocation = arrayOf("geo:19.4326,-99.1332", "geo:41.3851,2.1734")
    private val frenchlocation = arrayOf("geo:48.8566,2.3522", "geo:45.75,4.85")
    private val chineselocation = arrayOf("geo:39.9042,116.4074", "geo:31.2304,121.4737")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        listView = findViewById(R.id.listView)

        // Populate the list of languages
        val languages = arrayOf("Spanish", "French", "Chinese")
        val adapter = ArrayAdapter(this, R.layout.list_item, languages)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission()
        }

        listView.adapter = adapter
        // Set item click listener for the list view
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedLanguage = listView.getItemAtPosition(position) as String
            language = selectedLanguage
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)


            val desiredLanguage = when (language) {
                "Spanish" -> "es"
                "French" -> "fr"
                "Chinese" -> "zh_CN"
                else -> "en"
            }
            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, desiredLanguage)
            Log.d("MainActivity", "desire language is $desiredLanguage and Language is ${speechRecognizerIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)}")
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Speech recognition is ready
                    Log.d("MainActivity", "onReadyForSpeech---------")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("MainActivity", "onBeginningOfSpeech---------")
                }

                override fun onRmsChanged(rmsdB: Float) {
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    Log.d("MainActivity", "onBufferReceived---------")
                }

                override fun onEndOfSpeech() {
                    // Speech input has ended
                    Log.d("MainActivity", "mic recording")
                    editText.setText("")
                    editText.setHint("Listening...")
                }

                override fun onError(error: Int) {
                    Log.d("MainActivity", "onError---------"+error.toString())
                }

                override fun onResults(results: Bundle?) {
                    // Speech recognition results
                    Log.d("MainActivity", "got result")
                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    editText.setText(data!![0])
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    Log.d("MainActivity", "onPartialResults---------")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d("MainActivity", "onEvent---------")
                }
            }

            speechRecognizer?.setRecognitionListener(listener)

            speechRecognizer?.startListening(speechRecognizerIntent)
        }



        // Initialize the sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lastUpdate = System.currentTimeMillis()

        // Initialize media player
        mediaPlayer = MediaPlayer.create(this, R.raw.english_audio)

    }


    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 100) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
                if (speed > SHAKE_THRESHOLD) {
                    Log.d("MainActivity", "phone shaked")
                    // Shake detected, navigate to Google Maps
                    Log.d("MainActivity", language)
                    val selectedLanguage = language as? String
                    val randomIndex = Random.nextInt(2)
                    val locationUri = when (selectedLanguage) {
                        "Spanish" -> spainishlocation[randomIndex]
                        "French" -> frenchlocation[randomIndex] // Paris
                        "Chinese" -> chineselocation[randomIndex] // Beijing
                        else -> "geo:0,0"
                    }
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(locationUri))
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                    // Play audio clip
                    val audioResource = audioMap[selectedLanguage] ?: R.raw.english_audio
                    mediaPlayer = MediaPlayer.create(this, audioResource)
                    Log.d("MainActivity", selectedLanguage.toString())
                    mediaPlayer.start()
                }
                last_x = x
                last_y = y
                last_z = z
            }
        }
    }
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RecordAudioRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RecordAudioRequestCode && grantResults.size > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(
                this,
                "Permission Granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val RecordAudioRequestCode = 1
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }
}


