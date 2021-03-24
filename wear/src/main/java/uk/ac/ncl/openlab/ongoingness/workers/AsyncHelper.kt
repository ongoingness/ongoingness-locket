package uk.ac.ncl.openlab.ongoingness.workers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Movie
import android.util.Log
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
import uk.ac.ncl.openlab.ongoingness.utilities.*
import java.io.*
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Set of helper functions to facilitated the push of logs and pull of data to the server.
 *
 * @author Luis Carvalho
 */
class AsyncHelper {

    companion object {

        /**
         *  Synchronously pushes the logs to the server in a coroutine.
         *
         * @param context context of the application.
         * @return true if the logs where pushed successfully.
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

                    API(context).sendLogs(Logger.formatLogs(), callback, failure)
                }
                result
            }

        }

        /**
         * Synchronously pulls all media content belonging to a user from the server in a coroutine.
         *
         * @param context context of the application.
         * @result true if media was pulled successfully from the server.
         */
        @SuppressLint("SimpleDateFormat")
        fun pullMediaLocket(context: Context): Boolean {

            return runBlocking {

                val result = suspendCoroutine<Boolean> { cont ->

                    val api = API(context)

                    val watchMediaDao = WatchMediaRoomDatabase.getDatabase(context).watchMediaDao()
                    val watchMediaRepository = WatchMediaRepository(watchMediaDao)

                    val mediaDateDao = WatchMediaRoomDatabase.getDatabase(context).mediaDateDao()
                    val mediaDateRepository = MediaDateRepository(mediaDateDao)

                    val mediaList = watchMediaRepository.getAll().sortedWith(compareBy({ it.collection }, { it.order }))

                    val callback = callback@{ response: Response? ->

                        resetFailureCounter(context)

                        val stringResponse = response!!.body()?.string()
                        val jsonResponse = JSONObject(stringResponse)

                        val code = jsonResponse.getString("code")

                        if (!code.startsWith('2')) {
                            Logger.log(LogType.PULLED_CONTENT, listOf("success:false"), context)
                            cont.resume(false)
                            return@callback
                        }

                        val payload: JSONArray = jsonResponse.getJSONArray("payload")

                        val toBeRemoved = mediaList.toTypedArray().copyOf().toMutableList()

                        if (payload.length() == 0) {
                            removeMedia(context, toBeRemoved, watchMediaRepository)
                            Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                            response.body()?.close()
                            cont.resume(true)
                            return@callback
                        }

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
                            } else {
                                val fetchResult = fetchMedia(context, api, newMedia, watchMediaRepository)
                                if(fetchResult) {
                                    runBlocking {
                                        addMediaDates(mediaDateRepository, newMedia._id, times)
                                    }
                                }
                            }
                        }
                        removeMedia(context, toBeRemoved, watchMediaRepository)
                        Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                        response.body()?.close()
                        cont.resume(true)
                        return@callback
                    }

                    val failure = { e: java.lang.Exception ->
                        if(e.message!!.startsWith('4')) {
                            Log.d("404", "Page does not exist.")
                            recordFailure(context)
                        }
                        Log.d("Error", "$e")
                        Logger.log(LogType.PULLED_CONTENT, listOf("success:false"), context)
                        cont.resume(false)
                    }

                    api.fetchMediaPayload(callback, failure)
                }
                result
            }
        }

        /**
         * Synchronously pulls the most recent piece of media belonging to the present collection,
         * plus 5 from the past collection if a new piece of present media is found.
         *
         * @param context context of the application.
         * @result true if media was pulled successfully from the server.
         */
        fun pullMediaRefind(context: Context): Boolean {

            return runBlocking {

                val result = suspendCoroutine<Boolean> { cont ->

                    val api = API(context)

                    val watchMediaDao = WatchMediaRoomDatabase.getDatabase(context).watchMediaDao()
                    val watchMediaRepository = WatchMediaRepository(watchMediaDao)

                    // Get the current media belonging to the present collection in the device.
                    val mediaList = watchMediaRepository.getAll().sortedBy { it.order }
                    val currentImageID: String
                    currentImageID = if (mediaList.isNullOrEmpty()) {
                        "test"
                    } else {
                        mediaList[0]._id
                    }

                    val callback = callback@{ response: Response? ->

                        resetFailureCounter(context)

                        val stringResponse = response!!.body()?.string()
                        if (stringResponse == "[]") {
                            Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                            cont.resume(true)
                            return@callback
                        }

                        val jsonResponse = JSONObject(stringResponse)
                        val code = jsonResponse.getString("code")

                        if (!code.startsWith('2')) {
                            Logger.log(LogType.PULLED_CONTENT, listOf("success:false"), context)
                            cont.resume(false)
                            return@callback
                        }

                        val payload: JSONArray = jsonResponse.getJSONArray("payload")
                        if (payload.length() == 0) {
                            Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                            cont.resume(true)
                            return@callback
                        }

                        //Delete existing media
                        for (mediaToRemove in mediaList) {
                            Logger.log(LogType.CONTENT_REMOVED, listOf("contentID:${mediaToRemove._id}"), context)
                            deleteFile(context, mediaToRemove.path)
                        }
                        watchMediaRepository.deleteAll()

                        //Set present image
                        val presentImage: JSONObject = payload.getJSONObject(0)

                        val newWatchMedia = WatchMedia(presentImage.getString("_id"),
                                presentImage.getString("path"),
                                /*presentImage.getString("locket")*/"present",
                                presentImage.getString("mimetype"),
                                0,
                                Date(System.currentTimeMillis()),
                                0) //fixme check the name from the api json response

                        fetchMedia(context, api, newWatchMedia, watchMediaRepository)

                        //Set past images
                        if (payload.length() < 1) {
                            Logger.log(LogType.PULLED_CONTENT, listOf("success:true"), context)
                            cont.resume(true)
                            return@callback
                        }

                        val pastImages = mutableListOf<WatchMedia>()
                        var totalMedia = 0
                        for (i in 1 until payload.length()) {
                            try {
                                val pastImage: JSONObject = payload.getJSONObject(i)
                                pastImages.add(WatchMedia(pastImage.getString("id"),
                                        pastImage.getString("path"),
                                        "past",
                                        pastImage.getString("mimetype"),
                                        i,
                                        Date(System.currentTimeMillis()),
                                        0))
                                totalMedia += 1
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }

                        for (media: WatchMedia in pastImages) {
                            fetchMedia(context, api, media, watchMediaRepository)
                        }
                    }

                    val failure = { e: java.lang.Exception ->
                        if(e.message!!.startsWith('4')) {
                            Log.d("404", "Page does not exist.")
                            recordFailure(context)
                        }
                        Log.d("Error", "$e")
                        Logger.log(LogType.PULLED_CONTENT, listOf("success:false"), context)
                        cont.resume(false)
                    }
                    api.fetchInferredMedia(currentImageID, callback, failure)
                }
                result
            }
        }

        /**
         * Adds the dates of a new media content item to the local database.
         *
         * @param repository access point to the database.
         * @param mediaId id of media content item.
         * @param times JSON Array of all the dates to be recorded.
         */
        private suspend fun addMediaDates(repository: MediaDateRepository, mediaId:String, times:JSONArray){
            for (j in 0 until times.length()) {
                val time = times.getJSONObject(j).getLong("value") as Long?
                Log.d("Times", "$time")


                var c = Calendar.getInstance()
                c.timeInMillis = time!!


                val newMediaDate = MediaDate(MediaDate.longToDate(time!!), mediaId, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) )
                repository.insert(newMediaDate)
            }
        }

        /**
         * Synchronously fetches a media file from the server and stores it in the device.
         *
         * @param context context of the application.
         * @param api object containing the server calls.
         * @param media media item containing info of the file to be fetch.
         * @param repository access point to the local database.
         * @return true if the file was successfully fetch and the info was added to the local database.
         */
        private fun fetchMedia(context: Context, api: API, media: WatchMedia, repository: WatchMediaRepository): Boolean {

            return  runBlocking {

                val result = suspendCoroutine<Boolean> { cont ->

                    val callback = callback@ { body: ResponseBody? ->
                        val inputStream = body?.byteStream()
                        val file = File(context.filesDir, media.path)
                        lateinit var outputStream: OutputStream

                        outputStream = FileOutputStream(file)
                        if (media.mimetype.contains("video") || media.mimetype.contains("gif")) {

                            try {
                                inputStream.use { input ->
                                    outputStream.use { fileOut ->
                                        input!!.copyTo(fileOut)
                                    }
                                }
                                media.duration = Movie.decodeStream(FileInputStream(file)).duration()

                            } catch (e: Exception) {
                                Log.d("InputStream", "$e")
                                body?.close()
                                cont.resume(false)
                                return@callback
                            }

                        } else {
                            val image = BitmapFactory.decodeStream(inputStream)
                            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }
                        outputStream.flush()
                        outputStream.close()
                        inputStream?.close()

                        runBlocking {
                            repository.insert(media)
                            body?.close()
                            Logger.log(LogType.NEW_CONTENT_ADDED, listOf("contentID:${media._id}"), context)
                            cont.resume(true)
                        }
                    }

                    val failure = { e: Exception ->
                        cont.resume(false)
                    }

                    api.fetchBitmap(media._id, 300, callback, failure)
                }
                result
            }
        }

        /**
         * Given a list of media items, it removes the from the local database.
         *
         * @param context context of the application.
         * @param toBeRemoved list of media items to be removed.
         * @param watchMediaRepository access point to the local database.
         */
        private fun removeMedia(context: Context, toBeRemoved:List<WatchMedia>, watchMediaRepository: WatchMediaRepository) {
            return  runBlocking {
                if (toBeRemoved.isNotEmpty()) {
                    for (media in toBeRemoved) {
                        Logger.log(LogType.CONTENT_REMOVED, listOf("contentID:${media._id}"), context)
                        watchMediaRepository.delete(media._id)
                        deleteFile(context, media.path)
                    }
                }
            }
        }
    }

}



