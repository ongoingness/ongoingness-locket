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

class PullMediaWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    var context = ctx

    override fun doWork(): Result {

        when (FLAVOR) {
            "locket_touch" -> {
                return if(pullMediaLocket(context)) Result.success() else Result.failure()
            }

            "refind" -> {
                return if(pullMediaRefind(context)) Result.success() else Result.failure()
            }
        }

        return Result.failure()
    }

    companion object {

        @SuppressLint("SimpleDateFormat")
        fun pullMediaLocket(context: Context): Boolean {

            return runBlocking {

                val result = suspendCoroutine<Boolean> { cont ->

                    val api = API()

                    val watchMediaDao = WatchMediaRoomDatabase.getDatabase(context).watchMediaDao()
                    val watchMediaRepository = WatchMediaRepository(watchMediaDao)

                    val mediaDateDao = WatchMediaRoomDatabase.getDatabase(context).mediaDateDao()
                    val mediaDateRepository = MediaDateRepository(mediaDateDao)

                    val mediaList = watchMediaRepository.getAll().sortedWith(compareBy({ it.collection }, { it.order }))

                    val callback = { response: Response? ->

                        val stringResponse = response!!.body()?.string()
                        val jsonResponse = JSONObject(stringResponse)

                        val code = jsonResponse.getString("code")

                        if (code.startsWith('2')) {

                            val payload: JSONArray = jsonResponse.getJSONArray("payload")

                            val toBeRemoved = mediaList.toTypedArray().copyOf().toMutableList()

                            var mediaFetch = 0

                            if (payload.length() > 0) {

                                for (i in 0 until payload.length()) {
                                    val media: JSONObject = payload.getJSONObject(i)
                                    val newMedia = WatchMedia(media.getString("_id"),
                                            media.getString("path"),
                                            media.getString("locket"),
                                            media.getString("mimetype"),
                                            i,
                                            Date(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(media.getString("createdAt")).time))

                                    val times = media.getJSONArray("times")

                                    if (mediaList.contains(newMedia)) {
                                        toBeRemoved.remove(newMedia)
                                        mediaFetch++

                                        if (mediaFetch == payload.length()) {
                                            for (m in toBeRemoved) {
                                                GlobalScope.launch {
                                                    watchMediaRepository.delete(m._id)
                                                    deleteFile(context, m.path)
                                                }
                                            }
                                            cont.resume(true)
                                        }
                                    } else {
                                        api.fetchBitmap(newMedia._id) { body ->

                                            val inputStream = body?.byteStream()
                                            val file = File(context.filesDir, newMedia.path)
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
                                                    watchMediaRepository.insert(newMedia)
                                                    addMediaDates(mediaDateRepository, newMedia._id, times)
                                                    if (mediaFetch == payload.length()) {
                                                        for (m in toBeRemoved) {
                                                            watchMediaRepository.delete(m._id)
                                                            deleteFile(context, m.path)
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
                                            watchMediaRepository.delete(media._id)
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
        fun pullMediaRefind(context: Context): Boolean {

            val api = API()

            val watchMediaDao = WatchMediaRoomDatabase.getDatabase(context).watchMediaDao()
            val repository = WatchMediaRepository(watchMediaDao)

            var result = false

            val mediaList = repository.getAll().sortedBy { it.order }
            val currentImageID: String
            currentImageID = if (mediaList.isNullOrEmpty()) {
                "test"
            } else {
                mediaList[0]._id
            }
            api.fetchInferredMedia(currentImageID) { response ->

                val stringResponse = response!!.body()?.string()

                if (stringResponse != "[]") {

                    for (mediaToRemove in mediaList) {
                        deleteFile(context, mediaToRemove.path)
                    }
                    repository.deleteAll()

                    val jsonResponse = JSONObject(stringResponse)

                    val payload: JSONArray = jsonResponse.getJSONArray("payload")
                    if (payload.length() > 0) {


                        //Set present Image
                        val presentImage: JSONObject = payload.getJSONObject(0)


                        val  newWatchMedia = WatchMedia(presentImage.getString("_id"),
                                    presentImage.getString("path"),
                                    presentImage.getString("locket"),
                                    presentImage.getString("mimetype"),
                                    0,
                                    Date(System.currentTimeMillis())) //fixme check the name from the api json response
                        
                            api.fetchBitmap(newWatchMedia._id) { body ->
                                val inputStream = body?.byteStream()
                                val image = BitmapFactory.decodeStream(inputStream)
                                val file = File(context.filesDir, newWatchMedia.path)
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
                                val pastImage: JSONObject = payload.getJSONObject(i)
                                pastImages.add(WatchMedia(pastImage.getString("id"),
                                        pastImage.getString("path"),
                                        "past",
                                        pastImage.getString("mimetype"),
                                        i,
                                        Date(System.currentTimeMillis()))) //fixme check the name from the api json response
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }

                        var imageCounter = 0
                        for (media: WatchMedia in pastImages) {
                            api.fetchBitmap(media._id) { body ->
                                val inputStream = body?.byteStream()
                                val image = BitmapFactory.decodeStream(inputStream)
                                val file = File(context.filesDir, media.path)
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

        suspend fun addMediaDates(repository: MediaDateRepository, mediaId:String, times:JSONArray){
            for (j in 0 until times.length()){
                val item = times[j] as Long?
                val newMediaDate = MediaDate(MediaDate.longToDate(item!!), mediaId)
                repository.insert(newMediaDate)
            }
        }
    }
}