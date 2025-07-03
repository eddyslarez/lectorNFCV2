package com.eddyslarez.lectornfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNFC()
        setContent {
            MifareNFCApp()
        }
    }

    private fun setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC no disponible en este dispositivo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }

        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf(MifareClassic::class.java.name))
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let { processTag(it) }
        }
    }

    private fun processTag(tag: Tag) {
        val mifare = MifareClassic.get(tag)
        if (mifare != null) {
            AdvancedMifareManager.processNewTag(mifare)
        }
    }
}
//class MainActivity : ComponentActivity() {
//    private var nfcAdapter: NfcAdapter? = null
//    private var pendingIntent: PendingIntent? = null
//    private var intentFiltersArray: Array<IntentFilter>? = null
//    private var techListsArray: Array<Array<String>>? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setupNFC()
//
//        setContent {
//            MifareNFCApp()
//        }
//    }
//
//    private fun setupNFC() {
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//
//        if (nfcAdapter == null) {
//            Toast.makeText(this, "NFC no disponible en este dispositivo", Toast.LENGTH_LONG).show()
//            finish()
//            return
//        }
//
//        pendingIntent = PendingIntent.getActivity(
//            this, 0,
//            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
//            PendingIntent.FLAG_MUTABLE
//        )
//
//        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
//        try {
//            ndef.addDataType("*/*")
//        } catch (e: IntentFilter.MalformedMimeTypeException) {
//            throw RuntimeException("fail", e)
//        }
//        intentFiltersArray = arrayOf(ndef)
//        techListsArray = arrayOf(arrayOf(MifareClassic::class.java.name))
//    }
//
//    override fun onResume() {
//        super.onResume()
//        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        nfcAdapter?.disableForegroundDispatch(this)
//    }
//
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        setIntent(intent)
//
//        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
//            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
//            tag?.let { processTag(it) }
//        }
//    }
//
//    private fun processTag(tag: Tag) {
//        val mifare = MifareClassic.get(tag)
//        if (mifare != null) {
//            MifareManager.processNewTag(mifare)
//        }
//    }
//}