package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

private const val TAG = "PowerConnectionReceiver"

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, "WHATATATATATA")

        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d(TAG, log)
                Toast.makeText(context, log, Toast.LENGTH_LONG).show()
            }
        }
    }
}