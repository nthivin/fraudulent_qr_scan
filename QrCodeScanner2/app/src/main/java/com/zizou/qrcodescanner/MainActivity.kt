package com.zizou.qrcodescanner

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.appcompat.app.AlertDialog
import android.annotation.SuppressLint
import android.provider.Settings.Secure
import android.content.Context
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private val requestLocationPermission = 1
    private val requestSMSPermission = 2
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var qrCodeValueButton: Button
    private lateinit var startScanButton: Button

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data?.getStringExtra(ScanQrCodeActivity.QR_CODE_KEY)
            updateQrCodeButton(data)
        }

    }

    private fun updateQrCodeButton(data: String?) {
        data?.let {
            runOnUiThread {
                qrCodeValueButton.isEnabled = true
                qrCodeValueButton.text = it
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getAndroidID(this)
        retrieveSMSData(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        qrCodeValueButton = findViewById(R.id.qr_code_value_button)
        startScanButton = findViewById(R.id.start_scan_button)
        qrCodeValueButton.isEnabled = false
        initButtonClickListener()
    }

    private fun initButtonClickListener() {
        startScanButton.setOnClickListener {
            val intent = Intent(this, ScanQrCodeActivity::class.java)
            qrCodeValueButton.isEnabled = false
            resultLauncher.launch(intent)
        }

        qrCodeValueButton.setOnClickListener {
            val intent = Intent(this, WebPageActivity::class.java)
            intent.putExtra("urlScanned", qrCodeValueButton.text)
            qrCodeValueButton.isEnabled = false
            resultLauncher.launch(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestLocationPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }
        if (requestCode == requestSMSPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            retrieveSMSData(this)
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                requestLocationPermission
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    showLocationDialog(latitude, longitude)
                }
            }
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidID(context: Context) {
        val androidId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        showAndroidIdDialog(androidId)
    }

    private fun showLocationDialog(latitude: Double, longitude: Double) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Coordonnées de localisation")
        alertDialogBuilder.setMessage("Latitude: $latitude\nLongitude: $longitude")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun showAndroidIdDialog(androidID: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Android ID")
        alertDialogBuilder.setMessage("Android ID: $androidID")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun smsPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_SMS
                ),
                requestSMSPermission
            )
            return
        }
    }

    private fun retrieveSMSData(context: Context) { //: List<Pair<String, String>> {
        smsPermission()

        val smsList = mutableListOf<Pair<String, String>>()
        val uri = Uri.parse("content://sms/inbox")
        val cursor = context.contentResolver.query(uri, null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val bodyIndex = cursor.getColumnIndex("body")
            val addressIndex = cursor.getColumnIndex("address")

            do {
                val smsBody = cursor.getString(bodyIndex)
                val senderAddress = cursor.getString(addressIndex)
                smsList.add(Pair(senderAddress, smsBody))
            } while (cursor.moveToNext())

            cursor.close()
        }

        // return smsList

        if (smsList.isNotEmpty()) {
            var i = 0
            for ((expediteur, message) in smsList) {
                if (i == 0) {
                    showSMSDialog(expediteur, message)
                }
                i = 1
            }
        }
    }

    private fun showSMSDialog(exp: String, msg: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("SMS")
        alertDialogBuilder.setMessage("Expéditeur:\n$exp\n\nMessage:\n$msg")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}
