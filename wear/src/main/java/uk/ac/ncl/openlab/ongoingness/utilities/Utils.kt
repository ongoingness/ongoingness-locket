package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.*
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import android.util.Log
import androidx.work.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsWorker
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaWorker
import uk.ac.ncl.openlab.ongoingness.workers.PushLogsWorker
import java.io.*
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Miscellaneous collection of static functions.
 *
 * @author Daniel Welsh, Luis Carvalho
 */

const val minBandwidthKbps: Int = 320
const val TAG: String = "Utils"

/**
 * Clear contents of media folder.
 *
 * @param context context of the application.
 */
fun clearMediaFolder(context: Context) {
    context.filesDir.listFiles().forEach { file: File? -> file?.delete() }
}

/**
 * Deletes file.
 *
 * @param context context of the application.
 * @param filename name of the file to be deleted.
 */
fun deleteFile(context: Context, filename: String) {
    File(context.filesDir, filename).delete()
}

/**
 * Get the stored bitmap from file.
 *
 * @param context context of th application.
 * @param filename name of the file to be read.
 * @return Bitmap read from the file.
 */
fun getBitmapFromFile(context: Context,filename: String): Bitmap? {
    return try {
        val f = File(context.filesDir,filename)
        BitmapFactory.decodeStream(FileInputStream(f))
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    }
}

/**
 * Check that the activity has an active network connection.
 *
 * @param context context to check connection from.
 * @return boolean if device has connection.
 */
fun hasConnection(context: Context?): Boolean {
    // Check a network is available
    val mConnectivityManager: ConnectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: Network? = mConnectivityManager.activeNetwork

    if (activeNetwork != null) {
        val bandwidth = mConnectivityManager
                .getNetworkCapabilities(activeNetwork)
                .linkDownstreamBandwidthKbps
        if (bandwidth < minBandwidthKbps) {
            // Request a high-bandwidth network
            Log.d("OnCreate", "Request high-bandwidth network")
            return false
        }
        return true
    } else {
        return false
    }
}

/**
 * Get mac address from IPv6 address.
 *
 * @return device wifi mac address.
 */
fun getMacAddress(): String {
    try {
        val all: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (nif: NetworkInterface in all) {
            if (nif.name.toLowerCase(Locale.getDefault()) != "wlan0") continue

            val macBytes: ByteArray = nif.hardwareAddress ?: return ""

            val res1 = StringBuilder()
            for (b: Byte in macBytes) {
                res1.append(String.format("%02X", b))
            }

            return res1.toString()
        }
    } catch (ex: Exception) {
        println(ex.stackTrace)
    }
    return ""
}

/**
 * Get the battery level of the device.
 *
 * @param context context of the application.
 * @return the current battery level.
 */
fun getBatteryLevel(context: Context): Float? {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
        context.registerReceiver(null, filter)
    }

    return batteryStatus?.let { intent ->
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        level / scale.toFloat()
    }
}

/**
 * Calculates the next request time given an interval in minutes.
 *
 * @param interval the interval to the next request time in minutes.
 * @return the new request time in milliseconds.
 */
fun calculateNextMinutesRequestTimeDiff(interval : Int): Long {

    val currentTime = Calendar.getInstance()
    val dueDate = Calendar.getInstance()

    dueDate.add(Calendar.MINUTE, interval)

    return dueDate.timeInMillis - currentTime.timeInMillis;

}

/**
 * Calculates the next daily request from the current time.
 *
 * @return the new daily request time in milliseconds.
 */
fun calculateNextDailyRequestTimeDiff(): Long {

    val currentDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance()

    dueDate.set(Calendar.HOUR_OF_DAY, 1)
    dueDate.set(Calendar.MINUTE, 0)
    dueDate.set(Calendar.SECOND, 0)

    if(dueDate.before(currentDate)) {
        dueDate.add(Calendar.HOUR_OF_DAY, 24)
    }

    return dueDate.timeInMillis - currentDate.timeInMillis

}

/**
 * Creates a daily worker request to push logs and pull media from the server.
 *
 * @param context context of the application.
 */
fun addPullMediaWorkRequest(context: Context) {

    val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    val timeDiff = calculateNextDailyRequestTimeDiff()

    val dailyWorkRequest = OneTimeWorkRequestBuilder<PullMediaWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

    WorkManager.getInstance(context).enqueue(dailyWorkRequest)

}

/**
 * Creates a daily worker request to push logs from the server.
 *
 * @param context context of the application.
 */
fun addPushLogsWorkRequest(context: Context) {

    val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    val timeDiff = calculateNextDailyRequestTimeDiff()

    val dailyWorkRequest = OneTimeWorkRequestBuilder<PushLogsWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

    WorkManager.getInstance(context).enqueue(dailyWorkRequest)

}

/**
 * Creates a interval specific worker request to push logs from the server.
 *
 * @param context context of the application.
 */
fun addPullMediaPushLogsWorkRequest(context: Context) {

    val constraints = Constraints.Builder()
            .setRequiresCharging(BuildConfig.FETCH_REQUIRES_CHARGING)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    val timeDiff = calculateNextMinutesRequestTimeDiff(BuildConfig.FETCH_INTERVAL_MINUTES)

    val dailyWorkRequest = OneTimeWorkRequestBuilder<PullMediaPushLogsWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

    WorkManager.getInstance(context).enqueue(dailyWorkRequest)

}

/**
 * Create the charging cover image.
 *
 * @param battery level of battery.
 * @param screenSize size of screen.
 * @param context context of the application.
 * @return the charging cover image as a bitmap.
 */
