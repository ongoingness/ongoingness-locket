package uk.ac.ncl.openlab.ongoingness.recognisers

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
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class LightRecogniser(private val context: Context): Observable() {

    private var disposable: Disposable? = null

    private var buffer: Deque<Pair<Long, Float>> = ArrayDeque<Pair<Long, Float>>()
    private var bufferSize: Int = 20


    private var max: Float = 0F
    private var min: Float = 100F

    private var lastSampleTime: Long = System.currentTimeMillis()

    private var lastCoverSampleTime: Long = System.currentTimeMillis()

    private var state: String = "None"

    fun start() {

        disposable = RxSensor.sensorEvent(context, Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_FASTEST)
                .subscribeOn(Schedulers.computation())
                //.debounce(50, TimeUnit.MILLISECONDS)
                //.distinctUntilChanged(RxSensorFilter.uniqueEventValues())
                //.compose<RxSensorEvent>(RxSensorTransformer.lowPassFilter(0.2f))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rxSensorEvent -> processLight(rxSensorEvent) }

    }



    fun stop() {
        disposable!!.dispose()
    }


    private fun processLight(event: RxSensorEvent) {
        if(event.timestamp-lastSampleTime > 10000000) {


            if (buffer!!.size == bufferSize)
                buffer.removeFirst()
            buffer.addLast(Pair(event.timestamp, event.values[0]))


            var newState: String = getNearestState(event.values[0])

            var stateChanged: Boolean = newState != state

            if(stateChanged)
                Log.d("testing", "$state    ->    $newState")


            if( !stateChanged && state == "Closed" && lastAreCover(3)) {

                if (event.timestamp - lastCoverSampleTime > 5000000000 /*5 seconds*/) {
                    Log.d("TAF", "Long_COVER---------------------------------")
                } else if (event.timestamp - lastCoverSampleTime > 100000000 /*1 second*/) {
                    Log.d("TAF", "SHORT_COVER---------------------------------")
                }

            } else if( stateChanged && newState == "Closed" ) {
                lastCoverSampleTime = event.timestamp
            }

            state = newState

            if(event.values[0] > max) {
                max = event.values[0]
            } else if(event.values[0] < min) {
                min = event.values[0]
            }

        }
        Log.d("TESTING", "Last: ${event.values[0]}      Max: $max       Min: $min")
    }


    private fun getNearestState(value: Float): String {

        var minWindowTop: Float = min + min / 2
        var maxWindowBottom: Float = max - max / 2

        //Log.d("TAG", "minWindowTop: $minWindowTop   maxWindowBottom: $maxWindowBottom")

        if(value < minWindowTop) {
            //Log.d("testing", "Closed")
            return "Closed"
        } else if(value > maxWindowBottom) {
            //Log.d("testing", "Open")
            return "Open"
        }
        return "None"
    }

    private fun lastAreCover(n: Int): Boolean {
        if(buffer.size >= n) {
            for(i in 1..n) {
                if(getNearestState(buffer.toMutableList()[buffer.size-i].second) != "Closed")
                    return false
            }
        } else {
            buffer.forEach { element ->
                if(getNearestState(element.second) != "Closed")
                    return false
            }
        }
        return true
    }


}