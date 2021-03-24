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

/**
 * Recognises the horizontal and vertical rotation of the device through the gravity sensor.
 * Notifies the observers with the following events:
 *      - TOWARDS: If the piece screen is vertically facing the user.
 *      - AWAY: If the piece screen is vertically away from the user.
 *      - ROTATE_LEFT: If the piece is starting to be rotated horizontally to the left.
 *      - ROTATE_RIGHT: If the piece is starting to be rotated horizontally to the right.
 *      - AWAY_LEFT: If the piece is fully rotated horizontally to the left and the screen is away from the user.
 *      - AWAY_RIGHT: If the piece is fully rotated horizontally to the right and the screen is away from the user.
 *
 * @param context the application context.
 * @param activity the application activity.
 *
 * @author Luis Carvalho
 */
class HVRotationRecogniser(val context: Context, val activity: Activity) : AbstractRecogniser(){

    /**
     * List of sensors.
     */
    private var disposables: ArrayList<Disposable> = arrayListOf()

    /**
     * Last event that happened in the vertical axis.
     */
    private var lastVerticalGravityEvent: RecogniserEvent? = null

    /**
     * Last event that happened in the horizontal axis.
     */
    private var lastHorizontalGravityEvent: RecogniserEvent? = null

    /**
     * Timestamp of when new content was displayed.
     */
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

    /**
     * Analyses a given gravity sensor event into a recognisable vertical or horizontal events and notifies the observers.
     *
     * @param event gravity event from the sensor.
     */
    private fun processGravity(event: RxSensorEvent) {
        val x = floor(event.values[0]).toInt()
        val y = floor(event.values[1]).toInt()
        val z = floor(event.values[2]).toInt()

        //Vertical
        if (y >= 7 && z > -7 && z < 9 && lastVerticalGravityEvent != RecogniserEvent.TOWARDS) {

            if(newContentTimestamp != null) {
                logAwayDuration(LogType.AWAY_TOWARDS_DURATION)
            }
            Log.d("REC", "TOWARDS")
            lastVerticalGravityEvent = RecogniserEvent.TOWARDS
            notifyEvent(RecogniserEvent.TOWARDS)
        }
        else if (y < /*1*/-2 && z > -9 && z <= 9 && lastVerticalGravityEvent != RecogniserEvent.AWAY) {
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
                notifyEvent(RecogniserEvent.AWAY_LEFT)

            } else if(lastHorizontalGravityEvent == RecogniserEvent.ROTATE_RIGHT) {
                Log.d("yyy", "$y")
                newContentTimestamp = System.currentTimeMillis()

                Log.d("REC", "AWAY RIGHT")
                lastHorizontalGravityEvent = RecogniserEvent.AWAY_RIGHT
                notifyEvent(RecogniserEvent.AWAY_RIGHT)
            }
        }

    }

    /**
     * Logs the duration of the piece rotation.
     *
     * @param logType type of the to be recorded.
     */
    private fun logAwayDuration(logType: LogType) {
        val awayTime = System.currentTimeMillis() - newContentTimestamp!!
        Log.d("Away time", "$awayTime")
        val content = mutableListOf("awayTime:$awayTime")
        Runnable { Logger.log( logType, content, context!! ) }.run()
        newContentTimestamp = null
    }

}