package uk.ac.ncl.openlab.ongoingness.views

import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.recognisers.*
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.*
import uk.ac.ncl.openlab.ongoingness.viewmodel.*
import java.io.File

class TestActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider, Presenter.View {

    private val maxBrightness: Float = 1.0f
    private val minBrightness: Float = 0.01f

    private lateinit var controller: AbstractController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Logger.start(applicationContext)
        Logger.log(LogType.ACTIVITY_STARTED, listOf(), applicationContext!!)

        var startedWithTap = false
        var faceState = "UNKNOWN"
        var battery = 0f


        if(intent.hasExtra("startedWithTap"))
            startedWithTap = intent.getBooleanExtra("startedWithTap", false)

        if(intent.hasExtra("state"))
            faceState = intent.getStringExtra("state")


        if(intent.hasExtra("battery"))
            battery = intent.getFloatExtra("battery", 0f)

        when(FLAVOR){
            "locket_touch" -> {

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@TestActivity)

                val recogniser = TouchRevealRecogniserNew(applicationContext, this@TestActivity)

                val contentCollection = AnewContentCollection(this@TestActivity)

                controller = AnewController(context = applicationContext,
                                            presenter = presenter,
                                            recogniser = recogniser,
                                            contentCollection = contentCollection,
                                            startedWitTap = startedWithTap,
                                            faceState = faceState,
                                            battery = battery)

                controller.setup()

            }

            "locket_touch_inverted" -> {

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@TestActivity)

                val recogniser = InvertedTouchRevealRecogniser(applicationContext, this@TestActivity)

                val contentCollection = AnewContentCollection(this@TestActivity)

                controller = InvertedAnewController(context = applicationContext,
                        presenter = presenter,
                        recogniser = recogniser,
                        contentCollection = contentCollection,
                        startedWitTap = startedWithTap,
                        faceState = faceState,
                        battery = battery)

                controller.setup()

            }

            "refind" -> {

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.refind_cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.refind_cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@TestActivity)

                val recogniser = RotationRecogniserNew(applicationContext)

                val contentCollection = RefindContentCollection(this@TestActivity)

                controller = RefindController(context = applicationContext,
                        presenter = presenter,
                        recogniser = recogniser,
                        contentCollection = contentCollection)

                controller.setup()

            }
        }




    }

    /**
     * Restart the activity recogniser
     * Register the lightEventSensor
     */
    override fun onResume() {
        super.onResume()
        controller.start()
    }

    /**
     * Stop the activity recogniser
     * Unregister the light event listener
     */
    override fun onPause() {
        super.onPause()

    }

    /**
     * Detach the presenter
     */
    override fun onDestroy() {
        super.onDestroy()
        controller.stop()


    }

    /**
     * Update the background of the watch face.
     *
     * @param bitmap The bitmap to set the background to.
     * @return Unit
     */
    override fun updateBackgroundWithBitmap(bitmap: Bitmap) {
        runOnUiThread {
            macAddress.visibility = View.INVISIBLE
            Glide.with(this).load(bitmap).into(image)
        }
    }

    /**
     * Update the background of the watch face.
     */
    override fun updateBackground(file: File, contentType: ContentType) {
        runOnUiThread {
            macAddress.visibility = View.INVISIBLE
            when(contentType) {
               ContentType.IMAGE ->
                    Glide.with(this).load(file).into(image)
                ContentType.GIF ->
                    Glide.with(this).asGif().load(file).into(image)
            }
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
     * Display text on the device
     */
    override fun displayText(addr: String) {
        runOnUiThread {
            macAddress.visibility = View.VISIBLE
            macAddress.text = addr
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return MyAmbientCallback()
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

    override fun finishActivity() {
        finish()
    }

    private class MyAmbientCallback : AmbientModeSupport.AmbientCallback()
}