package uk.ac.ncl.openlab.ongoingness.old

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log

class LightEventListener(private val view: MainPresenter.View): SensorEventListener {
    private val closedThreshold: Float = 80.0f
    private var isClosed: Boolean = true
    private var lastRead: Long = 0
    private var timeThreshold: Long = 100
    private var isDiff: Boolean = false
    private var lastIsClosed: Boolean = true

    override fun onSensorChanged(p0: SensorEvent?) {
        val value: Float? = p0!!.values[0]
        val current: Long = System.currentTimeMillis()

        if(!view.getReady()) return

        // Too recent
        if ((current - lastRead) <= timeThreshold) {
            lastRead = current
            return
        }

        // Determine if closed.
        isClosed = when(value!!.compareTo(closedThreshold)) {
            -1 ->  true
            0  ->  true
            1  ->  false
            else -> false
        }

        /*
         * Check if there is a difference from last read, does the watch need to update?
         * Assign the current state to the last state
         * Record timestamp
         */
        isDiff = isClosed != lastIsClosed
        lastIsClosed = isClosed
        lastRead = current

        // If there is no difference, then return
        if(!isDiff) return

        /*
         * If the locket should be closed, put into standby?
         * If the locket should be open, update the background bitmap.
         */
        if (isClosed) {
            //view.closeLocket()
            Log.d("onSensorChanged", "Closing Locket")
        } else {
            //view.openLocket()
            Log.d("onSensorChanged", "Opening Locket")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}