package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import uk.ac.ncl.openlab.ongoingness.utilities.API
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.addPushLogsWorkRequest
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PushLogsWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        return pushLogs(API())
    }

    private fun pushLogs(api: API): Result {

        return runBlocking {

            val result = suspendCoroutine<Result> { cont ->

                val callback = { _: Response? ->
                    Logger.clearLogs()
                    addPushLogsWorkRequest(applicationContext)
                    cont.resume(Result.success())
                }

                val failure = { _: IOException ->
                    cont.resume(Result.failure())
                }
                api.fetchMediaPayload(callback, failure)
            }
            result
        }

    }

  }