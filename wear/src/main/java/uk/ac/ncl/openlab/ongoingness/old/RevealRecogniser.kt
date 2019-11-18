package uk.ac.ncl.openlab.ongoingness.old

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import com.gvillani.rxsensors.RxSensorTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class RevealRecogniser(private val context: Context) : Observable() {

    enum class State{
        STANDBY,
        AWAKE_COVER,
        AWAKE,
        PRE_COVERED,
        COVERED,
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

    enum class LightLevel{
        UNKNOWN,
        LOW,
        MEDIUM,
        HIGH
    }

    enum class LightChange{
        UNKNOWN,
        NONE,
        INCREASE,
        DECREASE
    }

    enum class Events{
        STARTED,
        COVERED,
        AWAKE,
        SHORT_REVEAL,
        SLEEP,
        STOPPED,
        HIDE,
        SHOW,
    }

    private var currentState: State = State.OFF
    private var previousState: State = State.UNKNOWN
    private var previousPreviousState: State = State.UNKNOWN

    private var disposables: ArrayList<Disposable> = arrayListOf()
    private var currentOrientation = Orientation.UNKNOWN
    private var previousOrientation = Orientation.UNKNOWN
    private var lastEventChange:Long = System.currentTimeMillis()

    private var buffer: Deque<Pair<Long, Float>> = ArrayDeque<Pair<Long, Float>>()
    private var bufferSize: Int = 10

    private var max: Float = 0F
    private var min: Float = 100F

    private var lastSampleTime: Long = System.currentTimeMillis()
    private var lastSample: RxSensorEvent? = null

    private var lastCoverSampleTime: Long = System.currentTimeMillis()

    private  var currentLightLevel: LightLevel = LightLevel.UNKNOWN

    private var shortRevealHappened: Boolean = false
    private var wokeUpAndShown: Boolean = false

    private var revealHandler = Handler()
    private var revealRunnable: Runnable? = null
    private val interval = 100L //100 milliseconds

    init {
        revealRunnable = kotlinx.coroutines.Runnable {
            //check if the light level has remained constant
            revealHandler.postDelayed(revealRunnable, interval)
            if(currentState == State.COVERED && lastSample != null && System.currentTimeMillis() - lastSampleTime > 1000) {
                lastSample!!.timestamp = System.currentTimeMillis() * 1000000
                Log.d("THREAD", "ADDED light")
                processLight(lastSample!!)
            }
        }
    }

    fun start(){
        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_UI)
                .subscribeOn(Schedulers.computation())
                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processGravity(rxSensorEvent) })


        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_FASTEST)
                .subscribeOn(Schedulers.computation())
                .debounce(0, TimeUnit.MILLISECONDS)
                //.distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                //.distinctUntilChanged(RxSensorFilter.minAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
                //  .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.1f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processLight(rxSensorEvent) })



        revealRunnable!!.run()

        setChanged()
        notifyObservers(Events.STARTED)
    }

    fun stop() {
        for (disposable in disposables) {
            disposable.dispose()
        }
        setChanged()
        notifyObservers(Events.STOPPED)
    }

    private fun enabled():Boolean{
        return arrayListOf(State.ACTIVE, State.STANDBY, State.COVERED, State.AWAKE, State.PRE_COVERED, State.AWAKE_COVER).contains(currentState)
    }

    private fun updateState(state: State){
        synchronized(currentState) {
            if (state == currentState)
                return //no change, so no need to notify of change

            previousPreviousState = previousState
            previousState = currentState
            currentState = state
        }
    }

    private fun updateOrientation(orientation: Orientation){
        previousOrientation = currentOrientation
        currentOrientation = orientation
    }

    private fun notifyEvent(event: Events) {
        lastEventChange = System.currentTimeMillis()
        setChanged()
        notifyObservers(event)
    }

    private fun processLight(event: RxSensorEvent) {
        Log.d("Data", "${event.values[0]}")

        if(enabled()) {

            if (System.currentTimeMillis() - lastSampleTime > 500) {

                lastSampleTime = System.currentTimeMillis()
                lastSample = event

                if (buffer.size == bufferSize)
                    buffer.removeFirst()
                buffer.addLast(Pair(event.timestamp, event.values[0]))

                lightStateChecker(event)

                if (event.values[0] > max) {
                    max = event.values[0]
                } else if (event.values[0] < min) {
                    min = event.values[0]
                    Log.d("DOWNDOWN", "$min")
                }

            }
            //Log.d("TESTING", "Last: ${event.values[0]}      Max: $max       Min: $min")

        }
    }


    private fun lightStateChecker(event: RxSensorEvent) {

        var lightChange: LightChange = getLightChange((event.values[0]))
        Log.d("Pre", "$lightChange" )
        /*
        if (!checkLightChange(3, lightChange))
            lightChange = LightChange.NONE
        */

       var lightChanged = false

        if(currentLightLevel != LightLevel.HIGH && lightChange == LightChange.INCREASE) {
            currentLightLevel = LightLevel.HIGH
            lightChanged = true
        } else if(currentLightLevel != LightLevel.LOW && lightChange == LightChange.DECREASE) {
            currentLightLevel = LightLevel.LOW
            lightChanged = true
        } else if (currentLightLevel == LightLevel.HIGH && lightChange == LightChange.INCREASE) {
            lightChange = LightChange.NONE
        } else if (currentLightLevel == LightLevel.LOW && lightChange == LightChange.DECREASE) {
            lightChange = LightChange.NONE
        }


        /*

        if(previousLightChange == lightChange)
            lightChange = LightChange.NONE
        else
            previousLightChange = lightChange
        */


        Log.d("Testing",  "$currentLightLevel $lightChange $currentState ${event.values[0]} $lightChanged")


        when(currentState) {

            State.STANDBY -> {

                Log.d("Standby", "enter" )


                if(currentLightLevel == LightLevel.LOW && lightChanged) {
                    Log.d("Standby", "Light decrease" )
                    lastCoverSampleTime = event.timestamp
                    updateState(State.AWAKE_COVER)
                }

            }

            State.PRE_COVERED -> {
                Log.d("Precovered", "enter" )
                if(currentLightLevel == LightLevel.LOW && lightChanged && wokeUpAndShown) {
                    Log.d("PRE_COVERED", "Light decrease" )
                    lastCoverSampleTime = event.timestamp
                    updateState(State.COVERED)
                }

            }

            State.AWAKE_COVER -> {

                if(currentLightLevel == LightLevel.HIGH && lightChanged) {
                    updateState(State.STANDBY)
                } else if (currentLightLevel == LightLevel.LOW && !lightChanged && event.timestamp - lastCoverSampleTime > 5000000000) {
                    notifyEvent(Events.AWAKE)
                    updateState(State.ACTIVE)
                }

            }

            State.COVERED -> {

                if(previousPreviousState == State.ACTIVE){

                    if(currentLightLevel == LightLevel.HIGH && lightChanged) {
                        updateState(State.ACTIVE)
                        shortRevealHappened = false
                    } else if (currentLightLevel== LightLevel.LOW && !lightChanged) {
                        if (event.timestamp - lastCoverSampleTime > 5000000000 && shortRevealHappened) {
                            notifyEvent(Events.SLEEP)
                            updateState(State.STANDBY)
                            shortRevealHappened = false
                            wokeUpAndShown = false
                        } else if (event.timestamp - lastCoverSampleTime > 100000000 && !shortRevealHappened) {
                            notifyEvent(Events.SHORT_REVEAL)
                            shortRevealHappened = true
                        }
                    }
                }
            }

            State.ACTIVE -> {

                if(currentLightLevel == LightLevel.LOW && lightChanged) {
                    lastCoverSampleTime = event.timestamp
                    updateState(State.COVERED)
                }

            }

            else -> {Log.d(TAG,"UNKNOWN  STATE")}
        }
    }

    private fun getLightChange(value: Float): LightChange {

        var minWindowTop: Float = min + min / 2
        val maxWindowBottom: Float = max - max / 2


        if(minWindowTop < 20) {
            minWindowTop = 2 * min
        }

        Log.d("HUM", "$minWindowTop  $maxWindowBottom")

        if(value < minWindowTop) {
            return LightChange.DECREASE
        } else if(value > maxWindowBottom) {
            return LightChange.INCREASE
        }



        return LightChange.NONE
    }

    private fun checkLightChange(n: Int, changeType: LightChange): Boolean {

        if(buffer.size >= n) {
            for(i in 1..n) {
                if(getLightChange(buffer.toMutableList()[buffer.size-i].second) != changeType)
                    return false
            }
        } else {
            buffer.forEach { element ->
                if(getLightChange(element.second) != changeType)
                    return false
            }
        }
        return true
    }

    private fun processGravity(event: RxSensorEvent) {
        val y = event.values[1]
        val z = event.values[2]

        when {
            z >= thresholdGravity -> updateOrientation(Orientation.UP)
            z <= -thresholdGravity -> updateOrientation(Orientation.DOWN)
            y >= thresholdGravity -> updateOrientation(Orientation.TOWARDS)
            y <= -thresholdGravity -> updateOrientation(Orientation.AWAY)
        }

        gravityStateChecker()
    }


    private fun gravityStateChecker() {

        if (currentState == State.OFF && currentOrientation == Orientation.TOWARDS) {
            //entered standby orientation
            updateState(State.STANDBY)
            notifyEvent(Events.STARTED)
        } else if(currentState == State.ACTIVE && currentOrientation == Orientation.UP) {
            updateState(State.PRE_COVERED)
            notifyEvent(Events.HIDE)
        } else if(currentState == State.PRE_COVERED && currentOrientation == Orientation.TOWARDS) {
            updateState(State.ACTIVE)
            notifyEvent(Events.SHOW)
        } else if(currentState == State.ACTIVE && currentOrientation == Orientation.TOWARDS) {
            if(!wokeUpAndShown)
                wokeUpAndShown = true
        } else if(currentState != State.OFF && (currentOrientation == Orientation.AWAY || currentOrientation == Orientation.DOWN)) {
            // entered off orientation
            updateState(State.OFF)
            notifyEvent(Events.STOPPED)
        } else if(currentState == State.COVERED && currentOrientation == Orientation.TOWARDS) {
            shortRevealHappened = false
            updateState(State.ACTIVE)
            notifyEvent(Events.SHOW)
        }

    }

    companion object {
        private const val standardGravity = SensorManager.STANDARD_GRAVITY
        const val thresholdGravity = standardGravity / 1.3
        const val TAG = "RevealRecogniserOld"
    }

}