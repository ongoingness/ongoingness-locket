package uk.ac.ncl.openlab.ongoingness.utilities

import android.accounts.Account
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MediaSyncAdapter @JvmOverloads constructor(
        context: Context,
        autoInitialize: Boolean,
        allowParallelSyncs: Boolean = false,

        val mContentResolver: ContentResolver = context.contentResolver
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    override fun onPerformSync(account: Account?, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {

            Log.d("sync", "please")

            val api = API()

            val watchMediaDao = WatchMediaRoomDatabase.getDatabase(this.context).watchMediaDao()
            val repository = WatchMediaRepository(watchMediaDao)

            val context = this.context
            val filesDir = this.context.filesDir


            when (BuildConfig.FLAVOR) {
                "locket_touch" -> {

                    GlobalScope.launch {
                        var mediaList = repository.getAll().sortedWith(compareBy({ it.collection }, { it.order }))


                        api.fetchMediaPayload(

                                callback = { response ->

                                    Log.d("sa", "nice")

                                    var stringResponse = response!!.body()?.string()
                                    val jsonResponse = JSONObject(stringResponse)

                                    Log.d("sa", stringResponse + jsonResponse.getString("code"))

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
                                                    }
                                                } else {
                                                    var gotFile = false
                                                    while (!gotFile) {
                                                        try {
                                                            api.fetchBitmap(newMedia._id) { body ->

                                                                val inputStream = body?.byteStream()
                                                                val file = File(filesDir, newMedia.path)
                                                                lateinit var outputStream: OutputStream

                                                                //try {
                                                                outputStream = FileOutputStream(file)
                                                                if (newMedia.mimetype.contains("video") ||
                                                                        newMedia.mimetype.contains("gif")) {
                                                                    inputStream.use { input ->
                                                                        outputStream.use { fileOut ->
                                                                            input!!.copyTo(fileOut)
                                                                        }
                                                                    }
                                                                } else {
                                                                    val image = BitmapFactory.decodeStream(inputStream)
                                                                    image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                                                }
                                                                outputStream.flush()
                                                                mediaFetch++

                                                                //} catch (e: IOException) {
                                                                //throw Exception("File Fetching - Something went wrong")
                                                                //} finally {
                                                                outputStream.close()
                                                                inputStream?.close()
                                                                GlobalScope.launch {
                                                                    repository.insert(newMedia)
                                                                    if (mediaFetch == payload.length()) {
                                                                        for (media in toBeRemoved) {
                                                                            repository.delete(media._id)
                                                                            deleteFile(context, media.path)
                                                                        }
                                                                    }
                                                                }
                                                                //}
                                                            }
                                                            gotFile = true
                                                        } catch (e: Exception) {
                                                            Log.d("FetchFile", "Fail")
                                                            gotFile = false
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
                                                }
                                            }
                                        }
                                    }
                                },
                                failure = { e ->

                                    Log.d("sa", "fail $e")

                                })
                    }
                }

                "refind" -> {
                    GlobalScope.launch {
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
                                }
                            }
                        }
                    }
                }
            }
    }

}