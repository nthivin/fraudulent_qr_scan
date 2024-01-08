package com.zizou.qrcodescanner

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts


import java.io.IOException
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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

        qrCodeValueButton.setOnClickListener() {
            val intent = Intent(this, WebPageActivity::class.java)
            intent.putExtra("urlScanned", qrCodeValueButton.text)
            qrCodeValueButton.isEnabled = false
            resultLauncher.launch(intent)
        }
    }

}