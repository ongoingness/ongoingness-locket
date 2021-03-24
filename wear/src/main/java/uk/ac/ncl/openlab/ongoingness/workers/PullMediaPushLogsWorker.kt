package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import androidx.work.WorkManager
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.utilities.*
import java.lang.Exception

/**
 * Worker that pull media and pushes to the server.
 *
 * @author Luis Carvalho
 */
class PullMediaPushLogsWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {

        try {
            Logger.log(LogType.TRIED_CONNECTION, listOf(), ctx)
        } catch (e: Exception) {
            Log.e("WorkerCrash", "$e")
        }

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            ctx.registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        if(batteryPct != null && batteryPct < BuildConfig.FETCH_BATTERY_MIN_LEVEL) {
            if(isLogging(ctx))
                addPullMediaPushLogsWorkRequest(ctx)
            else
                WorkManager.getInstance(ctx).cancelAllWork()
            return Result.failure()
        }

        var pullMediaSuccess = false

        when (FLAVOR) {
            "locket_touch", "locket_touch_inverted", "locket_touch_s" -> pullMediaSuccess = AsyncHelper.pullMediaLocket(ctx)
            "refind" -> pullMediaSuccess = AsyncHelper.pullMediaRefind(ctx)
        }
        val result = if(pullMediaSuccess && AsyncHelper.pushLogs(ctx)) Result.success() else Result.failure()

        if(isLogging(ctx))
            addPullMediaPushLogsWorkRequest(ctx)
        else
            WorkManager.getInstance(ctx).cancelAllWork()

        return result

    }
}