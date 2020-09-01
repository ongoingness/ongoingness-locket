package uk.ac.ncl.openlab.ongoingness.workers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.database.*
import uk.ac.ncl.openlab.ongoingness.database.repositories.MediaDateRepository
import uk.ac.ncl.openlab.ongoingness.database.repositories.WatchMediaRepository
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.API
import uk.ac.ncl.openlab.ongoingness.utilities.addPullMediaPushLogsWorkRequest
import uk.ac.ncl.openlab.ongoingness.utilities.addPullMediaWorkRequest
import uk.ac.ncl.openlab.ongoingness.utilities.deleteFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.sql.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log

class PullMediaPushLogsWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {

        Firebase.remoteConfig.fetchAndActivate()

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            ctx.registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        if(batteryPct != null && batteryPct < Firebase.remoteConfig.getDouble("FETCH_BATTERY_MIN_LEVEL")) return Result.failure()

        var pullMediaSuccess = false

        when (FLAVOR) {
            "locket_touch", "locket_touch_inverted" -> pullMediaSuccess = AsyncHelper.pullMediaLocket(ctx)
            "refind" -> pullMediaSuccess = AsyncHelper.pullMediaRefind(ctx)
        }
        val result = if(pullMediaSuccess && AsyncHelper.pushLogs(ctx)) Result.success() else Result.failure()
        addPullMediaPushLogsWorkRequest(ctx)
        return result

    }
}