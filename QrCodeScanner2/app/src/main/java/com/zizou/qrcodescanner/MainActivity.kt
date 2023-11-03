package com.zizou.qrcodescanner

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface

import java.io.IOException
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val requestLocationPermission = 1
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            getLocation()
        }

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
    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestLocationPermission)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestLocationPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
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
    private fun showLocationDialog(latitude: Double, longitude: Double) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Coordonnées de localisation")
        alertDialogBuilder.setMessage("Latitude: $latitude\nLongitude: $longitude")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

}