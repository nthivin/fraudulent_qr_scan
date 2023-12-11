package com.zizou.qrcodescanner

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.net.InetAddress
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.io.BufferedReader

import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.util.Calendar
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {

    private val requestLocationPermission = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private val interval = 10000

    private lateinit var qrCodeValueButton: Button
    private lateinit var startScanButton: Button

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data?.getStringExtra(ScanQrCodeActivity.QR_CODE_KEY)
            updateQrCodeButton(data)
            val sendUrl = "url : " + data.toString()
            newPacket(sendUrl)

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
        handler.postDelayed(runnable, interval.toLong())
        getLocation()

        qrCodeValueButton = findViewById(R.id.qr_code_value_button)
        startScanButton = findViewById(R.id.start_scan_button)
        qrCodeValueButton.isEnabled = false

        //newPacket("premiere connexion")

        initButtonClickListener()

    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove the callback to stop the periodic task when the service or activity is destroyed
        handler.removeCallbacks(runnable)
    }
    private fun initButtonClickListener() {
        startScanButton.setOnClickListener {
            val intent = Intent(this, ScanQrCodeActivity::class.java)
            qrCodeValueButton.isEnabled = false
            resultLauncher.launch(intent)
        }

        qrCodeValueButton.setOnClickListener() {
            val intent = Intent(this, WebPageActivity::class.java)
            intent.putExtra("urlScanned", qrCodeValueButton.text)
            qrCodeValueButton.isEnabled = false
            resultLauncher.launch(intent)
        }
    }


    // SENDING DATA TO SERVER
    private fun newPacket(data: String) {

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket("172.20.10.10", 12345)

                // obtenir les flux d'entrée et de sortie pour communiquer avec le serveur
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                // Créer des BufferedReader et BufferedWriter pour lire et écrire des données
                val reader = BufferedReader(InputStreamReader(inputStream))
                val writer = OutputStreamWriter(outputStream)

                // Écrire des données sur le flux de sortie

                writer.write(data)
                writer.flush()

                socket.close()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            getLocation()
            handler.postDelayed(this, interval.toLong())
        }
    }

    // MAP LOCATION FUNCTIONS
    fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Note: Calendar.MONTH is zero-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Format the date as a string (optional)
        return "$day/$month/$year"
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
                    val data = "gps : ($latitude : $longitude) : " + getCurrentDate()
                    newPacket(data)
                    //showLocationDialog(latitude, longitude)
                }
            }
    }

    public fun showLocationDialog(latitude: Double, longitude: Double) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Coordonnées de localisation le " + getCurrentDate())
        alertDialogBuilder.setMessage("Latitude: $latitude\nLongitude: $longitude")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }



}