package uk.ac.ncl.openlab.ongoingness.utilities

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
import org.json.JSONObject
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PushLogsWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        return pushLogs(API())
    }

    private fun pushLogs(api: API): Result {

        return runBlocking {

            var result = suspendCoroutine<Result> { cont ->

                val callback = { response: Response? ->
                    Logger.clearLogs()
                    addPushLogsWorkRequest(applicationContext)
                    cont.resume(Result.success())
                }

                val failure = { e: IOException ->
                    cont.resume(Result.failure())
                }
                api.fetchMediaPayload(callback, failure)
            }
            result
        }

    }

  }