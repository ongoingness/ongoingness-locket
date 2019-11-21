package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.ResponseBody
import uk.ac.ncl.openlab.ongoingness.utilities.API
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.addPullMediaPushLogsWorkRequest
import uk.ac.ncl.openlab.ongoingness.utilities.addPushLogsWorkRequest
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PushLogsWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val result = if(AsyncHelper.pushLogs(ctx)) Result.success() else Result.failure()
        addPullMediaPushLogsWorkRequest(ctx)
        return result
    }

  }