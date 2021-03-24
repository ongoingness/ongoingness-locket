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

/**
 * Event-driven state machine in charge of interconnecting and commanding the model and view layers of the watchface.
 *
 * @param context context of the application.
 * @param batteryChecking if true this controller expects to receive events from the battery.
 * @param presenter presenter to be used by this controller.
 * @author Luis Carvalho
 */
class WatchFaceController(var context: Context, var batteryChecking: Boolean, var presenter: WatchFacePresenter) {

    /**
     * Current state of this controller.
     */
    private var currentControllerState: ControllerState = ControllerState.STANDBY

    /**
     * Previous state of this controller.
     */
    private var previousControllerState: ControllerState = ControllerState.UNKNOWN

    /**
     * Receiver in charge of receiving events from the system about the battery and sending app based events.
     */
    private var systemBatteryInfoReceiver: SystemBatteryInfoReceiver? = null

    /**
     * Receiver in charge of receiving
     */
    private var batteryReceiver: BroadcastReceiver? = null

    /**
     * Battery level.
     */
    private var battery = 0f

    /**
     *  watchface in ambient mode
     */
    private var inAmbientMode = false

    /**
     * watchface in low bit ambient mode.
     */
    private var mLowBitAmbient = false

    /**
     * watchface burn in protection.
     */
    private var mBurnInProtection = false

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

                                        ControllerState.STANDBY ->  {
                                            battery = intent.getFloatExtra("battery", 0f)
                                            presenter.displayChargingCover(battery)
                                            updateState(ControllerState.CHARGING)
                                        }
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
                                        startActivity(false)
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

    /**
     * Terminates the battery receiver.
     */
    fun stop() {
        if(batteryChecking) {
            systemBatteryInfoReceiver!!.stop()
            context.unregisterReceiver(batteryReceiver)
        }
    }

    /**
     * Stores the current state of this controller as the previous state and sets the given state as the current.
     *
     * @param controllerState the new state of this controller.
     */
    fun updateState(controllerState: ControllerState){
        synchronized(currentControllerState) {
            if (controllerState == currentControllerState)
                return //no change, so no need to notify the change

            Log.d("watch face state update", "$currentControllerState")

            previousControllerState = currentControllerState
            currentControllerState = controllerState
        }
    }

    /**
     * Starts the app with a tap.
     */
    fun tapEvent() {
        startActivity(true)
    }

    /**
     * Starts the app and stores the given ambient mode state.
     * @param state the new ambient mode state.
     */
    fun ambientModeChanged(state: Boolean) {
        inAmbientMode = state
        if(!state)
            startActivity(false)
    }

    /**
     * Sets the low bit ambient mode.
     * @param state the new low bit ambient mode state.
     */
    fun lowBitAmbientChanged(state: Boolean) {
        mLowBitAmbient = state
    }

    /**
     * Sets the burn in protection.
     * @param state the new burn in protection state.
     */
    fun burnInProtectionChanged(state: Boolean) {
        mBurnInProtection = state
    }

    /**
     * Starts the app.
     * @param startedtWithTap stores if the app started with a tap.
     */
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