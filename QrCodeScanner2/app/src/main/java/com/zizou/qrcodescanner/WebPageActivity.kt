package com.zizou.qrcodescanner

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class WebPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_page)

        val intent = intent
        if (intent != null) {
            val newUrl = intent.getStringExtra("urlScanned")

            val myUrl: String = newUrl ?: "https://www.google.com/"


            val webView: WebView = findViewById(R.id.webview)
            webView.settings.javaScriptEnabled = true
            webView.loadUrl(myUrl)
            webView.webViewClient = WebViewClient()
        }
    }
}