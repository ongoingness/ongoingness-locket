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

/**
 * Event-driven state machine in charge of interconnecting and commanding the model, view, event-data layers of the app.
 *
 * @param context context of the application.
 * @param recogniser recogniser to be used by this controller.
 * @param presenter presenter to be used by this controller.
 * @param contentCollection content to be used by this controller.
 * @author Luis Carvalho
 */
abstract class AbstractController(
        var context: Context,
        private val recogniser: AbstractRecogniser,
        private val presenter: Presenter,
        private val contentCollection: AbstractContentCollection) {

    /**
     * Current state of this controller.
     */
    private var currentControllerState: ControllerState = ControllerState.UNKNOWN

    /**
     * Previous state of this controller.
     */
    private var previousControllerState: ControllerState = ControllerState.UNKNOWN

    /**
     * Trigger to true if any order to stop was issued.
     * Aims to prevent the stopping action to be triggered multiple times.
     */
    private var stopped = false

    /**
     * Connects events received from recognisers to the function to be triggered.
     */
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

            RecogniserEvent.ROTATE_LEFT -> onRotateLeft()
            RecogniserEvent.ROTATE_RIGHT -> onRotateRight()
            RecogniserEvent.AWAY_LEFT -> onAwayLeft()
            RecogniserEvent.AWAY_RIGHT -> onAwayRight()
            RecogniserEvent.AWAY_TOWARDS -> onAwayTowards()

        }

    }

    /**
     * Connects events received from the battery to the function to be triggered.
     */
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

    /**
     * Handler to the thread in charge of terminating the app, if inactive for a certain period.
     */
    private val killHandler = Handler()

    /**
     * Timestamp of the last activity.
     */
    private var lastChanged = System.currentTimeMillis()

    /**
     * Interval to check inactivity.
     */
    private var timeCheckInterval = 30 * 1000L //30 seconds

    /**
     * Max inactivity time.
     */
    private var killDelta = 5 * 60 * 1000L //5 minutes

    /**
     * Thread in charge of terminating the app, if inactive for a certain period.
     */
    private lateinit var killRunnable: Runnable

    /**
     * Sets up and interconnects collections, recognisers and termination threads.
     */
    open fun setup() {
        contentCollection.setup()
        recogniser.addObserver(recogniserObserver)
        context.registerReceiver(batteryReceiver, IntentFilter("BATTERY_INFO").apply {})
        killRunnable = Runnable {
            if(System.currentTimeMillis() - lastChanged > killDelta) {
                presenter.view!!.finishActivity()
            } else {
                killHandler.postDelayed(killRunnable, timeCheckInterval)
            }
        }

        setStartingState()
    }

    /**
     * Sets the recognisers to start looking for events.
     */
    open fun start() {
        recogniser.start()
    }

    /**
     * Stops and detaches all connections between this controller components.
     */
    open fun stop() {
        if(!stopped) {
            contentCollection.stop()
            killHandler.removeCallbacks(killRunnable)
            context.unregisterReceiver(batteryReceiver)
            recogniser.stop()
            presenter.detachView()
            Logger.log(LogType.ACTIVITY_TERMINATED, listOf(), context)
            Logger.deleteLogSessionToken()
            stopped = true
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
                return //no change, so no need to notify of change

            previousControllerState = currentControllerState
            currentControllerState = controllerState

            Log.d("newState", "$currentControllerState")
        }
    }

    /**
     * Gets the current state of this controller.
     * @return the current state.
     */
    fun getCurrentState(): ControllerState {
        return currentControllerState
    }

    /**
     * Gets the previous state of this controller.
     * @return the previous state.
     */
    fun getPreviousState(): ControllerState {
        return previousControllerState
    }

    /**
     * Gets the recogniser associated to this controller.
     * @return the recogniser.
     */
    fun getRecogniser(): AbstractRecogniser {
        return recogniser
    }

    /**
     * Gets the presenter associated to this controller.
     * @return the present.
     */
    fun getPresenter(): Presenter {
        return presenter
    }

    /**
     * Gets the content collection associated to this controller.
     * @return the content collection.
     */
    fun getContentCollection(): AbstractContentCollection {
        return contentCollection
    }

    /**
     * Starts the termination thread.
     * @param timeCheckInterval check inactivity interval
     * @param killDelta max inactivity time
     */
    fun startKillThread(timeCheckInterval : Long, killDelta : Long) {

        this.timeCheckInterval = timeCheckInterval
        this.killDelta = killDelta
        this.lastChanged = System.currentTimeMillis()

        killRunnable.run()
    }

    /**
     * Update the start of the inactivity counter.
     * @param updateTimestamp timestamp of the activity performed.
     */
    fun updateKillThread(updateTimestamp: Long) {
        this.lastChanged = updateTimestamp
    }

    /**
     * Terminate the termination thread.
     */
    fun stopKillThread() {
        killHandler.removeCallbacks(killRunnable)
    }

    /**
     *
     */
    abstract fun setStartingState()

    /**
     * Controller behaviour when receiving a start event.
     */
    abstract fun onStartedEvent()

    /**
     * Controller behaviour when receiving a stop event.
     */
    abstract fun onStoppedEvent()

    /**
     * Controller behaviour when receiving a up event.
     */
    abstract fun onUpEvent()

    /**
     * Controller behaviour when receiving a down event.
     */
    abstract fun onDownEvent()

    /**
     * Controller behaviour when receiving a towards event.
     */
    abstract fun onTowardsEvent()

    /**
     * Controller behaviour when receiving a away event.
     */
    abstract fun onAwayEvent()

    /**
     * Controller behaviour when receiving a unknown event.
     */
    abstract fun onUnknownEvent()

    /**
     * Controller behaviour when receiving a tap event.
     */
    abstract fun onTapEvent()

    /**
     * Controller behaviour when receiving a long press event.
     */
    abstract fun onLongPressEvent()

    /**
     * Controller behaviour when receiving a changer connected event.
     * @param battery amount of battery.
     */
    abstract fun onChargerConnectedEvent(battery: Float)

    /**
     * Controller behaviour when receiving a charger disconnected event.
     */
    abstract fun onChargerDisconnectedEvent()

    /**
     * Controller behaviour when receiving a battery changed event.
     * @param battery amount of battery.
     */
    abstract fun onBatteryChangedEvent(battery: Float)

    /**
     * Controller behaviour when receiving a rotate up event.
     */
    abstract fun onRotateUp()

    /**
     * Controller behaviour when receiving a rotate down event.
     */
    abstract fun onRotateDown()

    /**
     * Controller behaviour when receiving a rotate left event.
     */
    abstract fun onRotateLeft()

    /**
     * Controller behaviour when receiving a rotate right event.
     */
    abstract fun onRotateRight()

    /**
     * Controller behaviour when receiving a away left event.
     */
    abstract fun onAwayLeft()

    /**
     * Controller behaviour when receiving a away right event.
     */
    abstract fun onAwayRight()

    /**
     * Controller behaviour when receiving a away towards event.
     */
    abstract fun onAwayTowards()

}