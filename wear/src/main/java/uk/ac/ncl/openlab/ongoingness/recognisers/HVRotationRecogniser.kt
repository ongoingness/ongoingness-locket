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
import java.util.ArrayList
import kotlin.math.floor

class HVRotationRecogniser(val context: Context, val activity: Activity) : AbstractRecogniser(context){

    private var disposables: ArrayList<Disposable> = arrayListOf()
    private var lastVerticalGravityEvent: RecogniserEvent? = null
    private var lastHorizontalGravityEvent: RecogniserEvent? = null

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

        //Log.d("test", "X:$x Y:$y Z:$z")

        //Vertical
        if (y >= 2 && z > -9 && z < 9 && lastVerticalGravityEvent != RecogniserEvent.TOWARDS) {
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
            Log.d("REC", "ROTATE LEFT")
            lastHorizontalGravityEvent = RecogniserEvent.ROTATE_LEFT
            notifyEvent(RecogniserEvent.ROTATE_LEFT)
        } else if (x in -10..-2 && z in 0..9 && lastHorizontalGravityEvent != RecogniserEvent.ROTATE_RIGHT) {
            Log.d("REC", "ROTATE RIGHT")
            lastHorizontalGravityEvent = RecogniserEvent.ROTATE_RIGHT
            notifyEvent(RecogniserEvent.ROTATE_RIGHT)
        }

        if(x in -3..3 && z in -10..-6) {
            if(lastHorizontalGravityEvent == RecogniserEvent.ROTATE_LEFT) {
                Log.d("REC", "AWAY LEFT")
                lastHorizontalGravityEvent = RecogniserEvent.AWAY_LEFT
                notifyEvent(RecogniserEvent.AWAY_LEFT)
            } else if(lastHorizontalGravityEvent == RecogniserEvent.ROTATE_RIGHT) {
                Log.d("REC", "AWAY RIGHT")
                lastHorizontalGravityEvent = RecogniserEvent.AWAY_RIGHT
                notifyEvent(RecogniserEvent.AWAY_RIGHT)
            }
        }

    }

}