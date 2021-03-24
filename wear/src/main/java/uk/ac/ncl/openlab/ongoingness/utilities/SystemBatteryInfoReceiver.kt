package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlin.math.abs


/**
 * Battery broadcast tag.
 */
const val BROADCAST_INTENT_NAME: String = "BATTERY_INFO"

/**
 * Receives battery related events from the system and sends events app related battery events.
 *
 * @param context the context of the application.
 *
 * @author Luis Carvalho
 */
class SystemBatteryInfoReceiver(private var context: Context) {

    /**
     * Broadcast receiver for system charging events.
     */
    private var chargeReceiver: BroadcastReceiver

    /**
     * Broadcast receiver for system battery level events.
     */
    private var batteryReceiver: BroadcastReceiver

    /**
     * Last checked level of battery.
     */
    private var lastBatteryValue: Float = 0f

    /**
     * Is the charge receiver on.
     */
    private var checkingChargeReceiverOn = false

    /**
     * Is the battery receiver on
     */
    private var checkingBatteryReceiverOn: Boolean = false

    /**
     * Delta of battery
     */
    private val chargeDelta: Float = 0.01f

    /**
     * Is charging.
     */
    var charging = false

    /**
     * Tag for print.
     */
    private val tag = "SystemBatteryInfoReceiver"

    init {

        this.chargeReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                //If the device is connected to charger, broadcast CHARGER_CONNECTED and
                //starts listening to battery events
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

                // If the device is disconnected from the charger, broadcast CHARGER_DISCONNECTED and
                //stops listening to battery events
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

    /**
     * Starts listing for charge events.
     */
    fun start() {
        startCheckingChargeState()
    }

    /**
     * Stops listing for charge and battery events.
     */
    fun stop() {
        if(checkingChargeReceiverOn)
            stopCheckingChargeState()
        if(checkingBatteryReceiverOn)
            stopCheckingBatteryValues()
    }


    /**
     * Starts listening for charge events.
     */
    private fun startCheckingChargeState() {

        val filter = IntentFilter(BatteryManager.ACTION_CHARGING).apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(chargeReceiver, filter)
        checkingChargeReceiverOn = true
    }

    /**
     * Stops listening to charge events.
     */
    private fun stopCheckingChargeState() {
        context.unregisterReceiver(chargeReceiver)
        checkingChargeReceiverOn = false
    }

    /**
     * Starts listening for battery events.
     */
    private fun startCheckingBatteryValues() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {}
        context.registerReceiver(batteryReceiver, filter)
        checkingBatteryReceiverOn = true
        charging = true
    }

    /**
     * Stops listening to battery events.
     */
    private fun stopCheckingBatteryValues() {
        context.unregisterReceiver(batteryReceiver)
        checkingBatteryReceiverOn = false
        lastBatteryValue = 0f
        charging = false
    }

    /**
     * Gets the current battery level.
     *
     * @param intent the intent of the app.
     *
     * @return current level of battery.
     */
    private fun getBatteryValue(intent: Intent): Float? {
        return intent.let { i ->
            val level: Int = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        }
    }

}