package uk.ac.ncl.openlab.ongoingness


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

/**
 * Created by Kyle Montague on 10/11/2018.
 */
open class RotationRecogniser(val context: Context) {
    private val TAG = "RR"
    var listener: RotationRecogniser.Listener? = null
    var disposables: ArrayList<Disposable> = arrayListOf()
    private var states = ObservableList<State>()

    enum class State {
        UNKNOWN,
        TOWARDS,
        UP,
        AWAY,
        DOWN
    }

    var enterState = State.UNKNOWN
    var previousState = State.UNKNOWN
    var lastChanged: Long = System.currentTimeMillis()

    var timeoutHandler = Handler()
    var timeoutRunnable: Runnable? = null

    private var timeoutDuration = 1000L * 30 * 1 //one minute timeout
    private val timeoutInterval = 5000L

    init {
        timeoutRunnable = Runnable {

            if ((System.currentTimeMillis() - lastChanged) >= timeoutDuration) {
                Log.d(TAG, "onstanby")
                listener?.onStandby()
            } else {
                timeoutHandler.postDelayed(timeoutRunnable, timeoutInterval) //check again in 5seconds

            }
        }
    }

    fun start(listener: Listener) {
        this.listener = listener
        lastChanged = System.currentTimeMillis()
        timeoutHandler.postDelayed(timeoutRunnable, timeoutInterval)

        try {
            Log.d("RotationRecogniser", "Sensor missing, running without sensors")
        } catch (e: SensorNotFoundException) {
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
        }
    }

    fun stop() {
        this.listener = null
        for (disposable in disposables) {
            disposable.dispose()
        }
    }


    val standardGravity = SensorManager.STANDARD_GRAVITY
    val thresholdGraqvity = standardGravity / 1.3

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
            z >= thresholdGraqvity -> currentState = State.UP
            z <= -thresholdGraqvity -> currentState = State.DOWN
            y >= thresholdGraqvity -> currentState = State.TOWARDS
            y <= -thresholdGraqvity -> currentState = State.AWAY
        }

        if (currentState != State.UNKNOWN)
            states.add(currentState)
    }


    private fun onStateChange(currentState: RotationRecogniser.State) {

        if (currentState == State.DOWN) {
            //entering
            enterState = previousState
            Log.d(TAG, "prev: $previousState current: $enterState")
            //change image
            when (enterState) {
                State.TOWARDS -> goBack()
                State.AWAY -> goForward()
            }

        } else if (previousState == State.DOWN) {
            //exiting
            if (currentState == enterState) {
                //undo change
                when (enterState) {
                    State.TOWARDS -> goForward()
                    State.AWAY -> goBack()
                }
            }
        }

        previousState = currentState
        lastChanged = System.currentTimeMillis()
    }

    private fun goForward() {
        Log.d(TAG, "goforward")
        listener?.onRotateUp()
    }

    private fun goBack() {
        Log.d(TAG, "goback")
        listener?.onRotateDown()
    }


    class ObservableList<T> {

        protected val list: MutableList<T>
        protected val onAdd: PublishSubject<T>
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
    }
}