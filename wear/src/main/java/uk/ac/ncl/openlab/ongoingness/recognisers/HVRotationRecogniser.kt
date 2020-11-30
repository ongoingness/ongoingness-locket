package uk.ac.ncl.openlab.ongoingness.recognisers

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import com.gvillani.rxsensors.RxSensorTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import java.util.ArrayList
import kotlin.math.floor

class HVRotationRecogniser(val context: Context, val activity: Activity) : AbstractRecogniser(context){

    private var disposables: ArrayList<Disposable> = arrayListOf()
    private var lastVerticalGravityEvent: RecogniserEvent? = null
    private var lastHorizontalGravityEvent: RecogniserEvent? = null

    private var newContentTimestamp: Long? = null

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

    private fun processGravity(event: RxSensorEvent) {
        val x = floor(event.values[0]).toInt()
        val y = floor(event.values[1]).toInt()
        val z = floor(event.values[2]).toInt()

        var imageChanged = false

        //Vertical
        if (y >= 7 && z > -7 && z < 9 && lastVerticalGravityEvent != RecogniserEvent.TOWARDS) {

            if(newContentTimestamp != null) {
                logAwayDuration(LogType.AWAY_TOWARDS_DURATION)
            }

            Log.d("REC", "TOWARDS")
            lastVerticalGravityEvent = RecogniserEvent.TOWARDS
            notifyEvent(RecogniserEvent.TOWARDS)
        }
        else if (y < -2 && z > -9 && z < 9 && lastVerticalGravityEvent != RecogniserEvent.AWAY) {
            Log.d("REC", "AWAY")
            lastVerticalGravityEvent = RecogniserEvent.AWAY
            notifyEvent(RecogniserEvent.AWAY)
        }



        //Horizontal
        if(x in 2..9 && z in 0..9 && lastHorizontalGravityEvent != RecogniserEvent.ROTATE_LEFT) {

            if(newContentTimestamp != null) {
                logAwayDuration(LogType.AWAY_LEFT_DURATION)
            }

            Log.d("REC", "ROTATE LEFT")
            lastHorizontalGravityEvent = RecogniserEvent.ROTATE_LEFT
            notifyEvent(RecogniserEvent.ROTATE_LEFT)
        } else if (x in -10..-2 && z in 0..9 && lastHorizontalGravityEvent != RecogniserEvent.ROTATE_RIGHT) {

            if(newContentTimestamp != null) {
                logAwayDuration(LogType.AWAY_RIGHT_DURATION)
            }

            Log.d("REC", "ROTATE RIGHT")
            lastHorizontalGravityEvent = RecogniserEvent.ROTATE_RIGHT
            notifyEvent(RecogniserEvent.ROTATE_RIGHT)
        }

        if(x in -3..3 && z in -10..-6) {

            if(lastHorizontalGravityEvent == RecogniserEvent.ROTATE_LEFT) {
                Log.d("yyy", "$y")
                newContentTimestamp = System.currentTimeMillis()

                Log.d("REC", "AWAY LEFT")
                lastHorizontalGravityEvent = RecogniserEvent.AWAY_LEFT
                imageChanged = true
                notifyEvent(RecogniserEvent.AWAY_LEFT)

            } else if(lastHorizontalGravityEvent == RecogniserEvent.ROTATE_RIGHT) {
                Log.d("yyy", "$y")
                newContentTimestamp = System.currentTimeMillis()

                Log.d("REC", "AWAY RIGHT")
                lastHorizontalGravityEvent = RecogniserEvent.AWAY_RIGHT
                imageChanged = true
                notifyEvent(RecogniserEvent.AWAY_RIGHT)
            }
        }

        /*
        if(!imageChanged && lastVerticalGravityEvent == RecogniserEvent.TOWARDS &&
                x == -1 && y in 0 .. 6 && z in -10 .. -8) {
            Log.d("xxx", "$x")
            newContentTimestamp = System.currentTimeMillis()

            Log.d("REC", "AWAY TOWARDS")
            lastVerticalGravityEvent = RecogniserEvent.AWAY_TOWARDS
            notifyEvent(RecogniserEvent.AWAY_TOWARDS)
        }
         */
    }

    private fun logAwayDuration(logType: LogType) {
        val awayTime = System.currentTimeMillis() - newContentTimestamp!!
        Log.d("Away time", "$awayTime")
        val content = mutableListOf("awayTime:$awayTime")
        Runnable { Logger.log( logType, content, context!! ) }.run()
        newContentTimestamp = null
    }

}