package uk.ac.ncl.openlab.ongoingness.recognisers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.GestureDetector
import android.view.MotionEvent
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import com.gvillani.rxsensors.RxSensorTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import kotlin.math.floor

class TouchRevealRecogniserNew(val context: Context) : AbstractRecogniser(context), GestureDetector.OnGestureListener {

    private var disposables: ArrayList<Disposable> = arrayListOf()

    override fun start() {
        disposables.add(RxSensor.sensorEvent(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_UI)
                .subscribeOn(Schedulers.computation())
                .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                .compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processGravity(rxSensorEvent) })
        notifyEvent(RecogniserEvent.STARTED)
    }

    override fun stop() {
        for (disposable in disposables) {
            disposable.dispose()
        }
        notifyEvent(RecogniserEvent.STOPPED)
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        notifyEvent(RecogniserEvent.TAP)
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        notifyEvent(RecogniserEvent.LONG_PRESS)
    }

    override fun onShowPress(e: MotionEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDown(e: MotionEvent?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun processGravity(event: RxSensorEvent) {
        val y = floor(event.values[1]).toInt()
        val z = floor(event.values[2]).toInt()

        if (y >= 2 && z > -9 && z < 9)
            notifyEvent(RecogniserEvent.TOWARDS)
        else if (y > -2  && y < 2 && z > -9 && z < 9)
            notifyEvent(RecogniserEvent.AWAY)
    }

}