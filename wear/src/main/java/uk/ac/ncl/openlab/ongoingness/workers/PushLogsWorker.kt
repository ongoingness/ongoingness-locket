package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import uk.ac.ncl.openlab.ongoingness.utilities.addPullMediaPushLogsWorkRequest

/**
 * Worker that pushes logs into the server.
 *
 * @author Luis Carvalho
 */
class PushLogsWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val result = if(AsyncHelper.pushLogs(ctx)) Result.success() else Result.failure()
        addPullMediaPushLogsWorkRequest(ctx)
        return result
    }

  }