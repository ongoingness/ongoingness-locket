package uk.ac.ncl.openlab.ongoingness

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.wear.widget.BoxInsetLayout
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.WindowManager


class MainActivity : WearableActivity(), MainPresenter.View {

    private val presenter: MainPresenter = MainPresenter()

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

        // Create a background bit map from drawable
        updateBackground(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                resources, R.drawable.placeholder), getScreenSize(), getScreenSize(), false)!!)

        Log.d("OnCreate", "Getting a connection")

        presenter.generateToken {presenter.fetchAllMedia()}

        rotationRecogniser = RotationRecogniser(this)
    }

    // Restart the activity recogniser
    override fun onResume() {
        super.onResume()
        rotationRecogniser?.start(rotationListener)
    }

    // Pause the activity recogniser
    override fun onPause() {
        super.onPause()
        rotationRecogniser?.stop()

    }

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
}