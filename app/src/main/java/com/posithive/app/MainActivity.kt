package com.posithive.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

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
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        loginUsingSavedData()
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

        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
        if (tag != null) {
            val tagId = tag.id.toString()
            Toast.makeText(this, tagId, Toast.LENGTH_SHORT)
            loginUsingUuid(tagId)
        }
    }

    private fun loginUsingSavedData() {
        if (sharedPreferences == null)
            return

        val uuid = sharedPreferences!!.getString("uuid", "")
        if (uuid!!.isEmpty())
            return

        loginUsingUuid(uuid)
    }

    private fun loginUsingUuid(uuid: String) {
        webView.evaluateJavascript("javascript: loginFromPhone($uuid)", null)
    }

    class WebAppInterface(private val mainActivity: MainActivity) {
        @JavascriptInterface
        fun saveUuidOnPhone(uuid: String) {
            if (mainActivity.editor != null) {
                mainActivity.editor!!.putString("uuid", uuid)
                mainActivity.editor!!.commit()
            }
        }

        @JavascriptInterface
        fun logOutFromPhone() {
            if (mainActivity.editor != null) {
                mainActivity.editor!!.remove("uuid")
                mainActivity.editor!!.commit()
            }
        }
    }
}
