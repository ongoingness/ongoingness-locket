package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.recognisers.RecogniserEvent
import uk.ac.ncl.openlab.ongoingness.utilities.BatteryEvent
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import java.util.Observer

abstract class AbstractController(
        var context: Context,
        private val recogniser: AbstractRecogniser,
        private val presenter: Presenter,
        private val contentCollection: AbstractContentCollection) {

    private var currentControllerState: ControllerState = ControllerState.STANDBY
    private var previousControllerState: ControllerState = ControllerState.UNKNOWN

    private val recogniserObserver = Observer { _, arg ->

        when(arg) {

            RecogniserEvent.STARTED -> onStartedEvent()
            RecogniserEvent.STOPPED -> onStoppedEvent()
            RecogniserEvent.UP -> onUpEvent()
            RecogniserEvent.DOWN -> onDownEvent()
            RecogniserEvent.TOWARDS -> onTowardsEvent()
            RecogniserEvent.AWAY -> onAwayEvent()
            RecogniserEvent.UNKNOWN -> onUnknownEvent()
            RecogniserEvent.TAP -> onTapEvent()
            RecogniserEvent.LONG_PRESS -> onLongPressEvent()
            RecogniserEvent.ROTATE_UP -> onRotateUp()
            RecogniserEvent.ROTATE_DOWN -> onRotateDown()

        }

    }

    private val batteryReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            if(intent.hasExtra("event")) {

                when(intent.getStringExtra("event")) {

                    BatteryEvent.CHARGER_CONNECTED.toString() -> {

                        if(intent.hasExtra("battery")) {
                            onChargerConnectedEvent(intent.getFloatExtra("battery", 0f))
                        }

                    }

                    BatteryEvent.BATTERY_CHANGED.toString() -> {

                        if(intent.hasExtra("battery")) {
                            onBatteryChangedEvent(intent.getFloatExtra("battery", 0f))
                        }

                    }

                    BatteryEvent.CHARGER_DISCONNECTED.toString() -> onChargerDisconnectedEvent()

                }

            }
        }
    }

    private val killHandler = Handler()
    private var lastChanged = System.currentTimeMillis()
    private var timeCheckInterval = 30 * 1000L //30 seconds
    private var killDelta = 5 * 60 * 1000L //5 minutes
    private lateinit var killRunnable: Runnable

    open fun setup() {
        contentCollection.setup()
        recogniser.addObserver(recogniserObserver)
        context.registerReceiver(batteryReceiver, IntentFilter("BATTERY_INFO").apply {})

        killRunnable = Runnable {
            if(System.currentTimeMillis() - lastChanged > killDelta) {
                Logger.log(LogType.ACTIVITY_TERMINATED, listOf(), context)
                presenter.view!!.finishActivity()
            } else {
                killHandler.postDelayed(killRunnable, timeCheckInterval)
            }
        }

        setStatingState()
    }

    open fun start() {
        recogniser.start()
    }

    open fun stop() {
        killHandler.removeCallbacks(killRunnable)
        context.unregisterReceiver(batteryReceiver)
        recogniser.stop()
        presenter.detachView()
        contentCollection.stop()
    }

    fun updateState(controllerState: ControllerState){
        synchronized(currentControllerState) {
            if (controllerState == currentControllerState)
                return //no change, so no need to notify of change

            previousControllerState = currentControllerState
            currentControllerState = controllerState

            Log.d("newState", "$currentControllerState")
        }
    }

    fun getCurrentState(): ControllerState {
        return currentControllerState
    }

    fun getPreviousState(): ControllerState {
        return previousControllerState
    }

    fun getRecogniser(): AbstractRecogniser {
        return recogniser
    }

    fun getPresenter(): Presenter {
        return presenter
    }

    fun getContentCollection(): AbstractContentCollection {
        return contentCollection
    }

    fun startKillThread(timeCheckInterval : Long, killDelta : Long) {

        this.timeCheckInterval = timeCheckInterval
        this.killDelta = killDelta
        this.lastChanged = System.currentTimeMillis()

        killRunnable.run()
    }

    fun updateKillThread(updateTimestamp: Long) {
        this.lastChanged = updateTimestamp
    }

    fun stopKillThread() {
        killHandler.removeCallbacks(killRunnable)
    }

    abstract fun setStatingState()
    abstract fun onStartedEvent()
    abstract fun onStoppedEvent()
    abstract fun onUpEvent()
    abstract fun onDownEvent()
    abstract fun onTowardsEvent()
    abstract fun onAwayEvent()
    abstract fun onUnknownEvent()
    abstract fun onTapEvent()
    abstract fun onLongPressEvent()
    abstract fun onChargerConnectedEvent(battery: Float)
    abstract fun onChargerDisconnectedEvent()
    abstract fun onBatteryChangedEvent(battery: Float)
    abstract fun onRotateUp()
    abstract fun onRotateDown()
}