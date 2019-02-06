package uk.ac.ncl.openlab.ongoingness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainPresenter {
    private var view: View? = null
    private val apiUrl = "https://ongoingness-api.openlab.ncl.ac.uk/api"
    private val idsFile = "ids.txt"
    private var permCollection: ArrayList<Bitmap> = ArrayList()
    private val tempCollection: ArrayList<Bitmap> = ArrayList()
    private var maxPerm: Int = 0
    private var maxTemp: Int = 0
    private var allMedia: Array<Media>? = null
    private var token: String? = null
    private val client: OkHttpClient = OkHttpClient
            .Builder()
            .connectionSpecs(
                    Arrays.asList(ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.CLEARTEXT))
            .build()
    private var context: Context? = null
    private var lastPerm = false
    private var permIdx = 0
    private var tempIdx = 0
    private var tempIds: Array<String>? = null
    private var permIds: Array<String>? = null

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
     * Store the perm collection
     */
    private fun storePermCollection() {
        clearMediaFolder(context!!)

        persistArray(permIds!!, idsFile, context!!)
        persistBitmaps(permCollection.toTypedArray(), context!!)
    }

    /**
     * Load a collection from saved files.
     */
    private fun loadPermCollection() {
        permCollection = loadBitmaps(context!!)
        val bitmap: Bitmap? = updateBitmap()
        view?.updateBackground(bitmap!!)
        view?.setReady(true)
    }

    /**
     * Fetch images of of the past and add to an array list.
     *
     * @param links Array<String>
     * @param container String name of container
     */
    private fun fetchBitmaps(links: Array<String>, container: Container) {
        if (token == null) throw Error("Token cannot be empty")
        if (!hasConnection(context)) return

        for (id in links) {
            val url = "$apiUrl/media/$id/"
            val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token!!)
                    .build()

            Log.d("fetchBitmaps", "Fetching bitmap from $url")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    e?.printStackTrace()
                }

                /**
                 * Create a bitmap from the returned image.
                 */
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        // Get an input stream
                        val inputStream = response.body()?.byteStream()

                        // Get container to place image
                        when (container) {
                            Container.TEMP -> {
                                // Create a scaled bitmap to screen size from input stream.
                                tempCollection.add(Bitmap.createScaledBitmap(
                                    BitmapFactory.decodeStream(inputStream),
                                    view?.getScreenSize()!!,
                                    view?.getScreenSize()!!,
                                    false))
                            }

                            Container.PERM -> {
                                // Create a scaled bitmap to screen size from input stream.
                                permCollection.add(Bitmap.createScaledBitmap(
                                        BitmapFactory.decodeStream(inputStream),
                                        view?.getScreenSize()!!,
                                        view?.getScreenSize()!!,
                                        false))
                            }
                        }

                        // Handle item being added to a container
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
                loadPermCollection()
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val file = File(context?.filesDir, idsFile)
                var same = true

                // Parse response to Media response.
                // Typed array of Media
                val mediaResponse: MediaResponse = gson.fromJson(
                        response.body()?.string(),
                        MediaResponse::class.java)

                allMedia = mediaResponse.payload

                // Split into two filtered arrays
                val permMedia: List<Media> = allMedia!!
                        .filter { media: Media -> media.locket == "perm" }
                val tempMedia: List<Media> = allMedia!!
                        .filter { media: Media -> media.locket == "temp" }

                // Set global media maxims
                maxPerm = permMedia.size
                maxTemp = tempMedia.size

                // Get a list of ids from the media
                permIds = permMedia.map { media: Media -> media._id }.toTypedArray()
                tempIds = tempMedia.map { media: Media -> media._id }.toTypedArray()

                // If an IDS file exits, then check if perm collection needs updating.
                if (file.exists()) {
                    val ids: List<String> = file.readLines()
                    permMedia.forEach { media : Media ->
                        run {
                            // If not all new media is found, then stored files need updating.
                            if (!ids.contains(media._id)) same = false
                        }
                    }
                } else {
                    same = false
                }

                // If files don't match, else load stored collection
                if (!same) {
                    // Get new perm collection
                    fetchBitmaps(
                        permIds!!,
                        Container.PERM
                    )
                } else {
                    loadPermCollection()
                }

                fetchBitmaps(
                    tempIds!!,
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

    /**
     * Handler for item placed in container
     *
     * @param container Container
     */
    private fun onContainerUpdate(container: Container) {
        val isPerm: Boolean = when(container) {
            Container.PERM -> true
            Container.TEMP -> false
        }

        if(isPerm) {
            if (permCollection.size == maxPerm) {
                val bitmap: Bitmap? = updateBitmap()
                view?.updateBackground(bitmap!!)
                view?.setReady(true)
                storePermCollection()
            }
        }
    }

    /**
     * Dummy function for opening and closing locket.
     * Should alternate between returning a permanent or temp bitmap
     */
    fun updateBitmap(): Bitmap? {
        // If there are temp images and last image was perm...
        if (tempCollection.size > 0 && lastPerm) {
            /*
             * Update the index
             * flip lastPerm flag
             * wrap around array if needed
             * return bitmap.
             */
            tempIdx++
            lastPerm = !lastPerm
            if (tempIdx >= tempCollection.size) tempIdx %= tempCollection.size
            Log.d("updateBitmap", "Returning temp")
            return tempCollection[tempIdx]
        }

        if(permCollection.size < 0) return null

        /*
         * Default to perm
         *
         * update perm index
         * wrap around array
         * flip flag
         * return perm bitmap.
         */
        permIdx++
        if (permIdx >= permCollection.size) permIdx %= permCollection.size
        lastPerm = !lastPerm
        Log.d("updateBitmap", "Returning perm")
        return permCollection[permIdx]
    }

    /**
     * Control the view, must implement these methods
     */
    interface View {
        fun updateBackground(bitmap: Bitmap)
        fun getScreenSize(): Int
        fun openLocket()
        fun closeLocket()
        fun getReady(): Boolean
        fun setReady(ready : Boolean)
    }
}