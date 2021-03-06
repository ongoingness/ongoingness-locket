package uk.ac.ncl.openlab.ongoingness.views

import android.content.pm.ActivityInfo
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.collections.*
import uk.ac.ncl.openlab.ongoingness.controllers.*
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.recognisers.*
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.getMacAddress
import java.io.File

/**
 * Main activity of the app. Chooses what code to run based on the selected flavour.
 * In charge of starting the presenters, collections, recognisers classes and hooking them to a controller.
 *
 * @author Luis Carvalho
 */
class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider, Presenter.View {

    /**
     * The max brightness of the screen.
     */
    private val maxBrightness: Float = 1.0f

    /**
     * The min brightness of the screen.
     */
    private val minBrightness: Float = 0.01f

    /**
     * The controller of the application.
     */
    private var controller: AbstractController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setBrightness(maxBrightness)

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

            //Anew
            "locket_touch" -> {

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@MainActivity)

                val recogniser = TouchRevealRecogniser(applicationContext, this@MainActivity)

                val contentCollection = AnewContentCollection(this@MainActivity)

                controller = AnewController(context = applicationContext,
                        presenter = presenter,
                        recogniser = recogniser,
                        contentCollection = contentCollection,
                        startedWithTap = startedWithTap,
                        faceState = faceState,
                        battery = battery,
                        pullContentOnWake = false)

                controller?.setup()

            }

            //Anew
            "locket_touch_inverted" -> {

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@MainActivity)

                val recogniser = InvertedTouchRevealRecogniser(applicationContext, this@MainActivity)

                val contentCollection = AnewContentCollection(this@MainActivity)

                controller = InvertedAnewController(context = applicationContext,
                        presenter = presenter,
                        recogniser = recogniser,
                        contentCollection = contentCollection,
                        startedWitTap = startedWithTap,
                        faceState = faceState,
                        battery = battery,
                        pullContentOnWake = BuildConfig.FETCH_ON_AWAKE)

                controller?.setup()

                val mac = getMacAddress()
                Log.d("MAC",mac)

            }

            "refind" -> {

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.refind_cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.refind_cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@MainActivity)

                val recogniser = RotationRecogniser(applicationContext)

                val contentCollection = RefindContentCollection(this@MainActivity)

                controller = RefindController(context = applicationContext,
                        presenter = presenter,
                        recogniser = recogniser,
                        contentCollection = contentCollection,
                        pullContentOnWake = true)

                controller?.setup()

            }

            //Ivvor
            "locket_touch_s" -> {

                var recogniser: AbstractRecogniser

                val presenter = Presenter(applicationContext,
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.s_cover), getScreenSize(), getScreenSize(), false),
                        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.s_cover_white), getScreenSize(), getScreenSize(), false))
                presenter.attachView(this@MainActivity)

                val contentCollection = IvvorContentCollection(this@MainActivity)

                recogniser = HVRotationRecogniser(applicationContext, this@MainActivity)

                controller = IvvorController(context = applicationContext,
                        presenter = presenter,
                        recogniser = recogniser,
                        contentCollection = contentCollection,
                        faceState = faceState,
                        battery = battery,
                        pullContentOnWake = false)

                controller?.setup()

                val mac = getMacAddress()
                Log.d("MAC",mac)

            }
        }
    }

    override fun onResume() {
        super.onResume()
        controller?.start()
    }

    /**
     * Update the background of the app with a bitmap.
     *
     * @param bitmap The bitmap to set the background to.
     */
    override fun updateBackgroundWithBitmap(bitmap: Bitmap) {
        runOnUiThread {
            macAddress.visibility = View.INVISIBLE
            Glide.with(this).load(bitmap).placeholder(BitmapDrawable(bitmap)).into(image)
        }
    }

    /**
     * Update the background with a given file. Supports images and gifs.
     *
     * @param file the file to be displayed.
     * @param contentType type of content to be displayed.
     * @param bitmap either the first frame of a gif or the image it self.
     */
    override fun updateBackground(file: File, contentType: ContentType, bitmap: BitmapDrawable) {
        runOnUiThread {
            macAddress.visibility = View.INVISIBLE
            when(contentType) {
                ContentType.IMAGE ->
                    Glide.with(this)
                        .load(bitmap)
                        .placeholder(bitmap)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(image)
                ContentType.GIF -> {
                    Glide.with(this)
                        .asGif()
                        .load(file)
                        .placeholder(bitmap)
                        .listener(object : RequestListener<GifDrawable>{
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                                return false
                            }

                            override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
                                return false
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(image)
                }
            }
        }
    }

    /**
     * Get the screen size of a device.
     * @return the size of the screen.
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
     *
     * @param addr text to be displayed.
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
        controller?.stop()
        finish()
    }

    private class MyAmbientCallback : AmbientModeSupport.AmbientCallback()

}