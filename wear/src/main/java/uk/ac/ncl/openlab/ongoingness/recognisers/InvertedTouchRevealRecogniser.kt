package uk.ac.ncl.openlab.ongoingness.recognisers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import com.gvillani.rxsensors.RxSensorTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import uk.ac.ncl.openlab.ongoingness.R
import java.util.ArrayList
import kotlin.math.floor

class InvertedTouchRevealRecogniser(val context: Context, val activity: Activity) : AbstractRecogniser(context), GestureDetector.OnGestureListener {

    private var disposables: ArrayList<Disposable> = arrayListOf()
    private var lastGravityEvent: RecogniserEvent? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_UI)
                .subscribeOn(Schedulers.computation())

                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ rxSensorEvent -> processGravity(rxSensorEvent)}, {error -> Log.d("Sensor", "Error: $error") }))


        val gesture = GestureDetector(context, this)
        val touchListener = View.OnTouchListener {
            _, events -> gesture.onTouchEvent(events)
        }

        val mImageView = activity.findViewById<ImageView>(R.id.image)
        mImageView.setOnTouchListener(touchListener)

        notifyEvent(RecogniserEvent.STARTED)
    }

    override fun stop() {
        for (disposable in disposables) {
            disposable.dispose()
        }
        notifyEvent(RecogniserEvent.STOPPED)
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        Log.d("REC", "SingleTap")
        notifyEvent(RecogniserEvent.TAP)
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        Log.d("REC", "LONG_PRESS")
        notifyEvent(RecogniserEvent.LONG_PRESS)
    }

    override fun onShowPress(e: MotionEvent?) {}

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    private fun processGravity(event: RxSensorEvent) {
        val y = floor(event.values[1]).toInt()
        val z = floor(event.values[2]).toInt()


        if (y <= -2 && z > -9 && z < 9 && lastGravityEvent != RecogniserEvent.TOWARDS) {
            lastGravityEvent = RecogniserEvent.TOWARDS
            notifyEvent(RecogniserEvent.TOWARDS)
        } else if (y > 5 && z <= 8 && lastGravityEvent != RecogniserEvent.AWAY ) {
            lastGravityEvent = RecogniserEvent.AWAY
            notifyEvent(RecogniserEvent.AWAY)
        }

    }
    
}