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
import com.gvillani.rxsensors.exceptions.SensorNotFoundException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import uk.ac.ncl.openlab.ongoingness.utilities.CircularArray

/**
 * Created by Kyle Montague on 10/11/2018.
 */
open class RotationRecogniserOld(val context: Context) {
    private val tag = "RR"
    private var listener: Listener? = null
    private var disposables = arrayListOf<Disposable>()
    private var states = ObservableList<State>()

    enum class State {
        UNKNOWN,
        TOWARDS,
        UP,
        AWAY,
        DOWN,
        PICKED_UP,
    }

    private var enterState = State.UNKNOWN
    private var previousState = State.UNKNOWN
    private var lastChanged: Long = System.currentTimeMillis()

    private var timeoutHandler = Handler()
    private var timeoutRunnable: Runnable? = null

    private var timeoutDuration = 1000L * 30 * 1 //one minute timeout
    private val timeoutInterval = 5000L

    private var isPickUp = false
    private var onStandby = false

    init {
        timeoutRunnable = Runnable {

            if ((System.currentTimeMillis() - lastChanged) >= timeoutDuration) {
                Log.d(tag, "onstanby")
                isPickUp = false
                onStandby = true
                timeoutHandler.removeCallbacks(timeoutRunnable)
                listener?.onStandby()
            } else {
                timeoutHandler.postDelayed(timeoutRunnable, timeoutInterval) //check again in 5seconds
            }

        }
    }

    fun start(listener: Listener) {
        Log.d(tag, "start")
        Log.d(tag, "$listener")

        this.listener = listener
        lastChanged = System.currentTimeMillis()
        timeoutHandler.postDelayed(timeoutRunnable, timeoutInterval)

        try {

            disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_GAME)
                    .subscribeOn(Schedulers.computation())
                    .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                    .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { rxSensorEvent -> process(rxSensorEvent) })


            disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GAME_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_NORMAL)
                    .subscribeOn(Schedulers.computation())
                    .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { rxSensorEvent -> checkMotion(rxSensorEvent) })

            states = ObservableList()
            disposables.add(states.observable
                    .subscribeOn(Schedulers.io())
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { state -> onStateChange(state) })


        } catch (e: SensorNotFoundException) {
            Log.d("RotationRecogniserOld", "Sensor missing, running without sensors")
            throw e
        }
    }

    fun stop() {
        this.listener = null
        for (disposable in disposables) {
            disposable.dispose()
        }
    }


    private val standardGravity = SensorManager.STANDARD_GRAVITY
    private val thresholdGravity = standardGravity / 1.3

    private fun process(event: RxSensorEvent) {
        processState(event)
    }

    private var circularArray = CircularArray<RxSensorEvent>(30)
    private fun checkMotion(event: RxSensorEvent) {
        circularArray.add(event)
    }


    private fun processState(event: RxSensorEvent) {

        val y = event.values[1]
        val z = event.values[2]

        var currentState = previousState

        when {
            z >= thresholdGravity -> currentState = State.UP
            z <= -thresholdGravity -> currentState = State.DOWN
            y >= thresholdGravity -> currentState = State.TOWARDS
            y <= -thresholdGravity -> currentState = State.AWAY

            !isPickUp && y >= 1.0 -> currentState = State.PICKED_UP
        }

        if (currentState != State.UNKNOWN)
            states.add(currentState)
    }


    private fun onStateChange(currentState: State) {

        Log.d("ControllerState", "$currentState")

        if(currentState == State.PICKED_UP) {
            Log.d(tag, "OnPickUp")
            onPickUp()
        } else if (currentState == State.DOWN) {
            //entering
            enterState = previousState
            Log.d(tag, "prev: $previousState current: $enterState")
            //change image
            when (enterState) {
                State.TOWARDS -> goBack()
                State.AWAY -> goForward()
                else -> Log.d(tag,"Changed ControllerState to Unknown")
            }

        } else if (previousState == State.DOWN) {
            //exiting
            if (currentState == enterState) {
                //undo change
                when (enterState) {
                    State.TOWARDS -> goForward()
                    State.AWAY -> goBack()
                    else -> Log.d(tag,"Changed ControllerState to Unknown")
                }
            }
        }

        previousState = currentState
        lastChanged = System.currentTimeMillis()

        if(onStandby) {
            onStandby = false
            lastChanged = System.currentTimeMillis()
            timeoutHandler.postDelayed(timeoutRunnable, timeoutInterval)
            timeoutRunnable = Runnable {

                if ((System.currentTimeMillis() - lastChanged) >= timeoutDuration) {
                    Log.d(tag, "onstanby")
                    isPickUp = false
                    onStandby = true
                    listener?.onStandby()
                } else {
                    timeoutHandler.postDelayed(timeoutRunnable, timeoutInterval) //check again in 5seconds
                }

            }
        }

    }

    private fun goForward() {
        Log.d(tag, "goforward")
        listener?.onRotateUp()
    }

    private fun goBack() {
        Log.d(tag, "goback")
        listener?.onRotateDown()
    }

    private fun onPickUp() {
        Log.d(tag, "onpickup")
        isPickUp = true
        listener?.onPickUp()

    }



    class ObservableList<T> {

        val list: MutableList<T>
        private val onAdd: PublishSubject<T>
        val observable: Observable<T>
            get() = onAdd

        init {
            this.list = ArrayList()
            this.onAdd = PublishSubject.create()
        }

        fun add(value: T) {
            list.add(value)
            onAdd.onNext(value)
        }
    }


    interface Listener {

        fun onRotateUp()

        fun onRotateDown()

        fun onRotateLeft()

        fun onRotateRight()

        fun onStandby()

        fun onPickUp()

    }
}