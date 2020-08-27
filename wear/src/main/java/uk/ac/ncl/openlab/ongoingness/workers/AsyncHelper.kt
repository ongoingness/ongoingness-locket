package uk.ac.ncl.openlab.ongoingness.workers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Movie
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaRoomDatabase
import uk.ac.ncl.openlab.ongoingness.database.repositories.MediaDateRepository
import uk.ac.ncl.openlab.ongoingness.database.repositories.WatchMediaRepository
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.API
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.deleteFile
import java.io.*
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.MonthDay
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AsyncHelper {

    companion object {
/*
        fun fetchRemoteConfig(context: Context) : Boolean {

            return runBlocking {

                val result = suspendCoroutine<Boolean> { cont ->

                    val callback = { _: Task<Boolean> ->

                        cont.resume(true)
                    }

                    val failure = { _: IOException ->
                        cont.resume(false)
                    }

                    Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener(context., callback)
                }
                result
            }


            Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener(contexts[0]!!) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("ss", "Config params updated: $updated")

                } else {

                }

            }

        }
 */
        fun pushLogs(context: Context): Boolean {

            return runBlocking {

                val result = suspendCoroutine<Boolean> { cont ->

                    val callback = { _: ResponseBody? ->
                        Logger.clearLogs()
                        Logger.log(LogType.PUSHED_LOGS, listOf("success:true"), context)
                        cont.resume(true)
                    }

                    val failure = { _: IOException ->
                        Logger.log(LogType.PUSHED_LOGS, listOf("success:false"), context)
                        cont.resume(false)
                    }

                    API().sendLogs(Logger.formatLogs(), callback, failure)
                }
                result
            }

        }

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
                                            Date(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(media.getString("createdAt")).time),
                                            0)

                                    val times = media.getJSONArray("times")

                                    if (mediaList.contains(newMedia)) {
                                        toBeRemoved.remove(newMedia)
                                        mediaFetch++

                                        if (mediaFetch == payload.length()) {
                                            for (m in toBeRemoved) {
                                                GlobalScope.launch {
                                                    Logger.log(LogType.CONTENT_REMOVED, listOf("contentID:${m._id}"), context)
                                                    watchMediaRepository.delete(m._id)
                                                    deleteFile(context, m.path)
                                                }
                                            }
                                            Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
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
                                                    newMedia.duration = Movie.decodeStream(FileInputStream(file)).duration()

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
                                                    Logger.log(LogType.NEW_CONTENT_ADDED, listOf("contentID:${newMedia._id}"), context)
                                                    addMediaDates(mediaDateRepository, newMedia._id, times)
                                                    if (mediaFetch == payload.length()) {
                                                        for (m in toBeRemoved) {
                                                            Logger.log(LogType.CONTENT_REMOVED, listOf("contentID:${m._id}"), context)
                                                            watchMediaRepository.delete(m._id)
                                                            deleteFile(context, m.path)
                                                        }
                                                        try {
                                                            Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                                                            cont.resume(true)
                                                        } catch (e : Exception) {
                                                            Log.d("PullMedia", "$e")
                                                        }
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
                                            Logger.log(LogType.CONTENT_REMOVED, listOf("contentID:${media._id}"), context)
                                            watchMediaRepository.delete(media._id)
                                            deleteFile(context, media.path)
                                        }
                                        Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                                        cont.resume(true)
                                    }
                                }
                            }

                        }
                    }

                    val failure = { e: java.lang.Exception ->
                        Log.d("Error", "$e")
                        Logger.log(LogType.PULLED_CONTENT, listOf("success:false"), context)
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
                        Logger.log(LogType.CONTENT_REMOVED, listOf("contentID:${mediaToRemove._id}"), context)
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
                                Date(System.currentTimeMillis()),
                                0) //fixme check the name from the api json response

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
                                    Logger.log(LogType.NEW_CONTENT_ADDED, listOf("contentID:${newWatchMedia._id}"), context)
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
                                        Date(System.currentTimeMillis()),
                                        0)) //fixme check the name from the api json response
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
                                        Logger.log(LogType.NEW_CONTENT_ADDED, listOf("contentID:${media._id}"), context)
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

        private suspend fun addMediaDates(repository: MediaDateRepository, mediaId:String, times:JSONArray){
            Log.d("Times", "$times")
            for (j in 0 until times.length()) {
                val time = times.getJSONObject(j).getLong("value") as Long?
                Log.d("Times", "$time")


                var c = Calendar.getInstance()
                c.timeInMillis = time!!


                val newMediaDate = MediaDate(MediaDate.longToDate(time!!), mediaId, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) )
                repository.insert(newMediaDate)
            }
        }

    }

}



