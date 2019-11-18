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
open class RotationRecogniserNew(val context: Context) : AbstractRecogniser(context) {

    private var disposables = arrayListOf<Disposable>()
    private var states = ObservableList<State>()

    private enum class State {
        UNKNOWN,
        TOWARDS,
        UP,
        AWAY,
        DOWN
    }

    private var enterState = State.UNKNOWN
    private var previousState = State.UNKNOWN

    override fun start() {

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


            notifyEvent(RecogniserEvent.STARTED)

        } catch (e: SensorNotFoundException) {
            Log.d("RotationRecogniser", "Sensor missing, running without sensors")
            throw e
        }
    }

    override fun stop() {
        for (disposable in disposables) {
            disposable.dispose()
        }
        notifyEvent(RecogniserEvent.STOPPED)
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
        }

        if (currentState != State.UNKNOWN)
            states.add(currentState)
    }


    private fun onStateChange(currentState: State) {

        if (currentState == State.DOWN) {
            //entering
            enterState = previousState

            //change image
            when (enterState) {
                State.TOWARDS -> notifyEvent(RecogniserEvent.ROTATE_DOWN)
                State.AWAY -> notifyEvent(RecogniserEvent.ROTATE_UP)
                else -> {}
            }

        } else if (previousState == State.DOWN) {
            //exiting
            if (currentState == enterState) {
                //undo change
                when (enterState) {
                    State.TOWARDS -> notifyEvent(RecogniserEvent.ROTATE_UP)
                    State.AWAY -> notifyEvent(RecogniserEvent.ROTATE_DOWN)
                    else -> {}
                }
            }
        }

        previousState = currentState
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

}