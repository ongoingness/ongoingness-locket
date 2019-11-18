package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.BatteryManager
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.R
import java.io.ByteArrayOutputStream
import kotlin.math.abs

const val BROADCAST_INTENT_NAME: String = "BATTERY_INFO"

class SystemBatteryInfoReceiver(private var context: Context) {

    private var chargeReceiver: BroadcastReceiver
    private var batteryReceiver: BroadcastReceiver

    private var lastBatteryValue: Float = 0f
    private var checkingChargeReceiverOn = false
    private var checkingBatteryReceiverOn: Boolean = false

    private val chargeDelta: Float = 0.01f
    var charging = false

    private val tag = "SystemBatteryInfoReceiver"

    init {

        this.chargeReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                if(intent.action == Intent.ACTION_POWER_CONNECTED) {
                    Log.d(tag, " Connected")
                    Logger.log(LogType.CHARGER_CONNECTED, listOf(), context)

                    val battery = getBatteryValue(intent)
                    if(battery != null) {
                        val broadcastIntent = Intent(BROADCAST_INTENT_NAME)
                        broadcastIntent.putExtra("battery", battery)
                        broadcastIntent.putExtra("event", BatteryEvent.CHARGER_CONNECTED.toString())
                        context.sendBroadcast(broadcastIntent)

                    }
                    startCheckingBatteryValues()
                } else if(intent.action == Intent.ACTION_POWER_DISCONNECTED) {
                    Log.d(tag, " Disconnected")
                    Logger.log(LogType.CHARGER_DISCONNECTED, listOf(), context)
                    if(checkingBatteryReceiverOn)
                        stopCheckingBatteryValues()

                    val broadcastIntent = Intent(BROADCAST_INTENT_NAME)
                    broadcastIntent.putExtra("event", BatteryEvent.CHARGER_DISCONNECTED.toString())
                    context.sendBroadcast(broadcastIntent)
                }

            }

        }

        this.batteryReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                val battery = getBatteryValue(intent)

                if(battery != null && abs(battery - lastBatteryValue) > chargeDelta) {
                    val broadcastIntent = Intent(BROADCAST_INTENT_NAME)
                    broadcastIntent.putExtra("battery", battery)
                    broadcastIntent.putExtra("event", BatteryEvent.BATTERY_CHANGED.toString())
                    context.sendBroadcast(broadcastIntent)
                    lastBatteryValue = battery
                }

            }
        }
    }

    fun start() {
        startCheckingChargeState()
    }

    fun stop() {
        if(checkingChargeReceiverOn)
            stopCheckingChargeState()
        if(checkingBatteryReceiverOn)
            stopCheckingBatteryValues()
    }


    private fun startCheckingChargeState() {

        val filter = IntentFilter(BatteryManager.ACTION_CHARGING).apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(chargeReceiver, filter)
        checkingChargeReceiverOn = true
    }

    private fun stopCheckingChargeState() {
        context.unregisterReceiver(chargeReceiver)
        checkingChargeReceiverOn = false
    }

    private fun startCheckingBatteryValues() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {}
        context.registerReceiver(batteryReceiver, filter)
        checkingBatteryReceiverOn = true
        charging = true
    }

    private fun stopCheckingBatteryValues() {
        context.unregisterReceiver(batteryReceiver)
        checkingBatteryReceiverOn = false
        lastBatteryValue = 0f
        charging = false
    }

    private fun getBatteryValue(intent: Intent): Float? {
        return intent.let { i ->
            val level: Int = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        }
    }

}