package uk.ac.ncl.openlab.ongoingness.recognisers

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
import kotlinx.coroutines.*
import uk.ac.ncl.openlab.ongoingness.utilities.CircularArray
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class RevealRecogniserOld(private val context: Context): Observable() {

    enum class State{
        STANDBY,
        AWAKE,
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
    }


    private val bufferSize: Int = 5

    var lightLevels: CircularArray<Float> = CircularArray(bufferSize)

    private var currentState: State = State.OFF

    private var previousState: State = State.UNKNOWN
    private var disposables: ArrayList<Disposable> = arrayListOf()
    private var currentOrientation = Orientation.UNKNOWN
    private var previousOrientation = Orientation.UNKNOWN

    private var currentLight = LightLevel.UNKNOWN
    private var previousLight = LightLevel.UNKNOWN


    private var lightDelta = LightChange.UNKNOWN

    private var revealHandler = Handler()
    private var revealRunnable: Runnable? = null

    private var revealDuration = 1000L * 5 // three seconds
    private var shortRevealDuration = 500L // 500 milliseconds
    private val interval = 100L //100 milliseconds

    private var isActive = false

    private var coverStart:Long = System.currentTimeMillis()

    private var lastEventChange:Long = System.currentTimeMillis()

    private var shortRevealHappened: Boolean = false
    private var longRevealHappened: Boolean = false

    private var previousAverage = 0.0
    private var previousVariance: Int = 0

    private var stateHasChanged: Boolean = true

    init {
        revealRunnable = Runnable {
            //check if the light level has remained constant
            revealHandler.postDelayed(revealRunnable,interval)
            checkForReveal()
            checkForShortReveal()
        }
    }

    private var shortTime = System.currentTimeMillis()
    private fun checkForShortReveal(){
        if(exitShortReveal) {
            if (previousVariance >= 0) {
                Log.d(TAG, "short Reveal allowed")
                exitShortReveal = false
            } else {
                Log.d(TAG, "Going for long reveal??????")
            }
        }
    }

    fun start(){
        isActive = true
        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_UI)
                .subscribeOn(Schedulers.computation())
                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processGravity(rxSensorEvent) })


        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_FASTEST)
                .subscribeOn(Schedulers.computation())
                .debounce(100,TimeUnit.MILLISECONDS)
                //.distinctUntilChanged(RxSensorFilter.uniqueEventValues())
              //  .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.1f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processLight(rxSensorEvent) })
        setChanged()
        notifyObservers(Events.STARTED)

        revealRunnable!!.run()
    }


    fun stop() {
        isActive = false
        for (disposable in disposables) {
            disposable.dispose()
        }
        setChanged()
        notifyObservers(Events.STOPPED)
    }

    fun setWakeDuration(milliseconds:Long){
        if(milliseconds >= 1000){
            revealDuration = milliseconds
        }
    }

    fun isActive():Boolean{
        return isActive
    }

    fun getState(): State {
        return currentState
    }

    private fun enabled():Boolean{
        return arrayListOf(State.ACTIVE,State.STANDBY,State.COVERED,State.AWAKE).contains(currentState)
    }
    private fun processLight(event: RxSensorEvent) {

        if(!enabled())
            return

        val current = event.values[0]
        var previous = 0F
        if(lightLevels.size > 0) {
            previous = lightLevels.last()
        }

        lightLevels.add(current) //push value to  circular array

        if(lightLevels.size != bufferSize)
            return

        var average = 0.0;
        for(sample in lightLevels) {
            average += sample
        }
        average /= lightLevels.size

        lateinit var ordered:List<Float>
        synchronized(lightLevels) {
            ordered = lightLevels.sortedBy { it }
        }

        val min = ordered.first()
        val max = ordered.last()
        val median = ordered[(ordered.size/2)-1]

        val delta = previous - current

        val deltaRel = delta / (max-min)

        val maxInt = max.roundToInt()

        var variance: Int = average.toInt()-previousAverage.toInt()

        Log.d(TAG,"C:$average   -   P:$previousAverage    =    $variance;   $current")

        if(currentLight != LightLevel.HIGH && (variance > 5)){
            Log.d(TAG,"$currentLight -> LightLevel.HIGH")
            updateLightLevel(LightLevel.HIGH)
        }else if(currentLight != LightLevel.LOW && (variance < -5)){
            Log.d(TAG,"$currentLight -> LightLevel.LOW")
            updateLightLevel(LightLevel.LOW)
        }


        previousAverage = average
        previousVariance = variance

        /*

        Log.d(TAG,"Lux:$current Max:$max Median:$median Min:$min Range:${max - min} Delta:$delta Delta-Rel:$deltaRel")

        if(currentLight != LightLevel.HIGH && delta >= 10/*LIGHT_DELTA_THRESHOLD*/ ){
            Log.d(TAG,"$currentLight -> LightLevel.HIGH")
            updateLightLevel(LightLevel.HIGH)
        }else if(currentLight != LightLevel.LOW && delta <= 0-10/*LIGHT_DELTA_THRESHOLD*/){
            Log.d(TAG,"$currentLight -> LightLevel.LOW")
            updateLightLevel(LightLevel.LOW)
        }
        */

    }


    private var exitShortReveal: Boolean = false

    private fun notifyEvent(event: Events) {
        lastEventChange = System.currentTimeMillis()
        setChanged()
        notifyObservers(event)
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

        if (currentState == State.OFF && currentOrientation == Orientation.TOWARDS) {
            //entered standby orientation
            updateState(State.STANDBY)
            notifyEvent(Events.STARTED)
        } else if(currentState != State.OFF && (currentOrientation == Orientation.AWAY || currentOrientation == Orientation.DOWN)) {
            // entered off orientation
            updateState(State.OFF)
            notifyEvent(Events.STOPPED)
        }
    }


    private fun checkForReveal(){
        if(currentState == State.COVERED && (!shortRevealHappened || !longRevealHappened)) {
            val deltaCover = System.currentTimeMillis() - coverStart
            if (deltaCover >= revealDuration && debounceEvent() && !longRevealHappened) { //alert the observers that a long reveal has happened
                longRevealHappened = true
                exitShortReveal = false
                if(previousState == State.STANDBY) {
                    notifyEvent(Events.AWAKE)
                    updateState(State.ACTIVE)
                    shortRevealHappened = false
                } else if (previousState == State.ACTIVE) {
                    notifyEvent(Events.SLEEP)
                    updateState(State.STANDBY)
                    shortRevealHappened = false
                }
            } else if (deltaCover >= shortRevealDuration && debounceEvent() && !shortRevealHappened && previousState == State.ACTIVE) { //alert the observers that a short reveal has happened
                shortRevealHappened = true
                notifyEvent(Events.SHORT_REVEAL)
            }
        }
    }

    private fun updateState(state: State){
        synchronized(currentState) {
            if (state == currentState)
                return //no change, so no need to notify of change

            previousState = currentState
            currentState = state
            stateHasChanged = true
        }
    }

    private fun updateOrientation(orientation: Orientation){
        previousOrientation = currentOrientation
        currentOrientation = orientation
    }


    //FIXME - Going to have some issues with the sensor values, as it is currently set to notify when the value changes. Not using a regular sampling rate.
    private fun updateLightLevel(lightLevel: LightLevel) {
        synchronized(currentLight) {
            previousLight = currentLight
            currentLight = lightLevel

            if ((previousLight == LightLevel.UNKNOWN && currentLight != LightLevel.UNKNOWN) || previousLight == currentLight) {
                lightDelta = LightChange.NONE
            } else if (previousLight.ordinal > currentLight.ordinal && currentLight != LightLevel.UNKNOWN) {
                lightDelta = LightChange.DECREASE
            } else if (previousLight.ordinal < currentLight.ordinal && currentLight != LightLevel.UNKNOWN) {
                lightDelta = LightChange.INCREASE
            }

            if (currentState != State.COVERED && currentLight == LightLevel.LOW && lightDelta == LightChange.DECREASE) { //entered a covered state
                coverStart = System.currentTimeMillis()
                updateState(State.COVERED)
                notifyEvent(Events.COVERED)

                shortRevealHappened = false
                longRevealHappened = false

                
            } else if (currentState == State.COVERED && currentLight.ordinal > LightLevel.LOW.ordinal && lightDelta == LightChange.INCREASE) { //exited a covered state

                if(previousState == State.STANDBY) {
                    shortRevealHappened = false
                    updateState(State.STANDBY)
                } else if (previousState == State.ACTIVE) {
                    shortRevealHappened = false
                    updateState(State.ACTIVE)
                }

            }
        }
    }

    private fun debounceEvent():Boolean{
        return (System.currentTimeMillis() - lastEventChange) >= 100
    }


    companion object {
        private const val standardGravity = SensorManager.STANDARD_GRAVITY
        const val thresholdGravity = standardGravity / 1.3
        const val TAG = "RevealRecogniserOld"
    }

}