fun getChargingBackground(battery: Float, screenSize: Int, context: Context): Bitmap {

    //First layer
    val transparent = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
    val canvasT = Canvas(transparent)
    canvasT.drawColor(Color.BLACK)

    //Second Layer
    val backgroundBitmap = when(BuildConfig.FLAVOR){
        "locket_touch", "locket_touch_inverted" -> R.drawable.flower_pattern_white
        "refind" -> R.drawable.refind_cover
        "locket_touch_s" -> R.drawable.s_cover_charging
        else -> R.drawable.flower_pattern_white
    }
    val mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, backgroundBitmap), screenSize, screenSize, false)

    //Third Layer
    val blue = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
    val canvasB = Canvas(blue)
    val circleSize = (battery * screenSize) / 2
    val circlePaint = Paint().apply {}
    canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize,  circlePaint)
    val borderPaint = Paint().apply {color = Color.parseColor("#009FE3"); style = Paint.Style.STROKE; strokeWidth = 10f }
    canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize, borderPaint)
    val alphaPaint = Paint()
    alphaPaint.alpha = 250
    alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    canvasB.drawBitmap(mBackgroundBitmap, Matrix(), alphaPaint)

    val overlay = overlayBitmaps(transparent, mBackgroundBitmap, blue, screenSize)

    return darkenBitmap(overlay, screenSize)
}

/*
fun getAnewChargingBackground(battery: Float, screenSize: Int, context: Context): Bitmap {

    //First layer
    val transparent = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
    val canvasT = Canvas(transparent)
    canvasT.drawColor(Color.BLACK)

    //Second Layer
    val mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.flower_pattern_white), screenSize, screenSize, false)

    //Third Layer
    val blue = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
    val canvasB = Canvas(blue)
    val circleSize = (battery * screenSize) / 2
    val circlePaint = Paint().apply {}
    canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize,  circlePaint)
    val borderPaint = Paint().apply {color = Color.parseColor("#009FE3"); style = Paint.Style.STROKE; strokeWidth = 10f }
    canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize, borderPaint)
    val alphaPaint = Paint()
    alphaPaint.alpha = 250
    alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    canvasB.drawBitmap(mBackgroundBitmap, Matrix(), alphaPaint)

     return overlayBitmaps(transparent, mBackgroundBitmap, blue, screenSize)
}

 */

/**
 * Darkens a given bitmap.
 *
 * @param bitmap bitmap to be darkened.
 * @param screenSize size of the bitmap in pixels.
 * @return the bitmap darkened as a bitmap.
 */
fun darkenBitmap(bitmap: Bitmap, screenSize: Int): Bitmap {
    val darkB = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
    val darkCanvas = Canvas(darkB)

    val darknessPaint = Paint().apply {}
    darknessPaint.colorFilter = LightingColorFilter(0xFF808080.toInt(), 0)

    darkCanvas.drawBitmap(bitmap, Matrix(), darknessPaint)

    return Bitmap.createScaledBitmap(darkB, screenSize, screenSize, false)
}

/**
 * Overlays bitmaps by the order of parameters.
 *
 * @param b1 bitmap at the background.
 * @param b2 bitmap in the middle.
 * @param b3 bitmap in the foreground.
 * @param screenSize size of the bitmap in pixels.
 */
fun overlayBitmaps(b1: Bitmap, b2: Bitmap, b3: Bitmap, screenSize: Int): Bitmap {

    val bmOverlay: Bitmap  = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmOverlay)

    val secondLayerAlphaPaint = Paint()
    secondLayerAlphaPaint.alpha = 80

    val alphaPaint = Paint()
    alphaPaint.alpha = 250
    alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    canvas.drawBitmap(b1, Matrix(), null)
    canvas.drawBitmap(b2, Matrix(), secondLayerAlphaPaint)
    canvas.drawBitmap(b3, Matrix(), null)

    return Bitmap.createScaledBitmap(bmOverlay, screenSize, screenSize, false)

}

/**
 * Record into the app preferences a failure of connecting to the server.
 *
 * @param context context of the application.
 */
fun recordFailure(context: Context) {
    val pref: SharedPreferences = context.getSharedPreferences(BuildConfig.ONGOINGNESS_PREFS, 0)
    val failures = pref.getInt(BuildConfig.SERVER_CONNECT_FAILURES_KEY, 0)
    with (pref.edit()) {
        putInt(BuildConfig.SERVER_CONNECT_FAILURES_KEY, failures + 1)
        apply()
    }

    val failures2 = pref.getInt(BuildConfig.SERVER_CONNECT_FAILURES_KEY, 0)
    Log.d("recordFailure", "$failures2")


    if(failures2 >  BuildConfig.MAX_FAILURES) {
        Logger.disableLogging(context)
    }

}

/**
 * Resets the failure counter in app preferences.
 *
 * @param context context of the application.
 */
fun resetFailureCounter(context: Context) {
    val pref: SharedPreferences = context.getSharedPreferences(BuildConfig.ONGOINGNESS_PREFS, 0)
    val failures = pref.getInt(BuildConfig.SERVER_CONNECT_FAILURES_KEY, 0)
    with (pref.edit()) {
        putInt(BuildConfig.SERVER_CONNECT_FAILURES_KEY, 0)
        apply()
    }

    val failures2 = pref.getInt(BuildConfig.SERVER_CONNECT_FAILURES_KEY, 0)
    Log.d("recordFailure", "$failures2")
}

/**
 * Checks if the "logging" flag in the app preferences.
 *
 * @param context context of the application.
 * @return if false the app will forever not try to connect to the server.
 */
fun isLogging(context: Context): Boolean {
    val pref: SharedPreferences = context.getSharedPreferences(BuildConfig.ONGOINGNESS_PREFS, 0)
    Log.d("isLogging", "${pref.getBoolean(BuildConfig.LOGGING_KEY, true)}")
    return pref.getBoolean(BuildConfig.LOGGING_KEY, true)
}



