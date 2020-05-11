package com.posithive.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {
    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "This device does not support NFC or NFC option is disabled.", Toast.LENGTH_LONG)
            return
        }

        sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        if (sharedPreferences != null) {
            editor = sharedPreferences!!.edit()
        }

        webView.webViewClient = WebViewClient()
        webView.loadUrl("http://dev.posithive.co.uk:8283")
        webView.settings.javaScriptEnabled = true
    }

    override fun onResume() {
        super.onResume()

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        if (nfcAdapter != null)
            nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()

        if (nfcAdapter != null)
            nfcAdapter!!.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent == null)
            return

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                if (messages.isNotEmpty()) {
                    val message = messages[0]
                    if (message.records.isNotEmpty()) {
                        val record = message.records[0]
                        val uuid = String(record.payload, Charset.forName("UTF-8"))
                        loginUsingUuid(uuid)
                    }
                }
            }
        }
    }

    private fun makeHex(bytes: ByteArray): String {
        var result = ""
        for (byte in bytes) {
            val value = String.format("%02X", byte)
            result += value
        }
        return result
    }

    private fun loginUsingUuid(uuid: String) {
        webView.evaluateJavascript("javascript:loginFromPhone('$uuid');", null)
    }
}
