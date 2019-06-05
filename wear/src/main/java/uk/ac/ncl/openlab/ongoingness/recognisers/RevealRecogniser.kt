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
import uk.ac.ncl.openlab.ongoingness.utilities.CircularArray
import java.util.*
import java.util.concurrent.TimeUnit

class RevealRecogniser(private val context: Context): Observable() {

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
        STOPPED,
        COVERED,
        SHORT_REVEAL,
        LONG_REVEAL,
        STATE_CHANGED
    }

    var lightLevels: CircularArray<Float> = CircularArray(10)

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

    private var revealDuration = 1000L * 3 // three seconds
    private var shortRevealDuration = 500L // 500 milliseconds
    private val interval = 100L //100 milliseconds

    private var isActive = false

    private var coverStart:Long = System.currentTimeMillis()

    private var lastEventChange:Long = System.currentTimeMillis()

    init {
        revealRunnable = Runnable {
            Log.d(TAG,"checking for sleep / wake action")
            //check if the light level has remained constant
            revealHandler.postDelayed(revealRunnable,interval)
            checkForReveal()
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


        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_NORMAL)
                .subscribeOn(Schedulers.computation())
                .debounce(50,TimeUnit.MILLISECONDS)
                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processLight(rxSensorEvent) })
        setChanged()
        notifyObservers(Events.STARTED)
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

        if(lightLevels.size <= 2)
            return


        lateinit var ordered:List<Float>
        synchronized(lightLevels) {
            ordered = lightLevels.sortedBy { it }
        }

        val min = ordered.first()
        val max = ordered.last()
        val median = ordered[(ordered.size/2)-1]

        val delta = previous - current

        val deltaRel = delta / (max-min)

        Log.d(TAG,"Lux:$current Max:$max Median:$median Min:$min Range:${max - min} Delta:$delta Delta-Rel:$deltaRel")

//        Log.d(TAG,"Lux:$current")


        if(currentLight != LightLevel.HIGH && delta >= LIGHT_DELTA_THRESHOLD ){
            updateLightLevel(LightLevel.HIGH)
        }else if(currentLight != LightLevel.LOW && delta <= 0-LIGHT_DELTA_THRESHOLD){
            updateLightLevel(LightLevel.LOW)
        }

    }




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

        if (currentState == State.OFF && currentOrientation == Orientation.UP) {
            //entered standby orientation
            updateState(State.STANDBY)
        }else if (currentState == State.ACTIVE)
        {
            if(currentOrientation == Orientation.UP){
                //still active
            }else{
                //start timer for sleep mode as the device is in an other position???
            }

        }
    }


    private fun checkForReveal(){
    }

    private fun updateState(state: State){
        synchronized(currentState) {
            if (state == currentState)
                return //no change, so no need to notify of change

            previousState = currentState
            currentState = state
            setChanged()
            notifyObservers(currentState)
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

            if ((previousLight == LightLevel.UNKNOWN && currentLight != previousLight) || previousLight == currentLight) {
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
            } else if (currentState == State.COVERED && currentLight.ordinal > LightLevel.LOW.ordinal && lightDelta == LightChange.INCREASE) { //exited a covered state
                updateState(State.ACTIVE)

                val deltaCover = System.currentTimeMillis() - coverStart
                if (deltaCover >= revealDuration && debounceEvent()) { //alert the observers that a long reveal has happened
                    notifyEvent(Events.LONG_REVEAL)
                } else if (deltaCover >= shortRevealDuration && currentState == State.ACTIVE && debounceEvent()) { //alert the observers that a short reveal has happened
                    notifyEvent(Events.SHORT_REVEAL)
                }


            }
        }
    }

    fun debounceEvent():Boolean{
        return (System.currentTimeMillis() - lastEventChange) >= 100
    }


    companion object {
        private const val standardGravity = SensorManager.STANDARD_GRAVITY
        const val thresholdGravity = standardGravity / 1.3
        const val LIGHT_DELTA_THRESHOLD = 0.35F
        const val TAG = "RevealRecogniser"
    }
}