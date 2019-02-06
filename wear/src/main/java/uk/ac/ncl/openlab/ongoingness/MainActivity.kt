package uk.ac.ncl.openlab.ongoingness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_LIGHT
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.wear.widget.BoxInsetLayout
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.WindowManager


class MainActivity : WearableActivity(), MainPresenter.View, SensorEventListener {
    private val presenter: MainPresenter = MainPresenter()
    private var maxLight: Float = 0.0f
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var lightEventListener: LightEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        presenter.attachView(this)
        presenter.setContext(applicationContext)

        Log.d("OnCreate", "Getting bitmap")

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /*
         * Create the sensor manager.
         * Get a light sensor, will return null if there is no sensor.
         *
         * TODO: Add flag for when sensor is not present, fallback to accelerometer
         */
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(TYPE_LIGHT)

        /*
         * If there is a light sensor, then get the maximum range.
         */
        if (lightSensor == null) {
            Log.d("onCreate", "No light sensor")
        } else {
            maxLight = lightSensor!!.maximumRange
        }

        lightEventListener = LightEventListener(this)
        sensorManager?.registerListener(lightEventListener, lightSensor!!, SensorManager.SENSOR_DELAY_FASTEST)

        // Create a background bit map from drawable
        updateBackground(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                resources, R.drawable.placeholder), getScreenSize(), getScreenSize(), false)!!)

        presenter.generateToken {presenter.fetchAllMedia()}

        rotationRecogniser = RotationRecogniser(this)
    }

    /**
     * Restart the activity recogniser
     * Register the lightEventSensor
     */
    override fun onResume() {
        super.onResume()
        rotationRecogniser?.start(rotationListener)
        sensorManager?.registerListener(lightEventListener, lightSensor!!, SensorManager.SENSOR_DELAY_FASTEST)
    }

    /**
     * Stop the activity recogniser
     * Unregister the light event listener
     */
    override fun onPause() {
        super.onPause()
        rotationRecogniser?.stop()
        sensorManager?.unregisterListener(lightEventListener)

    }

    /**
     * Detach the presenter
     */
    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    /**
     * Update the background of the watch face.
     *
     * @param bitmap The bitmap to set the background to.
     * @return Unit
     */
    override fun updateBackground(bitmap: Bitmap) {
        runOnUiThread {
            // Stuff that updates the UI
            try {
                val background = findViewById<BoxInsetLayout>(R.id.background)
                background.background = BitmapDrawable(resources, bitmap)
            } catch (e: java.lang.Error) {
                e.printStackTrace()
            }

        }
    }

    private var rotationRecogniser: RotationRecogniser? = null
    private val rotationListener = object : RotationRecogniser.Listener {

        override fun onRotateUp() {
//            val background = presenter.updateBitmap()
//            updateBackground(background)
        }

        override fun onRotateDown() {
        }

        override fun onRotateLeft() {
        }

        override fun onRotateRight() {
        }

        override fun onStandby() {
            Log.d("WATCH", "standby")
            finish()
        }
    }

    /**
     * Get the screen size of a device.
     */
    override fun getScreenSize(): Int {
        val scaleFactor = 1.1
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return (size.x * scaleFactor).toInt()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // TODO: Implement
    }

    /**
     * Handle a change from the light sensor
     */
    override fun onSensorChanged(p0: SensorEvent?) {
        val value: Float? = p0!!.values[0]
        Log.d("onSensorChanged", "Lux: $value")
    }

    /**
     * Handle the locket being opened.
     */
    override fun openLocket() {
        val bitmap: Bitmap? = presenter.updateBitmap()
        updateBackground(bitmap!!)
    }

    /**
     * Handle the locket being closed.
     */
    override fun closeLocket() {
        Log.d("closeLocket", "Locket closed")
    }
}