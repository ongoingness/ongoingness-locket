package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.utilities.*
import java.io.File
import java.lang.Exception
import kotlin.collections.ArrayList

class MainPresenter {

    private val api: API = API()
    private var view: View? = null
    private val idsFile = "ids.txt"
    private var permCollection: ArrayList<Bitmap> = ArrayList()
    private val tempCollection: ArrayList<Bitmap> = ArrayList()
    private var collection:ArrayList<Bitmap> = ArrayList()
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

    }

    /**
     * Store the perm collection
     */
    private fun storeCollection() {


        clearMediaFolder(context!!)
        persistArray(permIds!!, idsFile, context!!)





    }

    /**
     * Load a collection from saved files.
     */
    fun loadPermCollection() {


        val file = File(context?.filesDir, idsFile)
        if (file.exists()) {
            val ids: List<String> = file.readLines()
            for(filename in ids){
                Log.d("Loading",filename)
                try {
                    permCollection.add(getBitmapFromFile(context!!, filename)!!)
                }catch(e:Exception){
                    Log.e("Loading",e.toString())
                }
            }
        }

        collection = permCollection


        if (collection.size == 0) {
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

        links.forEach{link ->

            val filename = "$link.jpg"

            if(!hasLocalCopy(context!!, filename)) {

                api.fetchBitmap(link, view?.getScreenSize()!!) { body ->
                    val inputStream = body?.byteStream()
                    val image = BitmapFactory.decodeStream(inputStream)
                    Log.d("Presenter", "Got image: $image")
                    when (container) {
                        Container.TEMP -> {
                            tempCollection.add(image)
                        }
                        Container.PERM -> {
                            permCollection.add(image)
                        }
                    }

                    persistBitmap(image, filename, context!!)
                    collection.add(image)

                    Log.d("Presenter", "Perm: ${permCollection.size} Temp: ${tempCollection.size}")
                }
            }else{
                Log.d("Presenter","using Local Copy")
                collection.add(getBitmapFromFile(context!!, filename)!!)
            }
        }
    }

    /**
     * Fetch all media from the api.
     */
    fun fetchAllMedia() {


        api.fetchMedia { allMedia ->

            setConfigured(context!!, true)

            val file = File(context?.filesDir, idsFile)
            var same = true

            // Split into two filtered arrays
            val permMedia: List<Media> = allMedia!!.filter { media: Media -> media.locket == "perm" }
            val tempMedia: List<Media> = allMedia.filter { media: Media -> media.locket == "temp" }

            // Set global media maxims
            maxPerm = permMedia.size
            maxTemp = tempMedia.size

            /*
             * If there is no uploaded media, then default to setup.
             * Should display the device's code.
             */
            if (maxPerm == 0 && maxTemp == 0) {
                view?.displayText("Device Code:\n${getMacAddress()}")
                return@fetchMedia
            }

            // Get a list of ids from the media
            var permFilenames = permMedia.map { media: Media -> "${media._id}.jpg" }.toTypedArray()

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

            persistArray(permFilenames, idsFile, context!!)

            // If files don't match, else load stored collection
            if (!same) {
                // Get new perm collection
                fetchBitmaps(permIds!!, Container.PERM)
            } else {
                loadPermCollection()
            }

            fetchBitmaps(tempIds!!, Container.TEMP)
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
            storeCollection()
        }

        collection = (permCollection + tempCollection) as ArrayList<Bitmap>

        Log.d("Presenter","Collection: ${collection.size} Perm: ${permCollection.size} Temp: ${tempCollection.size}")


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