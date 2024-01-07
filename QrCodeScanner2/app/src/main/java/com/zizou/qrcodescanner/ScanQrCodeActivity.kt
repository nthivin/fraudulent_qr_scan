// SCAN ACTIVITY USING THE CAMERA

package com.zizou.qrcodescanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class ScanQrCodeActivity : AppCompatActivity() {

    companion object {
        const val QR_CODE_KEY = "qr_code_key"
        private const val CAMERA_REQUEST_CODE = 23
    }

    private lateinit var scanSurfaceView: SurfaceView
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr_code)

        scanSurfaceView = findViewById(R.id.scan_surface_view)

        initBarcodeDetector()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (cameraPermissionGranted(requestCode, grantResults)) {
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)

        } else {
            Toast.makeText(this, "Camera is mandatory to scan QR codes", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun cameraPermissionGranted(requestCode: Int, grantResults: IntArray): Boolean {
        return requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun initBarcodeDetector() {
        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        initCameraSource()
        initScanSurfaceView()

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems

                if (barcodes.isNotEmpty()) {
                    barcodes.forEach { _, barcode ->
                        if (barcode.displayValue.isNotEmpty()) {
                            onQrCodeScanned(barcode.displayValue)
                        }
                    }
                }
            }
        })
    }

    private fun onQrCodeScanned(value: String) {
        val intent = Intent()
        intent.putExtra(QR_CODE_KEY, value)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun initCameraSource() {
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()
    }

    private fun initScanSurfaceView() {
        scanSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(p0: SurfaceHolder) {
                if (ActivityCompat.checkSelfPermission(this@ScanQrCodeActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(scanSurfaceView.holder)
                } else {
                    ActivityCompat.requestPermissions(this@ScanQrCodeActivity, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
                }
            }
            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.release()
            }
        })
    }
}