package uk.ac.ncl.openlab.ongoingness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainPresenter {

    private val api:API = API()
    private var view: View? = null
    private val idsFile = "ids.txt"
    private var permCollection: ArrayList<Bitmap> = ArrayList()
    private val tempCollection: ArrayList<Bitmap> = ArrayList()
    private var collection: ArrayList<Bitmap> = ArrayList()
    private var maxPerm: Int = 0
    private var maxTemp: Int = 0

    private var context: Context? = null
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
        api.generateToken { token ->  Log.d("API",token) }
    }

    /**
     * Store the perm collection
     */
    private fun storeCollection(container: Container) {


        clearMediaFolder(context!!)
        when(container){
            Container.PERM ->{
                persistArray(permIds!!, idsFile, context!!)
                persistBitmaps(permCollection.toTypedArray(), context!!)
            }
            Container.TEMP ->{
                persistBitmaps(tempCollection.toTypedArray(), context!!)
            }
        }




    }

    /**
     * Load a collection from saved files.
     */
    fun loadPermCollection() {

        if (permCollection.size == 0) {
            displayCode()
        } else {
            val bitmap: Bitmap? = updateBitmap()
            view?.updateBackground(bitmap!!)
            view?.setReady(true)
        }
    }

    /**
     * Fetch images of of the past and add to an array list.
     *
     * @param links Array<String>
     * @param container String name of container
     */
    private fun fetchBitmaps(links: Array<String>, container: Container) {

        links.forEach { link ->
            api.fetchBitmap(link) { image ->
                when (container) {
                    Container.TEMP -> { tempCollection.add(Bitmap.createScaledBitmap(image, view?.getScreenSize()!!, view?.getScreenSize()!!, false)) }
                    Container.PERM ->{ permCollection.add(Bitmap.createScaledBitmap(image, view?.getScreenSize()!!, view?.getScreenSize()!!, false)) }
                }
            }
        }
        onContainerUpdate(container)
    }

    /**
     * Fetch all media from the api.
     */
    fun fetchAllMedia() {

        api.fetchAllMedia { allMedia ->

            val file = File(context?.filesDir, idsFile)
            var same = true

            // Split into two filtered arrays
            val permMedia: List<Media> = allMedia!!.filter { media: Media -> media.locket == "perm" }
            val tempMedia: List<Media> = allMedia!!.filter { media: Media -> media.locket == "temp" }

            // Set global media maxims
            maxPerm = permMedia.size
            maxTemp = tempMedia.size

            /*
             * If there is no uploaded media, then default to setup.
             * Should display the device's code.
             */
            if (maxPerm == 0 && maxTemp == 0) {
                view?.displayText("Device Code:\n${getMacAddress()}")
                return@fetchAllMedia
            }

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
            }
        }

        collection = (permCollection + tempCollection) as ArrayList<Bitmap>

        storeCollection(container)

    }



    var index = -1
    fun updateBitmap(): Bitmap? {
        return if(collection.size > 0){
            index++
            if(index >= collection.size) index %= collection.size
            collection[index]
        }else{
            null
        }
    }

    /**
     * Handle a network error, default to loading perm collection
     */
    private fun onNetworkError() {
        loadPermCollection()
    }

    /**
     * Display the devices mac address
     */
    fun displayCode() {
        val mac = getMacAddress()
        val txt = "Device Code:\n$mac"
        view?.displayText(txt)
    }

    /**
     * Control the view, must implement these methods
     */
    interface View {
        fun updateBackground(bitmap: Bitmap)
        fun displayText(addr: String)
        fun getScreenSize(): Int
        fun openLocket()
        fun closeLocket()
        fun getReady(): Boolean
        fun setReady(ready : Boolean)
    }
}