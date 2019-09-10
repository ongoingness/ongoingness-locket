package uk.ac.ncl.openlab.ongoingness.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.nfc.Tag
import android.os.*
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.view.GestureDetectorCompat
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
import java.time.LocalDateTime

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider, MainPresenter.View {

    private var presenter: MainPresenter?  = null
    private var isReady: Boolean = false
    private val maxBrightness: Float = 1.0f
    private val minBrightness: Float = 0.01f

    private var revealRecogniser:RevealRecogniser? = null
    private var revealRecogniserObserver: Observer? = null

    private var touchRevealRecogniser: TouchRevealRecogniser? = null
    private var touchRevealRecogniserObserver: Observer?  = null

    private var vibrator: Vibrator? = null

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
    private val timeCheckInterval = 1000L
    private val killDelta = 5 * 60 * 1000L //5 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        killRunnable = Runnable {
            if(System.currentTimeMillis() - startTime > killDelta)
                finish()
            else
                killHandler.postDelayed(killRunnable, timeCheckInterval)
        }

        killRunnable!!.run()


        // Enables Always-on
        //setAmbientEnabled()
        presenter = MainPresenter()
        presenter!!.attachView(this)
        presenter!!.setContext(applicationContext)

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        when(FLAVOR){
            "locket_touch" -> {
                presenter!!.setWatchMediaRepository(this)
                Glide.with(this).load(R.drawable.cover).into(image)
                if(intent.hasExtra("background")) {
                    var bitmap = BitmapFactory.decodeByteArray(
                            intent.getByteArrayExtra("background"), 0,
                            intent.getByteArrayExtra("background").size)

                    presenter!!.updateCoverBitmap(bitmap)

                }

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

                setLocketTouch()
            }
            "locket" -> {
                presenter!!.setWatchMediaRepository(this)
                Glide.with(this).load(R.drawable.cover).into(image)
                setLocket()
            }
            "refind" -> {
                presenter!!.setWatchMediaRepository(this)
                setRefind()
                Glide.with(this).load(R.drawable.refind_cover).into(image)
            }
        }


        /* Thread that pulls media from the server*/
        mediaPullRunnable = kotlinx.coroutines.Runnable {

            when (FLAVOR) {
                "locket" -> {

                }

                "locket_touch" -> {

                    writeToLogFile("Locket touch. Checking media.")
                    val api = API()

                    val watchMediaDao = WatchMediaRoomDatabase.getDatabase(this.applicationContext).watchMediaDao()
                    val repository = WatchMediaRepository(watchMediaDao);

                    val context = this.applicationContext
                    val filesDir = this.applicationContext.filesDir

                    GlobalScope.launch {
                        var mediaList = repository.getAll().sortedBy { it.order }
                        var currentImageID: String
                        currentImageID = if (mediaList.isNullOrEmpty()) {
                            "test"
                        } else {
                            mediaList[0]._id
                        }
                        api.fetchInferredMedia(currentImageID) { response ->

                            var stringResponse = response!!.body()?.string()

                            if (stringResponse == "[]") {
                                isGettingData = false
                                rotationRecogniser?.start(rotationListener!!)
                                mImageView.setOnTouchListener(touchListener)
                                presenter!!.pullingData(false)
                            } else {

                                for (mediaToRemove in mediaList) {
                                    deleteFile(context, mediaToRemove.path)
                                }

                                repository.deleteAll()
                                val jsonResponse = JSONObject(stringResponse)

                                var payload: JSONArray = jsonResponse.getJSONArray("payload")
                                if (payload.length() > 0) {

                                    //Set present Image
                                    var presentImage: JSONObject = payload.getJSONObject(0)
                                    var newWatchMedia = WatchMedia(presentImage.getString("_id"),
                                            presentImage.getString("path"),
                                            presentImage.getString("locket"),
                                            presentImage.getString("mimetype"), 0)

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
                                            Log.d("Utils", "stored image: ${file.absolutePath}")
                                            GlobalScope.launch {
                                                repository.insert(newWatchMedia)
                                            }
                                        }
                                    }

                                    var pastImages = mutableListOf<WatchMedia>()
                                    for (i in 1..5) {
                                        try {
                                            var pastImage: JSONObject = payload.getJSONObject(i)
                                            pastImages.add(WatchMedia(pastImage.getString("id"),
                                                    pastImage.getString("path"),
                                                    "past",
                                                    pastImage.getString("mimetype"), i))
                                        } catch (e: java.lang.Exception) {
                                            Log.d("EXCEPTION", "GOT a NULL???")
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
                                                Log.d("Utils", "stored image: ${file.absolutePath}")
                                                GlobalScope.launch {
                                                    repository.insert(media)
                                                    if (imageCounter == pastImages.size) {
                                                        Log.d("WHAT", "hummmm")
                                                        //touchRevealRecogniser!!.ignoreTouch(false)
                                                        mImageView.setOnTouchListener(touchListener)
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

                "refind" -> {

                    val api = API()

                    val watchMediaDao = WatchMediaRoomDatabase.getDatabase(this.applicationContext).watchMediaDao()
                    val repository = WatchMediaRepository(watchMediaDao);

                    val context = this.applicationContext
                    val filesDir = this.applicationContext.filesDir

                    GlobalScope.launch {
                        var mediaList = repository.getAll().sortedBy { it.order }
                        var currentImageID: String
                        currentImageID = if (mediaList.isNullOrEmpty()) {
                            "test"
                        } else {
                            mediaList[0]._id
                        }
                        api.fetchInferredMedia(currentImageID) { response ->

                            var stringResponse = response!!.body()?.string()

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

                                var payload: JSONArray = jsonResponse.getJSONArray("payload")
                                if (payload.length() > 0) {

                                    //Set present Image
                                    var presentImage: JSONObject = payload.getJSONObject(0)
                                    var newWatchMedia = WatchMedia(presentImage.getString("_id"),
                                            presentImage.getString("path"),
                                            presentImage.getString("locket"),
                                            presentImage.getString("mimetype"), 0)

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
                                            Log.d("Utils", "stored image: ${file.absolutePath}")
                                            GlobalScope.launch {
                                                repository.insert(newWatchMedia)
                                            }
                                        }
                                    }

                                    var pastImages = mutableListOf<WatchMedia>()
                                    for (i in 1..5) {
                                        try {
                                            var pastImage: JSONObject = payload.getJSONObject(i)
                                            pastImages.add(WatchMedia(pastImage.getString("id"),
                                                    pastImage.getString("path"),
                                                    "past",
                                                    pastImage.getString("mimetype"), i))
                                        } catch (e: java.lang.Exception) {
                                            Log.d("EXCEPTION", "GOT a NULL???")
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
                                                Log.d("Utils", "stored image: ${file.absolutePath}")
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

        var closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        applicationContext.sendBroadcast(closeIntent)


        Log.d("THTHTH", "close dialog please")


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
            "refind" -> {
            }
        }

        presenter!!.detachView()

       var closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        applicationContext.sendBroadcast(closeIntent);
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


    private fun setLocketTouch() {

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        touchRevealRecogniser = TouchRevealRecogniser(this)

        touchRevealRecogniserObserver = Observer { _, arg ->
            when (arg) {
                TouchRevealRecogniser.Events.STARTED -> {

                }

                TouchRevealRecogniser.Events.AWAKE -> {
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                    if(!gotData && hasConnection(applicationContext)) {
                        mImageView.setOnTouchListener(null)
                        gotData = true
                        isGettingData = true
                        presenter!!.pullingData(true)
                        mediaPullRunnable.run()
                    } else {

                        presenter!!.displayContent()
                    }

                }
                TouchRevealRecogniser.Events.NEXT -> {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    presenter!!.goToNextImage()

                }
                TouchRevealRecogniser.Events.SLEEP -> {
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                    presenter!!.hideContent(1)
                    startTime = System.currentTimeMillis()

                }
                TouchRevealRecogniser.Events.STOPPED -> {
                    isGoingToStop = true
                    finish()
                }
            }
            setBrightness(maxBrightness)
        }

        touchRevealRecogniser?.addObserver(touchRevealRecogniserObserver)


        gesture = GestureDetector(MainActivity@this, touchRevealRecogniser)
        touchListener = View.OnTouchListener(){
            v,events -> gesture.onTouchEvent(events)
        }
        mImageView = findViewById(R.id.image)
        mImageView.setOnTouchListener(touchListener)


    }

    private fun setLocket() {

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator;

        revealRecogniser = RevealRecogniser(this)

        revealRecogniserObserver = Observer { _, arg ->
            when (arg) {
                RevealRecogniser.Events.STARTED -> {

                }
                RevealRecogniser.Events.COVERED -> {

                }
                RevealRecogniser.Events.AWAKE -> {
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                    presenter!!.displayContent()

                }
                RevealRecogniser.Events.SHORT_REVEAL -> {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    presenter!!.goToNextImage()

                }
                RevealRecogniser.Events.SLEEP -> {
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                    presenter!!.hideContent(1)

                }
                RevealRecogniser.Events.STOPPED -> {
                    isGoingToStop = true
                    finish()
                }
                RevealRecogniser.Events.HIDE -> {
                    presenter!!.hideContent(1)
                }
                RevealRecogniser.Events.SHOW -> {
                    presenter!!.displayContent()
                }
            }
            setBrightness(maxBrightness)
        }

        revealRecogniser?.addObserver(revealRecogniserObserver)
    }

    private fun setRefind() {

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

    override fun getContext(): Context {
        return this.applicationContext
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return MyAmbientCallback()
    }

    /*
    * General function for writing appending message to log file.
     */
    private fun writeToLogFile(message: String) {
        var filePath = getFilesDir().getPath().toString() + "/locket_log_file.txt";
        var f = File(filePath);
        Log.d("Log File", "Log Path: " + filePath)
        //var batteryLevel = getBatteryLevel()
        //Log.d("Battery check", batteryLevel.toString())

        //f.appendText(LocalDateTime.now().toString() + ": " + message + " Battery level: " + batteryLevel + "\n")
    }

    /*
    * Function for sending log file contents to API and clearing lofile
    */
    //TODO
    private fun uploadLogFileContents() {
    }

    /*
    * Function returns current battery status.
     */
    /*
    private fun getBatteryLevel(): Float? {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        }

        return batteryPct
    }
    */

    // TODO
    private class MyAmbientCallback : AmbientModeSupport.AmbientCallback() {

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
        }
    }



}