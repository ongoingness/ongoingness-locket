package uk.ac.ncl.openlab.ongoingness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainPresenter {
    private var view: View? = null
    private val apiUrl = "https://ongoingness-api.openlab.ncl.ac.uk/api"
    private val idsFile = "ids.txt"
    private val permCollection: ArrayList<Bitmap> = ArrayList()
    private val tempCollection: ArrayList<Bitmap> = ArrayList()
    private var maxPerm: Int = 0
    private var maxTemp: Int = 0
    private var allMedia: Array<Media>? = null
    private var token: String? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .build()
    private var context: Context? = null
    private var lastPerm = false
    private var permIdx = 0
    private var tempIdx = 0

    /**
     * Attach the view to the presenter
     *
     * @param view View to attach
     */
    fun attachView(view: View) {
        this.view = view
    }

    /**
     * Detach the view from the presenter
     * Call this on view's onDestroy method.
     */
    fun detachView() {
        this.view = null
    }

    /**
     * Set the context for http requests
     *
     * @param context
     */
    fun setContext(context: Context) {
        this.context = context
    }

    /**
     * Load the perm collection
     */
    fun loadPermanentCollection() {
        val ids: List<String> = File(context?.filesDir, idsFile).readLines()

        context?.filesDir!!.listFiles().forEach { file: File -> run {
            if (file.name == idsFile) return@run
            if (ids.contains(file.name)) {
                permCollection.add(BitmapFactory.decodeFile(file.absolutePath))
            }
        } }
    }

    /**
     * Store the perm collection
     */
    fun storePermCollection() {
        permCollection.forEach { bitmap : Bitmap ->
            run {
                val file = File(context?.filesDir, "${hashCode()}.jpg")
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

                file.writeBytes(stream.toByteArray())
                stream.close()
            }
        }
    }

    /**
     * Fetch images of of the past and add to an array list.
     *
     * @param links Array<String>
     * @param container String name of container
     * @param callback callback for when item is placed in container.
     */
    private fun fetchBitmaps(links: Array<String>, container: Container) {
        if (token == null) throw Error("Token cannot be empty")
        if (!hasConnection(context)) return

        for (id in links) {
            val url = "$apiUrl/media/$id/"
            val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .build()

            Log.d("fetchBitmaps", "Fetching bitmap from $url")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    e?.printStackTrace()
                }

                override fun onResponse(call: Call?, response: Response) {
                    try {
                        val inputStream = response.body()?.byteStream()
                        when (container) {
                            Container.TEMP -> {
                                tempCollection.add(Bitmap.createScaledBitmap(
                                    BitmapFactory.decodeStream(inputStream),
                                    view?.getScreenSize()!!,
                                    view?.getScreenSize()!!,
                                    false))
                            }

                            Container.PERM -> {
                                permCollection.add(Bitmap.createScaledBitmap(
                                        BitmapFactory.decodeStream(inputStream),
                                        view?.getScreenSize()!!,
                                        view?.getScreenSize()!!,
                                        false))
                            }
                        }

                        onContainerUpdate(container)
                    } catch (error: Error) {
                        error.printStackTrace()
                    }
                }
            })
        }
    }

    /**
     * Fetch all media from the api.
     */
    fun fetchAllMedia() {
        val gson = Gson()
        val url = "$apiUrl/media"

        if (token == null) throw Error("Token cannot be empty")
        if (!hasConnection(context)) return

        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token!!)
                .build()

        Log.d("fetchAllMedia", "Getting all media")
        Log.d("fetchAllMedia", url)

        client.newCall(request).enqueue(object : Callback {
            /**
             * On error, just load permanent collection
             */
            override fun onFailure(call: Call, e: IOException) {
                loadPermanentCollection()
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val mediaResponse: MediaResponse = gson.fromJson(response.body()?.string(), MediaResponse::class.java)

                allMedia = mediaResponse.payload
                val file = File(context?.filesDir, idsFile)
                var same = true

                val permMedia: List<Media> = allMedia!!.filter { media: Media -> media.locket == "perm" }
                val tempMedia: List<Media> = allMedia!!.filter { media: Media -> media.locket == "temp" }

                maxPerm = permMedia.size
                maxTemp = tempMedia.size

                if (file.exists()) {
                    val ids: List<String> = file.readLines()
                    permMedia.forEach { media : Media ->
                        run {
                            if (!ids.contains(media._id)) same = false
                        }
                    }
                } else {
                    same = false
                }

                if (!same) {
                    permMedia.forEach { m: Media -> file.writeText("${m._id}\n") }
                    // Populate perm

                    fetchBitmaps(
                        permMedia
                            .map { media: Media -> media._id }
                            .toTypedArray(),
                        Container.PERM
                    )
                }
                fetchBitmaps(
                    tempMedia
                        .map { media: Media -> media._id }
                        .toTypedArray(),
                    Container.TEMP
                )
            }
        })
    }

    /**
     * Generate a token from the api using the devices mac address.
     *
     * @param callback function to call after generating a token
     */
    fun generateToken(callback: () -> Unit) {
        val url = "$apiUrl/auth/mac"
        val gson = Gson()
        val mac: String = getMacAddress() // Get mac address
        println(mac)
        val formBody = FormBody.Builder()
                .add("mac", mac)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val genericResponse: GenericResponse = gson.fromJson(
                        response.body()?.string(),
                        GenericResponse::class.java)

                // Set token
                token = genericResponse.payload

                callback()
            }
        })
    }

    private fun onContainerUpdate(container: Container) {
        val isPerm: Boolean = when(container) {
            Container.PERM -> true
            Container.TEMP -> false
        }

        if(isPerm) {
            if (permCollection.size < maxPerm) return
            dummyOpen()
            storePermCollection()
        } else {
            if (tempCollection.size < maxTemp) return
        }
    }

    /**
     * Dummy function for opening and closing locket.
     */
    fun dummyOpen(): Bitmap {
        if (tempCollection.size > 0 && lastPerm) {
            tempIdx++
            lastPerm = !lastPerm
            if (tempIdx >= tempCollection.size) tempIdx %= tempCollection.size
            return tempCollection[tempIdx]
        }
        permIdx++
        if (permIdx >= permCollection.size) permIdx %= permCollection.size
        lastPerm = !lastPerm
        return permCollection[permIdx]
    }

    interface View {
        fun updateBackground(bitmap: Bitmap)
        fun getScreenSize(): Int
    }
}