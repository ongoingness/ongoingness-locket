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

class PullMediaWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    var context = ctx

    override fun doWork(): Result {

        val api = API()

        val watchMediaDao = WatchMediaRoomDatabase.getDatabase(context).watchMediaDao()
        val repository = WatchMediaRepository(watchMediaDao)

        val filesDir = context.filesDir

        when (BuildConfig.FLAVOR) {
            "locket_touch" -> {
                if(pullMediaLocket(repository, api, filesDir)) {
                    addPullMediaWorkRequest(context)
                    return Result.success()
                } else
                    return Result.failure()
            }

            "refind" -> {
                return if(pullMediaRefind(context, repository, api, filesDir)) Result.success() else Result.failure()
            }
        }

        return Result.failure()
    }

    private fun pullMediaLocket(repository: WatchMediaRepository, api: API, filesDir: File): Boolean {

       return runBlocking {

            var result = suspendCoroutine<Boolean> { cont ->

                var mediaList = repository.getAll().sortedWith(compareBy({ it.collection }, { it.order }))
                val callback = { response: Response? ->

                    var stringResponse = response!!.body()?.string()
                    val jsonResponse = JSONObject(stringResponse)

                    var code = jsonResponse.getString("code")

                    if (code.startsWith('2')) {

                        var payload: JSONArray = jsonResponse.getJSONArray("payload")

                        var toBeRemoved = mediaList.toTypedArray().copyOf().toMutableList()

                        var mediaFetch = 0

                        if (payload.length() > 0) {

                            for (i in 0 until payload.length()) {
                                var media: JSONObject = payload.getJSONObject(i)
                                var newMedia = WatchMedia(media.getString("_id"),
                                        media.getString("path"),
                                        media.getString("locket"),
                                        media.getString("mimetype"), i)

                                if (mediaList.contains(newMedia)) {
                                    toBeRemoved.remove(newMedia)
                                    mediaFetch++

                                    if (mediaFetch == payload.length()) {
                                        for (media in toBeRemoved) {
                                            GlobalScope.launch {
                                                repository.delete(media._id)
                                                deleteFile(context, media.path)
                                            }
                                        }
                                        cont.resume(true)
                                    }
                                } else {
                                    api.fetchBitmap(newMedia._id) { body ->

                                        val inputStream = body?.byteStream()
                                        val file = File(filesDir, newMedia.path)
                                        lateinit var outputStream: OutputStream

                                        outputStream = FileOutputStream(file)
                                        if (newMedia.mimetype.contains("video") || newMedia.mimetype.contains("gif")) {

                                            try {
                                                inputStream.use { input ->
                                                    outputStream.use { fileOut ->
                                                        input!!.copyTo(fileOut)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.d("InputStream", "$e")
                                            }

                                        } else {
                                            val image = BitmapFactory.decodeStream(inputStream)
                                            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                        }
                                        outputStream.flush()
                                        outputStream.close()
                                        inputStream?.close()
                                        mediaFetch++

                                        runBlocking {

                                            val job = GlobalScope.launch {
                                                repository.insert(newMedia)
                                                if (mediaFetch == payload.length()) {
                                                    for (media in toBeRemoved) {
                                                        repository.delete(media._id)
                                                        deleteFile(context, media.path)
                                                    }
                                                    cont.resume(true)
                                                }
                                            }
                                            job.join()
                                        }
                                    }
                                }
                            }
                        } else {
                            if (toBeRemoved.isNotEmpty()) {
                                GlobalScope.launch {
                                    for (media in toBeRemoved) {
                                        repository.delete(media._id)
                                        deleteFile(context, media.path)
                                    }
                                    cont.resume(true)
                                }
                            }
                        }

                    }
                }

                val failure = { e: java.lang.Exception ->
                    Log.d("Error", "$e")
                    cont.resume(false)
                }

                api.fetchMediaPayload(callback, failure)
            }
            result
        }
    }

    //TODO
    // Probably not working
    private fun pullMediaRefind(context: Context, repository: WatchMediaRepository, api: API, filesDir: File): Boolean {

        var result = false

        var mediaList = repository.getAll().sortedBy { it.order }
        var currentImageID: String
        currentImageID = if (mediaList.isNullOrEmpty()) {
            "test"
        } else {
            mediaList[0]._id
        }
        api.fetchInferredMedia(currentImageID) { response ->

            var stringResponse = response!!.body()?.string()

            if (stringResponse != "[]") {

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
                            GlobalScope.launch {
                                repository.insert(newWatchMedia)
                            }
                        }
                    }

                    val pastImages = mutableListOf<WatchMedia>()
                    for (i in 1..5) {
                        try {
                            var pastImage: JSONObject = payload.getJSONObject(i)
                            pastImages.add(WatchMedia(pastImage.getString("id"),
                                    pastImage.getString("path"),
                                    "past",
                                    pastImage.getString("mimetype"), i))
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
                                }
                            }
                        }
                    }
                    result = true
                }
            }
        }

        return result
    }
}