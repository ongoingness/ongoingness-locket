package uk.ac.ncl.openlab.ongoingness.old

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.*
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.*
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaAsyncTask
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaWorker
import java.io.File
import java.util.Observer

const val PULL_CONTENT_ON_WAKE = false

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider, MainPresenter.View {

    private var presenter: MainPresenter?  = null
    private var isReady: Boolean = false
    private val maxBrightness: Float = 1.0f
    private val minBrightness: Float = 0.01f

    private var revealRecogniser: RevealRecogniser? = null
    private var revealRecogniserObserver: Observer? = null

    private var touchRevealRecogniserOld: TouchRevealRecogniserOld? = null
    private var touchRevealRecogniserObserver: Observer?  = null

    private var rotationRecogniserOld: RotationRecogniserOld? = null
    private var rotationListenerOld: RotationRecogniserOld.Listener? = null

    var isGoingToStop: Boolean = false

    private var isGettingData = false
    private var gotData = false

    lateinit var mImageView: ImageView
    lateinit var gesture : GestureDetector
    lateinit var touchListener: View.OnTouchListener

    private lateinit var bitmapReceiver: BroadcastReceiver

    private lateinit var killRunnable: Runnable

    private val killHandler = Handler()
    private var startTime = System.currentTimeMillis()
    private val timeCheckInterval = 30 * 1000L //30 seconds
    private val killDelta = 5 * 60 * 1000L //5 minutes

    //TODO Very Messy
    private var chargingState = false
    private var awakeState = false
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.start(applicationContext)
        Logger.log(LogType.ACTIVITY_STARTED, listOf(), applicationContext!!)

        killRunnable = Runnable {
            if(System.currentTimeMillis() - startTime > killDelta) {
                Logger.log(LogType.ACTIVITY_TERMINATED, listOf(), applicationContext)
                finish()
            } else {
                killHandler.postDelayed(killRunnable, timeCheckInterval)
            }
        }

        killRunnable.run()

        presenter = MainPresenter()
        presenter!!.attachView(this)
        presenter!!.setContext(applicationContext)
        presenter!!.setWatchMediaRepository(this)

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        when(FLAVOR){
            "locket_touch", "locket_touch_inverted" -> setLocketTouch()
            "locket" -> setLocket()
            "refind" -> setRefind()
        }

    }

    /**
     * Restart the activity recogniser
     * Register the lightEventSensor
     */
    override fun onResume() {
        super.onResume()
        when(FLAVOR){
            "locket" -> { revealRecogniser?.start() }
            "locket_touch", "locket_touch_inverted" -> { touchRevealRecogniserOld?.start() }
            "refind" -> { rotationRecogniserOld?.start(rotationListenerOld!!) }
        }
    }

    /**
     * Stop the activity recogniser
     * Unregister the light event listener
     */
    override fun onPause() {
        super.onPause()
        if (isGoingToStop) {
            when (FLAVOR) {
                "locket" -> {
                    revealRecogniser?.stop()
                }
                "locket_touch","locket_touch_inverted" -> {
                   touchRevealRecogniserOld?.stop()
                }
                "refind" -> {
                    rotationRecogniserOld?.stop()
                }
            }
        }
    }

    /**
     * Detach the presenter
     */
    override fun onDestroy() {
        super.onDestroy()

        when (FLAVOR) {
            "locket" -> {
                revealRecogniser?.deleteObserver(revealRecogniserObserver)
            }
            "locket_touch", "locket_touch_inverted" -> {
                touchRevealRecogniserOld?.deleteObserver(touchRevealRecogniserObserver)
                unregisterReceiver(bitmapReceiver)
            }
            "refind" -> {}
        }

        presenter!!.detachView()

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
    override fun updateBackground(file: File, mediaType: MainPresenter.View.MediaType) {
        runOnUiThread {
            macAddress.visibility = View.INVISIBLE
            when(mediaType) {
                MainPresenter.View.MediaType.IMAGE ->
                    Glide.with(this).load(file).into(image)

                MainPresenter.View.MediaType.GIF ->
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

    override fun getContext(): Context {
        return this.applicationContext
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

    /**
     *  Setup the Locket that has as input the watch position and the touch screen
     */
    private fun setLocketTouch() {

        when(FLAVOR) { "locket_touch_inverted" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT }

        Glide.with(this).load(R.drawable.cover).into(image)

        //Check if is charging
        if(intent.hasExtra("background")) {
            val bitmap = BitmapFactory.decodeByteArray(
                    intent.getByteArrayExtra("background"), 0,
                    intent.getByteArrayExtra("background").size)
            presenter!!.updateCoverBitmap(bitmap)
        }

        if(intent.hasExtra("chargingState")) {
            chargingState = intent.getBooleanExtra("chargingState", false)
            if(awakeState && touchRevealRecogniserOld != null)
                touchRevealRecogniserOld?.updateState(TouchRevealRecogniserOld.State.STANDBY)
        }

        //Charging background receiver
        bitmapReceiver = object: BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                Log.d("NewBitMap", "${intent.extras}")

                if(intent.hasExtra("background")) {
                    val bitmap = BitmapFactory.decodeByteArray(
                            intent.getByteArrayExtra("background"),0,
                            intent.getByteArrayExtra("background").size)
                    presenter!!.updateCoverBitmap(bitmap)
                }

                if(intent.hasExtra("chargingState")) {
                    chargingState = intent.getBooleanExtra("chargingState", false)
                    if(awakeState && touchRevealRecogniserOld != null) {
                        touchRevealRecogniserOld?.updateState(TouchRevealRecogniserOld.State.STANDBY)
                        touchRevealRecogniserOld?.notifyEvent(TouchRevealRecogniserOld.Events.SLEEP)
                    }
                }
            }
        }
        val filter = IntentFilter("BATTERY_INFO").apply {}
        registerReceiver(bitmapReceiver, filter)

        touchRevealRecogniserOld = TouchRevealRecogniserOld(this)
        touchRevealRecogniserObserver = Observer { _, arg ->
            when (arg) {
                TouchRevealRecogniserOld.Events.STARTED -> {


                    when(FLAVOR) {
                        "locket_touch_inverted" -> {

                            if (touchRevealRecogniserOld!!.currentOrientation == TouchRevealRecogniserOld.Orientation.TOWARDS) {
                                presenter!!.setWatchMediaRepository(this@MainActivity)
                                presenter!!.restartIndex()
                                presenter!!.displayContent()
                                touchRevealRecogniserOld?.updateState(TouchRevealRecogniserOld.State.ACTIVE)
                                touchRevealRecogniserOld?.notifyEvent(TouchRevealRecogniserOld.Events.AWAKE)
                            }

                        }
                    }
                }

                TouchRevealRecogniserOld.Events.AWAKE -> {

                    if(chargingState) {
                        //TODO
                        touchRevealRecogniserOld?.updateState(TouchRevealRecogniserOld.State.STANDBY)
                        touchRevealRecogniserOld?.notifyEvent(TouchRevealRecogniserOld.Events.SLEEP)

                    }
                    else {
                        awakeState = true

                        //Stop Activity Kill Thread
                        killHandler.removeCallbacks(killRunnable)

                        if (PULL_CONTENT_ON_WAKE && !gotData && hasConnection(applicationContext)) {

                            gotData = true
                            isGettingData = true
                            presenter!!.pullingData(true)

                            val postExecuteCallback: (result: Boolean) -> Unit = {
                                isGettingData = false
                                presenter!!.setWatchMediaRepository(this@MainActivity)
                                presenter!!.pullingData(false)
                            }

                            PullMediaAsyncTask(postExecuteCallback = postExecuteCallback).execute(this.applicationContext)

                        } else {
                            presenter!!.restartIndex()
                            presenter!!.displayContent()
                        }

                        Logger.log(LogType.WAKE_UP, listOf(), applicationContext)
                    }

                }
                TouchRevealRecogniserOld.Events.NEXT -> {
                    if(!isGettingData)
                        presenter!!.goToNextImage()
                }

                TouchRevealRecogniserOld.Events.SLEEP -> {

                    if(!isGettingData) {

                        awakeState = false

                        var layoutParams: WindowManager.LayoutParams = this.window.attributes
                        layoutParams.dimAmount = 0.75f
                        layoutParams.screenBrightness = 0.1f

                        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        window.attributes = layoutParams

                        windowManager.updateViewLayout(window.decorView, layoutParams)

                        //Start Activity Kill Thread
                        killRunnable.run()

                        presenter!!.hideContent(MainPresenter.CoverType.BLACK)
                        startTime = System.currentTimeMillis()
                        Logger.log(LogType.SLEEP, listOf(), applicationContext)

                    }

                }

                TouchRevealRecogniserOld.Events.STOPPED -> {
                    if(!isGoingToStop) {
                        isGoingToStop = true
                        killHandler.removeCallbacks(killRunnable)
                        Logger.log(LogType.ACTIVITY_TERMINATED, listOf(), applicationContext)
                        finish()

                    }
                }

            }
            setBrightness(maxBrightness)
        }

        touchRevealRecogniserOld?.addObserver(touchRevealRecogniserObserver)

        gesture = GestureDetector(this, touchRevealRecogniserOld)
        touchListener = View.OnTouchListener {
            _, events -> gesture.onTouchEvent(events)
        }
        mImageView = findViewById(R.id.image)
        mImageView.setOnTouchListener(touchListener)

    }

    /**
     *  Setup the Locket that has as input the watch position and the light sensor
     */
    private fun setLocket() {

        Glide.with(this).load(R.drawable.cover).into(image)

        revealRecogniser = RevealRecogniser(this)

        revealRecogniserObserver = Observer { _, arg ->
            when (arg) {
                RevealRecogniser.Events.STARTED -> {

                }
                RevealRecogniser.Events.COVERED -> {

                }
                RevealRecogniser.Events.AWAKE -> {
                    presenter!!.displayContent()

                }
                RevealRecogniser.Events.SHORT_REVEAL -> {
                    presenter!!.goToNextImage()

                }
                RevealRecogniser.Events.SLEEP -> {
                    presenter!!.hideContent(MainPresenter.CoverType.BLACK)

                }
                RevealRecogniser.Events.STOPPED -> {
                    isGoingToStop = true
                    finish()
                }
                RevealRecogniser.Events.HIDE -> {
                    presenter!!.hideContent(MainPresenter.CoverType.BLACK)
                }
                RevealRecogniser.Events.SHOW -> {
                    presenter!!.displayContent()
                }
            }
            setBrightness(maxBrightness)
        }

        revealRecogniser?.addObserver(revealRecogniserObserver)

    }

    /**
     *  Setup the Refind
     */
    private fun setRefind() {

        Glide.with(this).load(R.drawable.refind_cover).into(image)

        //mediaPullRunnable = getPullMediaThread()

        rotationRecogniserOld = RotationRecogniserOld(this)
        rotationListenerOld = object : RotationRecogniserOld.Listener {

            override fun onPickUp() {
                if(!gotData && hasConnection(applicationContext)) {
                    gotData = true
                    isGettingData = true
                    rotationRecogniserOld?.stop()
                    presenter!!.pullingData(true)
                    //PullMediaWorker.pullMediaRefind(this@MainActivity)
                    isGettingData = false
                    rotationRecogniserOld?.start(rotationListenerOld!!)
                    presenter!!.pullingData(false)
                } else {
                    presenter!!.displayContent()
                }
            }

            override fun onRotateUp() {
                presenter!!.goToNextImage()
            }

            override fun onRotateDown() {
                presenter!!.goToPreviousImage()
            }

            override fun onRotateLeft() {

            }

            override fun onRotateRight() {

            }

            override fun onStandby() {
                isGoingToStop = true
                rotationRecogniserOld!!.stop()
                rotationRecogniserOld = null
                rotationListenerOld = null
                isGettingData = false
                gotData = false
                finish()
            }

        }

    }

    private class MyAmbientCallback : AmbientModeSupport.AmbientCallback()
}