package uk.ac.ncl.openlab.ongoingness


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject



/**
 * Created by Kyle Montague on 10/11/2018.
 */
class RotationRecogniser(val context: Context) {


    val TAG = "RR"

    var listener:RotationRecogniser.Listener? = null
    var accelerometerDisposable: Disposable? = null
    var gravityDisposable: Disposable? = null
    var gyroDisposable: Disposable? = null
    var stateDisposable: Disposable? = null

    var states = ObservableList<State>()
    var orientations = ObservableList<Orientation>()

    enum class State {
        UNKNOWN,
        TOWARDS,
        UP,
        AWAY,
        DOWN
    }

    enum class Orientation{
        LEFT,
        RIGHT,
        FLAT,
        UNKNOWN
    }


    var enterState = State.UNKNOWN
    var previousState = State.UNKNOWN
    var currentOrientation = Orientation.UNKNOWN

    private var timeoutDuration = 1000L * 60 * 1 //one minute timeout
    private var movementThreshold = 20.0

    fun setTimeoutDuration(duration:Long){
        if(duration >= 1000L * 10){
            timeoutDuration = duration
        }
    }

    fun setMovementThreshold(threshold:Double){
        if(movementThreshold >= 5){
            movementThreshold = threshold
        }
    }

    fun start(listener: Listener){
        this.listener = listener
        //start

        accelerometerDisposable = RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY)
                .subscribeOn(Schedulers.computation())
                .filter(RxSensorFilter.minAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
//                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> process(rxSensorEvent) }

//        gravityDisposable = RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY)
//                .subscribeOn(Schedulers.computation())
//                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { rxSensorEvent -> checkGravity(rxSensorEvent)}

        states = ObservableList()
        stateDisposable = states.observable
                .subscribeOn(Schedulers.computation())
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state -> onStateChange(state) }
//                .subscribeBy(onNext ={
//                    Log.d(TAG,"new state ${it.name}")
//                },onError ={},onComplete = {})



        orientations = ObservableList()
        orientations.observable
                .subscribeOn(Schedulers.computation())
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { orientation -> onOrientationChange(orientation) }
    }




    fun stop(){
        this.listener = null
        this.accelerometerDisposable?.dispose()
        this.gravityDisposable?.dispose()
        this.stateDisposable?.dispose()
    }


    val standardGravity = SensorManager.STANDARD_GRAVITY
    val thresholdGraqvity = standardGravity/1.3

    private fun checkGravity(rxSensorEvent: RxSensorEvent) {
        val x = rxSensorEvent.values[0]
        val y = rxSensorEvent.values[1]
        val z = rxSensorEvent.values[2]

        when {
            x >= thresholdGraqvity -> currentOrientation = Orientation.LEFT
            x <= -thresholdGraqvity -> currentOrientation = Orientation.RIGHT
            z >= thresholdGraqvity -> currentOrientation = Orientation.FLAT
            y >= thresholdGraqvity -> currentOrientation = Orientation.FLAT
        }

        orientations.add(currentOrientation)
    }

    private fun onOrientationChange(orientation: RotationRecogniser.Orientation) {
        Log.d(TAG,"orientation: ${orientation.name}")

        if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT){
            //flush the buffer, we've just changed orientation
        }else{
            //we are now back into the flat orientation and ready for simple recognition.
        }
        listener?.onOrientationChange(orientation)
    }

    private fun process(event: RxSensorEvent) {
        processState2(event)
    }


    private fun processState2(rxSensorEvent: RxSensorEvent) {
        val x = rxSensorEvent.values[0]
        val y = rxSensorEvent.values[1]
        val z = rxSensorEvent.values[2]

        var currentState = previousState

        when{
            z >= thresholdGraqvity -> currentState = State.UP
            z <= -thresholdGraqvity -> currentState = State.DOWN
            y >= thresholdGraqvity -> currentState = State.TOWARDS
            y <= -thresholdGraqvity -> currentState = State.AWAY
        }

        if(currentState != State.UNKNOWN)
            states.add(currentState)
    }

    private fun processState(event: RxSensorEvent){
        var currentState = State.UNKNOWN

        val y = event.values[1]
        val z = event.values[2]

        Log.d(TAG,"y: ${y} z: ${z}")

        if(y >= 3 && (z in -4..9)){
            currentState = State.TOWARDS
        }else if((y in -7..3) && (z in 5.0..9.8)){
            currentState = State.UP

        }else if((z in -6.0..-20.0) && (y in -5.0..4.5)){ //(y in -5..5) &&
            currentState = State.DOWN
        }
        else if((y in -6.0..-20.0) && (z in -5.0..3.5)){ //&& (z in -6..2)
            currentState = State.AWAY
        }

        //        if(currentState == previousState)
//            return
//        else{
//            if(currentState == State.DOWN){
//                //entering
//                enterState = previousState
//                //change image
//                when(enterState){
//                    State.TOWARDS -> goBack()
//                    State.AWAY -> goForward()
//                }
//
//            }else if(previousState == State.DOWN){
//                //exiting
//                if(currentState == enterState){
//                    //undo change
//                    when(enterState){
//                        State.TOWARDS -> goForward()
//                        State.AWAY -> goBack()
//                    }
//                }
//            }
//        }

        if(currentState != State.UNKNOWN)
            states.add(currentState)
    }

    private fun onStateChange(currentState: RotationRecogniser.State) {

        Log.d(TAG,"state: ${currentState.name}")

        if(currentState == State.DOWN){
            //entering
            enterState = previousState
            Log.d(TAG, "prev: ${previousState} current: ${enterState}")
            //change image
            when(enterState){
                State.TOWARDS -> goBack()
                State.AWAY -> goForward()
            }

        }else if(previousState == State.DOWN){
            //exiting
            if(currentState == enterState){
                //undo change
                when(enterState){
                    State.TOWARDS -> goForward()
                    State.AWAY -> goBack()
                }
            }
        }

        previousState = currentState
    }

    private fun goForward() {
        Log.d(TAG,"goforward")
        listener?.onRotateUp()
    }

    private fun goBack() {
        Log.d(TAG,"goback")
        listener?.onRotateDown()
    }

//    private fun orientation(event:RxSensorEvent){
//        val rotationMatrix = FloatArray(9)
//        val orientationMatrix = FloatArray(3)
//
//        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
//        SensorManager.getOrientation(rotationMatrix, orientationMatrix)
//
//        Log.d(TAG,"Azimuth: ${orientationMatrix[0]}, Pitch: ${orientationMatrix[1]}, Roll: ${orientationMatrix[2]}")
//    }


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



    interface Listener{

        fun onOrientationChange(orientation: Orientation )

        fun onRotateUp()

        fun onRotateDown()

        fun onRotateLeft()

        fun onRotateRight()

        fun onStandby()
    }
}