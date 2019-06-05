package uk.ac.ncl.openlab.ongoingness.views

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_LIGHT
import android.hardware.SensorManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.recognisers.LightEventListener
import uk.ac.ncl.openlab.ongoingness.recognisers.RotationRecogniser
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.viewmodel.MainPresenter

class MainActivity : WearableActivity(), MainPresenter.View {
    private val presenter: MainPresenter = MainPresenter()
    private var maxLight: Float = 0.0f
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var lightEventListener: LightEventListener? = null
    private var isReady: Boolean = false
    private val maxBrightness: Float = 1.0f
    private val minBrightness: Float = 0.01f

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

        Glide.with(this).load(R.drawable.placeholder).into(image)


        when(FLAVOR){
            "locket" ->{
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

            }
            "refind" -> { rotationRecogniser = RotationRecogniser(this)
            }
        }


    }

    /**
     * Restart the activity recogniser
     * Register the lightEventSensor
     */
    override fun onResume() {
        super.onResume()


        presenter.loadPermCollection()


        if (hasConnection(applicationContext)) {
            presenter.fetchAllMedia()
        }

        when(FLAVOR){
            "locket" ->{ sensorManager?.registerListener(lightEventListener, lightSensor!!, SensorManager.SENSOR_DELAY_FASTEST) }
            "refind" -> { rotationRecogniser?.start(rotationListener) }
        }
    }

    /**
     * Stop the activity recogniser
     * Unregister the light event listener
     */
    override fun onPause() {
        super.onPause()
        when(FLAVOR){
            "locket" ->{ sensorManager?.unregisterListener(lightEventListener) }
            "refind" -> { rotationRecogniser?.stop() }
        }

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
            macAddress.visibility = View.INVISIBLE
            Glide.with(this).load(bitmap).into(image)
        }
    }

    private var rotationRecogniser: RotationRecogniser? = null
    private val rotationListener = object : RotationRecogniser.Listener {

        override fun onRotateUp() {}

        override fun onRotateDown() {}

        override fun onRotateLeft() {}

        override fun onRotateRight() {}

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

    /**
     * Handle the locket being opened.
     */
    override fun openLocket() {
        val bitmap: Bitmap? = presenter.updateBitmap()
        updateBackground(bitmap!!)
        setBrightness(maxBrightness)
    }

    /**
     * Handle the locket being closed.
     */
    override fun closeLocket() {
        setBrightness(minBrightness)
        Log.d("closeLocket", "Locket closed")
    }

    /**
     * Brightness should be between 0.00f and 0.01f
     *
     * @param brightness Float
     */
    private fun setBrightness(brightness: Float) {
        if (brightness.compareTo(minBrightness) < 0) return
        if (brightness.compareTo(maxBrightness) > 0) return

        val params: WindowManager.LayoutParams = window.attributes
        params.screenBrightness = brightness
        window.attributes = params
    }

    /**
     * Get the ready flag for the activity.
     * This is used to start the lightSensorListener.
     *
     * @return Boolean
     */
    override fun getReady(): Boolean {
        return this.isReady
    }

    /**
     * Get the ready flag for the activity.
     * This is used to start the lightSensorListener.
     *
     * @param ready Boolean
     */
    override fun setReady(ready: Boolean) {
        this.isReady = ready
    }

    /**
     * Display text on the device
     */
    override fun displayText(addr: String) {
        runOnUiThread {
            macAddress.visibility = View.VISIBLE
            macAddress.text = addr
        }
    }
}