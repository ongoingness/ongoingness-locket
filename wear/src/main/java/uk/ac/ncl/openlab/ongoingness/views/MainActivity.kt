package uk.ac.ncl.openlab.ongoingness.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.recognisers.RevealRecogniser
import uk.ac.ncl.openlab.ongoingness.recognisers.RotationRecogniser
import uk.ac.ncl.openlab.ongoingness.recognisers.TouchRevealRecogniser
import uk.ac.ncl.openlab.ongoingness.utilities.*
import uk.ac.ncl.openlab.ongoingness.viewmodel.MainPresenter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Observer

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider, MainPresenter.View {

    private var presenter: MainPresenter?  = null
    private var isReady: Boolean = false
    private val maxBrightness: Float = 1.0f
    private val minBrightness: Float = 0.01f

    private var revealRecogniser:RevealRecogniser? = null
    private var revealRecogniserObserver: Observer? = null

    private var touchRevealRecogniser: TouchRevealRecogniser? = null
    private var touchRevealRecogniserObserver: Observer?  = null

    private var rotationRecogniser: RotationRecogniser? = null
    private var rotationListener: RotationRecogniser.Listener? = null

    var isGoingToStop: Boolean = false

    private var isGettingData = false
    private var gotData = false

    private lateinit var mediaPullRunnable : kotlinx.coroutines.Runnable

    lateinit var mImageView: ImageView
    lateinit var gesture : GestureDetector
    lateinit var touchListener: View.OnTouchListener

    private lateinit var bitmapReceiver: BroadcastReceiver

    private lateinit var killRunnable: Runnable

    private val killHandler = Handler()
    private var startTime = System.currentTimeMillis()
    private val timeCheckInterval = 30 * 1000L //30 seconds
    private val killDelta = 5 * 60 * 1000L //5 minutes

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

        killRunnable!!.run()

        presenter = MainPresenter()
        presenter!!.attachView(this)
        presenter!!.setContext(applicationContext)
        presenter!!.setWatchMediaRepository(this)

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        when(FLAVOR){
            "locket_touch" -> setLocketTouch()
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
            "locket_touch" -> { touchRevealRecogniser?.start() }
            "refind" -> { rotationRecogniser?.start(rotationListener!!) }
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
                "locket_touch" -> {
                   touchRevealRecogniser?.stop()
                }
                "refind" -> {
                    rotationRecogniser?.stop()
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
            "locket_touch" -> {
                touchRevealRecogniser?.deleteObserver(touchRevealRecogniserObserver)
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

        Glide.with(this).load(R.drawable.cover).into(image)

        //Check if is charging
        if(intent.hasExtra("background")) {
            var bitmap = BitmapFactory.decodeByteArray(
                    intent.getByteArrayExtra("background"), 0,
                    intent.getByteArrayExtra("background").size)
            presenter!!.updateCoverBitmap(bitmap)
        }

        //Charging background receiver
        bitmapReceiver = object: BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                Log.d("NewBitMap", "$intent")

                if(intent.hasExtra("background")) {
                    var bitmap = BitmapFactory.decodeByteArray(
                            intent.getByteArrayExtra("background"),0,
                            intent.getByteArrayExtra("background").size)

                    presenter!!.updateCoverBitmap(bitmap)

                }
            }
        }
        val filter = IntentFilter("BATTERY_INFO").apply {}
        registerReceiver(bitmapReceiver, filter)

        //Thread to pull new media from the server
        //mediaPullRunnable = getPullMediaThread()

        touchRevealRecogniser = TouchRevealRecogniser(this)
        touchRevealRecogniserObserver = Observer { _, arg ->
            when (arg) {
                TouchRevealRecogniser.Events.STARTED -> {

                }

                TouchRevealRecogniser.Events.AWAKE -> {

                    //Stop Activity Kill Thread
                    killHandler.removeCallbacks(killRunnable)
/*
                    if(!gotData && hasConnection(applicationContext)) {
                        mImageView.setOnTouchListener(null)
                        gotData = true
                        isGettingData = true
                        presenter!!.pullingData(true)
                        mediaPullRunnable.run()*/

                    //} else {
                        presenter!!.restartIndex()
                        presenter!!.displayContent()
                    //}

                    Logger.log(LogType.WAKE_UP, listOf(), applicationContext)
                }
                TouchRevealRecogniser.Events.NEXT -> {
                    presenter!!.goToNextImage()

                }
                TouchRevealRecogniser.Events.SLEEP -> {


                    var layoutParams : WindowManager.LayoutParams = this.window.attributes
                    layoutParams.dimAmount = 0.75f
                    layoutParams.screenBrightness = 0.1f

                    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    window.attributes = layoutParams

                    windowManager.updateViewLayout(window.decorView, layoutParams);

                    //Start Activity Kill Thread
                    killRunnable.run()

                    presenter!!.hideContent(MainPresenter.CoverType.BLACK)
                    startTime = System.currentTimeMillis()
                    Logger.log(LogType.SLEEP, listOf(), applicationContext)

                }
                TouchRevealRecogniser.Events.STOPPED -> {
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

        touchRevealRecogniser?.addObserver(touchRevealRecogniserObserver)

        gesture = GestureDetector(this, touchRevealRecogniser)
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

        mediaPullRunnable = getPullMediaThread()

        rotationRecogniser = RotationRecogniser(this)
        rotationListener = object : RotationRecogniser.Listener {

            override fun onPickUp() {
                if(!gotData && hasConnection(applicationContext)) {
                    gotData = true
                    isGettingData = true
                    rotationRecogniser?.stop()
                    presenter!!.pullingData(true)
                    mediaPullRunnable.run()
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
                rotationRecogniser!!.stop()
                rotationRecogniser = null
                rotationListener = null
                isGettingData = false
                gotData = false
                finish()
            }

        }

    }


    /**
     * Creates the thread to pull new media from the server
     */
    private fun getPullMediaThread(): kotlinx.coroutines.Runnable {
        return kotlinx.coroutines.Runnable {
            val api = API()

            val watchMediaDao = WatchMediaRoomDatabase.getDatabase(this.applicationContext).watchMediaDao()
            val repository = WatchMediaRepository(watchMediaDao)

            val context = this.applicationContext
            val filesDir = this.applicationContext.filesDir


            when(FLAVOR) {
                "locket_touch" -> {

                    GlobalScope.launch {
                        var mediaList = repository.getAll().sortedWith(compareBy({it.collection}, {it.order}))



                        api.fetchMediaPayload (

                            callback = { response ->

                                Log.d("sa", "nice")

                            var stringResponse = response!!.body()?.string()
                            val jsonResponse = JSONObject(stringResponse)

                            Log.d("sa", stringResponse +  jsonResponse.getString("code"))

                            var code = jsonResponse.getString("code")

                            if (code.startsWith('5')) {
                                isGettingData = false
                                mImageView.setOnTouchListener(touchListener)
                                presenter!!.pullingData(false)
                                if(mediaList.isEmpty())
                                    presenter!!.hideContent(MainPresenter.CoverType.BLACK)
                            } else if (code.startsWith('2')) {

                                var payload: JSONArray = jsonResponse.getJSONArray("payload")

                                var toBeRemoved = mediaList.toTypedArray().copyOf().toMutableList()

                                var mediaFetch = 0

                                if (payload.length() > 0) {

                                    for(i in 0 until payload.length()) {
                                        val media:JSONObject = payload.getJSONObject(i)
                                        val newMedia = WatchMedia(media.getString("_id"),
                                                media.getString("path"),
                                                media.getString("locket"),
                                                media.getString("mimetype"),
                                                i)

                                        if(mediaList.contains(newMedia)) {
                                            toBeRemoved.remove(newMedia)
                                            mediaFetch++

                                            if(mediaFetch == payload.length()) {
                                                for(mediaItem in toBeRemoved) {
                                                    GlobalScope.launch {
                                                        repository.delete(mediaItem._id)
                                                        deleteFile(context, mediaItem.path)
                                                    }
                                                }
                                                mImageView.setOnTouchListener(touchListener)
                                                presenter!!.pullingData(false)
                                            }
                                        } else {
                                            var gotFile = false
                                            while(!gotFile) {
                                                try {
                                                    api.fetchBitmap(newMedia._id) { body ->

                                                        val inputStream = body?.byteStream()
                                                        val file = File(filesDir, newMedia.path)
                                                        lateinit var outputStream: OutputStream

                                                        //try {
                                                            outputStream = FileOutputStream(file)
                                                            if(newMedia.mimetype.contains("video") ||
                                                                    newMedia.mimetype.contains("gif")) {
                                                                inputStream.use { input ->
                                                                    outputStream.use { fileOut ->
                                                                        input!!.copyTo(fileOut)
                                                                    }
                                                                }
                                                            } else {
                                                                val image = BitmapFactory.decodeStream(inputStream)
                                                                image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                                            }
                                                            outputStream.flush()
                                                            mediaFetch++

                                                        //} catch (e: IOException) {
                                                            //throw Exception("File Fetching - Something went wrong")
                                                        //} finally {
                                                            outputStream.close()
                                                            inputStream?.close()
                                                            GlobalScope.launch {
                                                                repository.insert(newMedia)
                                                                if(mediaFetch == payload.length()) {
                                                                    for(mediaItem in toBeRemoved) {
                                                                        repository.delete(mediaItem._id)
                                                                        deleteFile(context, mediaItem.path)
                                                                    }
                                                                    mImageView.setOnTouchListener(touchListener)
                                                                    presenter!!.pullingData(false)
                                                                }
                                                            }
                                                        //}
                                                    }
                                                    gotFile = true
                                                } catch(e: Exception) {
                                                    Log.d("FetchFile", "Fail")
                                                    gotFile = false
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if(toBeRemoved.isNotEmpty()) {
                                        GlobalScope.launch {

                                            for(media in toBeRemoved) {
                                                repository.delete(media._id)
                                                deleteFile(context, media.path)
                                            }
                                            presenter!!.pullingData(false)
                                            presenter!!.hideContent(MainPresenter.CoverType.BLACK)
                                        }

                                    } else {
                                        presenter!!.pullingData(false)
                                        presenter!!.hideContent(MainPresenter.CoverType.BLACK)
                                    }

                                    mImageView.setOnTouchListener(touchListener)
                                }
                            }
                        },
                        failure = { e ->

                            Log.d("sa", "fail $e")

                            isGettingData = false
                            mImageView.setOnTouchListener(touchListener)
                            presenter!!.pullingData(false)
                        })
                    }
                }

                "refind" -> {
                    GlobalScope.launch {
                        val mediaList = repository.getAll().sortedBy { it.order }
                        val currentImageID: String
                        currentImageID = if (mediaList.isNullOrEmpty()) {
                            "test"
                        } else {
                            mediaList[0]._id
                        }
                        api.fetchInferredMedia(currentImageID) { response ->

                            val stringResponse = response!!.body()?.string()

                            if (stringResponse == "[]") {
                                isGettingData = false
                                rotationRecogniser?.start(rotationListener!!)
                                presenter!!.pullingData(false)
                            } else {

                                for (mediaToRemove in mediaList) {
                                    deleteFile(context, mediaToRemove.path)
                                }

                                repository.deleteAll()
                                val jsonResponse = JSONObject(stringResponse)

                                val payload: JSONArray = jsonResponse.getJSONArray("payload")
                                if (payload.length() > 0) {

                                    //Set present Image
                                    val presentImage: JSONObject = payload.getJSONObject(0)
                                    val newWatchMedia = WatchMedia(presentImage.getString("_id"),
                                            presentImage.getString("path"),
                                            presentImage.getString("locket"),
                                            presentImage.getString("mimetype"),
                                            0)

                                    api.fetchBitmap(newWatchMedia._id) { body ->
                                        val inputStream = body?.byteStream()
                                        val image = BitmapFactory.decodeStream(inputStream)
                                        val file = File(filesDir, newWatchMedia.path)
                                        lateinit var stream: OutputStream
                                        try {
                                            stream = FileOutputStream(file)
                                            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                                            stream.flush()
                                        } catch (e: IOException) { // Catch the exception
                                            e.printStackTrace()
                                        } finally {
                                            stream.close()
                                            inputStream?.close()
                                            GlobalScope.launch {
                                                repository.insert(newWatchMedia)
                                            }
                                        }
                                    }

                                    val pastImages = mutableListOf<WatchMedia>()
                                    for (i in 1..5) {
                                        try {
                                            val pastImage: JSONObject = payload.getJSONObject(i)
                                            pastImages.add(WatchMedia(pastImage.getString("id"),
                                                    pastImage.getString("path"),
                                                    "past",
                                                    pastImage.getString("mimetype"),
                                                     i))
                                        } catch (e: java.lang.Exception) {
                                           e.printStackTrace()
                                        }
                                    }

                                    var imageCounter = 0
                                    for (media: WatchMedia in pastImages) {
                                        api.fetchBitmap(media._id) { body ->
                                            val inputStream = body?.byteStream()
                                            val image = BitmapFactory.decodeStream(inputStream)
                                            val file = File(filesDir, media.path)
                                            lateinit var stream: OutputStream
                                            try {
                                                stream = FileOutputStream(file)
                                                image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                                                stream.flush()
                                            } catch (e: IOException) { // Catch the exception
                                                e.printStackTrace()
                                            } finally {
                                                inputStream?.close()
                                                imageCounter += 1
                                                stream.close()
                                                GlobalScope.launch {
                                                    repository.insert(media)
                                                    if (imageCounter == pastImages.size) {
                                                        rotationRecogniser?.start(rotationListener!!)
                                                        presenter!!.pullingData(false)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class MyAmbientCallback : AmbientModeSupport.AmbientCallback()
}