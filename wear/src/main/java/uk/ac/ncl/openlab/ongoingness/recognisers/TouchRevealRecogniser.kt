package uk.ac.ncl.openlab.ongoingness.recognisers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import com.gvillani.rxsensors.RxSensorTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import java.util.*
import kotlin.math.floor

class TouchRevealRecogniser(private val context: Context) : Observable(), GestureDetector.OnGestureListener {

    override fun onLongPress(e: MotionEvent?) {

        Log.d(TAG, "Long Press")

        when (currentState) {
            State.STANDBY -> {
                updateState(State.ACTIVE)
                notifyEvent(Events.AWAKE)
            }

            State.ACTIVE -> {
                updateState(State.STANDBY)
                notifyEvent(Events.SLEEP)
            }

            else -> {}
        }

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {

        Log.d(TAG, "Single Press")

        when (currentState) {
            State.ACTIVE -> {
                notifyEvent(Events.NEXT)
                return true
            }

            else -> {}
        }

        return false
    }


    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {}

    enum class State{
        STANDBY,
        ACTIVE,
        OFF,
        UNKNOWN,
    }

    enum class Orientation{
        UP,
        DOWN,
        TOWARDS,
        AWAY,
        UNKNOWN
    }

    enum class Events{
        STARTED,
        AWAKE,
        SLEEP,
        STOPPED,
        NEXT
    }

    private var currentState: State = State.OFF
    private var previousState: State = State.UNKNOWN

    private var disposables: ArrayList<Disposable> = arrayListOf()
    private var currentOrientation = Orientation.UNKNOWN
    private var previousOrientation = Orientation.UNKNOWN

    fun start(){
        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_UI)
                .subscribeOn(Schedulers.computation())
                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processGravity(rxSensorEvent) })

        setChanged()
        notifyObservers(Events.STARTED)
    }


    fun stop() {
        for (disposable in disposables) {
            disposable.dispose()
        }
        notifyEvent(Events.STOPPED)
    }

    fun updateState(state: State){
        synchronized(currentState) {
            if (state == currentState)
                return //no change, so no need to notify of change

            previousState = currentState
            currentState = state

            Log.d(TAG, "State Update: $currentState")
        }
    }

    private fun updateOrientation(orientation: Orientation){
        synchronized(currentOrientation) {
            if (orientation == currentOrientation)
                return

            previousOrientation = currentOrientation
            currentOrientation = orientation

            Log.d(TAG, "Orientation Update: $currentOrientation")
        }
    }

    fun notifyEvent(event: Events) {

        Log.d("ss", "$event")

        setChanged()
        notifyObservers(event)
    }

    private fun processGravity(event: RxSensorEvent) {
        val y = floor(event.values[1]).toInt()
        val z = floor(event.values[2]).toInt()

        Log.d("acc", "Y:$y Z:$z")

        when(FLAVOR) {
            "locket_touch" -> {
                if (y >= 2 && z > -9 && z < 9)
                    updateOrientation(Orientation.TOWARDS)
                else if (y > -2  && y < 2 && z > -9 && z < 9)
                    updateOrientation(Orientation.AWAY)
            }

            "locket_touch_inverted" -> {
                if (y <= -2 && z > -9 && z < 9)
                    updateOrientation(Orientation.TOWARDS)
                else if (y > 5 && z <= 8 )//y < 2 && y > -2 && z > -9 && z < 9)
                    updateOrientation(Orientation.AWAY)
            }
        }



        gravityStateChecker()
    }

    private fun gravityStateChecker() {

        if (currentState == State.OFF && currentOrientation == Orientation.TOWARDS) {
            updateState(State.STANDBY)
            notifyEvent(Events.STARTED)
        } else if (currentState != State.OFF && currentOrientation == Orientation.AWAY) {
            updateState(State.OFF)
            stop()
        }

    }

    private

    companion object {
        private const val standardGravity = SensorManager.STANDARD_GRAVITY
        const val thresholdGravity = standardGravity / 1.3
        const val TAG = "TouchRevealRecogniser"
    }

}