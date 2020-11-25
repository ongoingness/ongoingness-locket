package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.utilities.BROADCAST_INTENT_NAME
import uk.ac.ncl.openlab.ongoingness.utilities.BatteryEvent
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.SystemBatteryInfoReceiver
import uk.ac.ncl.openlab.ongoingness.presenters.WatchFacePresenter
import uk.ac.ncl.openlab.ongoingness.views.MainActivity

class WatchFaceController(var context: Context, var batteryChecking: Boolean, var presenter: WatchFacePresenter) {

    private var currentControllerState: ControllerState = ControllerState.STANDBY
    private var previousControllerState: ControllerState = ControllerState.UNKNOWN

    private var systemBatteryInfoReceiver: SystemBatteryInfoReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null

    private var battery = 0f

    init {

        if(batteryChecking) {
            systemBatteryInfoReceiver = SystemBatteryInfoReceiver(context)

            batteryReceiver = object: BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {

                    if(intent.hasExtra("event")) {

                        when(intent.getStringExtra("event")) {

                            BatteryEvent.CHARGER_CONNECTED.toString() -> {

                                if(intent.hasExtra("battery")) {

                                    when(currentControllerState) {

                                        ControllerState.STANDBY -> updateState(ControllerState.CHARGING)
                                        else -> {}
                                    }
                                }
                            }

                            BatteryEvent.BATTERY_CHANGED.toString() -> {

                                if(intent.hasExtra("battery")) {

                                    when(currentControllerState) {

                                        ControllerState.CHARGING -> {
                                            battery = intent.getFloatExtra("battery", 0f)
                                            presenter.displayChargingCover(battery)
                                        }
                                        else -> {}

                                    }

                                }

                            }

                            BatteryEvent.CHARGER_DISCONNECTED.toString() -> {

                                when(currentControllerState) {

                                    ControllerState.CHARGING -> {
                                        presenter.displayCover(CoverType.BLACK)
                                        updateState(ControllerState.STANDBY)
                                    }
                                    else -> {}

                                }

                            }
                        }
                    }
                }
            }

            val filter = IntentFilter(BROADCAST_INTENT_NAME).apply {}
            context.registerReceiver(batteryReceiver, filter)


            systemBatteryInfoReceiver!!.start()

        }
    }

    fun stop() {
        if(batteryChecking) {
            systemBatteryInfoReceiver!!.stop()
            context.unregisterReceiver(batteryReceiver)
        }
    }


    fun updateState(controllerState: ControllerState){
        synchronized(currentControllerState) {
            if (controllerState == currentControllerState)
                return //no change, so no need to notify of change


            Log.d("watch face state update", "$currentControllerState")

            previousControllerState = currentControllerState
            currentControllerState = controllerState
        }
    }

    fun tapEvent() {
        startActivity(true)
    }


    fun ambientModeChanged() {
        startActivity(false)
    }


    private fun startActivity(startedtWithTap: Boolean) {

        when(currentControllerState) {

            ControllerState.STANDBY -> {

                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                intent.putExtra("startedWithTap", startedtWithTap)
                intent.putExtra("state", currentControllerState.toString())
                intent.putExtra("battery", battery)

                context.startActivity(intent)
            }
        }
    }

}