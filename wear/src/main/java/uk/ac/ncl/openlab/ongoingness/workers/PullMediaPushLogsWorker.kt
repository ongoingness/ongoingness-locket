package uk.ac.ncl.openlab.ongoingness.workers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
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