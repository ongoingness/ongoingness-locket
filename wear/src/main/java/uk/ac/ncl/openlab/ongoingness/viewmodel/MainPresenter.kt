package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.utilities.*
import java.io.File
import java.sql.Date

/**
 * Connects the activity view to the media in the local database, allowing it to be displayed on screen.
 */
class MainPresenter {

    var view: View? = null
    private var context: Context? = null
    private var mediaCollection: List<WatchMedia>? = null
    private var currentIndex = 0;
    private lateinit var watchMediaViewModel: WatchMediaViewModel
    private var displayContent: Boolean = false

    private var coverWhiteBitmap: Bitmap? = null
    private var coverBitmap: Bitmap? = null

    private var newImageTime: Long? = null
    private var indexTime: Int? = null

    /**
     * Gets the collection of images in the local database
     *
     * @param activity Activity here the media is going to be displayed
     */
    fun setWatchMediaRepository(activity: FragmentActivity) {

        watchMediaViewModel = ViewModelProviders.of(activity).get(WatchMediaViewModel::class.java)


        when(FLAVOR) {
            "refind" -> {
                mediaCollection = watchMediaViewModel.allWatchMedia().sortedWith(compareBy({it.collection}, {it.order}))
            }

            "locket_touch" -> {
                mediaCollection = watchMediaViewModel.allWatchMedia().sortedWith(compareBy({it.collection}, {it.createdAt}))
            }

            "locket_touch_s" -> {
                var arraylist:ArrayList<WatchMedia> =  ArrayList()
                arraylist.addAll(watchMediaViewModel.getWatchMediaForDate(Date(System.currentTimeMillis())).sortedWith(compareBy {it.createdAt}))
                arraylist.addAll(watchMediaViewModel.getWatchMediaWithNoDate().sortedWith(compareBy {it.createdAt}))
                mediaCollection =  arraylist
            }
        }
        if(displayContent)
            displayNewMediaFromCollection(mediaCollection)
    }



    /**
     * Restarts the index of to be displayed
     */
    fun restartIndex() {
        currentIndex = 0
    }

    /**
     * Updates the black cover
     */
    fun updateCoverBitmap(bitmap: Bitmap) {
        this.coverBitmap = bitmap
        if(!displayContent)
            view?.updateBackgroundWithBitmap(coverBitmap!!)
    }

    /**
     * Display behaviour during the fetching of new media from the server
     *
     * @param state either is fetching data from the server or not
     */
    fun pullingData(state: Boolean) {
        if (state) hideContent(CoverType.WHITE) else displayContent()
    }

    /**
     * Displays the next image in the collection
     */
    fun goToNextImage(){
        if(displayContent) {
            currentIndex++
            displayNewMediaFromCollection(mediaCollection)
        }
    }

    /**
     * Displays the previous images in the collection
     */
    fun goToPreviousImage() {
        if(displayContent) {
            currentIndex--
            displayNewMediaFromCollection(mediaCollection)
        }
    }

    /**
     * Sets the cover to be displayed
     *
     * @param coverType The type of cover to be displayed
     */
    private fun setCover(coverType: CoverType) {
        when(coverType) {
            CoverType.BLACK -> view?.updateBackgroundWithBitmap(coverBitmap!!)
            CoverType.WHITE -> view?.updateBackgroundWithBitmap(coverWhiteBitmap!!)
        }
        view?.setReady(true)
    }

    /**
     * Displays the media in the current index from a collection of media
     *
     * @param localCollection Collection of media
     */
    private fun displayNewMediaFromCollection(localCollection: List<WatchMedia>?) {
        if (localCollection.isNullOrEmpty() || !displayContent) {
            currentIndex = 0
        } else {

            if (currentIndex >= localCollection.size)
                currentIndex %= localCollection.size
            else if (currentIndex < 0)
                currentIndex = localCollection.size - 1

            if(newImageTime == null) {
                newImageTime = System.currentTimeMillis()
                indexTime = currentIndex
            } else {
                var timePassed = System.currentTimeMillis() - newImageTime!!
                var content = listOf("imageID:${localCollection[currentIndex]._id}", "displayedTime:$timePassed")

                if((indexTime == localCollection.size && currentIndex == 0) || currentIndex > indexTime!!)
                    Logger.log(LogType.NEXT_IMAGE, content, context!! )
                else if((indexTime == 0 && currentIndex == localCollection.size) || currentIndex < indexTime!!)
                    Logger.log(LogType.PREV_IMAGE, content, context!! )

                newImageTime = System.currentTimeMillis()
                indexTime = currentIndex

            }

            var file  = File(context!!.filesDir, localCollection[currentIndex].path)
            if(!file.exists()) {
                //Just in case something goes wrong with the file
                watchMediaViewModel.delete(localCollection!![currentIndex], view!!.getContext())
                goToNextImage()
            }
            if(localCollection[currentIndex].mimetype.contains("gif"))
                view?.updateBackground(file, View.MediaType.GIF)
            else
                view?.updateBackground(file, View.MediaType.IMAGE)
            view?.setReady(true)
        }
    }

    /**
     * Displays the current piece of media
     */
    fun displayContent() {
        this.displayContent = true
        displayNewMediaFromCollection(mediaCollection)
    }

    /**
     * Displays the cover
     *
     * @param coverType The type of the cover to be displayed
     */
    fun hideContent(coverType: CoverType) {
        this.displayContent = false
        setCover(coverType)
    }

    /**
     * Attach the view to the presenter
     *
     * @param view View to attach
     */
    fun attachView(view: View) {
        this.view = view

        when(FLAVOR){
            "locket_touch_s" -> {
                coverBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.cover), view!!.getScreenSize(), view!!.getScreenSize(), false)
                coverWhiteBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.cover_white), view!!.getScreenSize(), view!!.getScreenSize(), false)
            }
            "locket_touch" -> {
                coverBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.cover), view!!.getScreenSize(), view!!.getScreenSize(), false)
                coverWhiteBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.cover_white), view!!.getScreenSize(), view!!.getScreenSize(), false)
            }
            "locket" -> {
                coverBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.cover), view!!.getScreenSize(), view!!.getScreenSize(), false)
                coverWhiteBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.cover_white), view!!.getScreenSize(), view!!.getScreenSize(), false)
            }
            "refind" -> {
                coverBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.refind_cover), view!!.getScreenSize(), view!!.getScreenSize(), false)
                coverWhiteBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(view!!.getContext().resources, R.drawable.refind_cover_white), view!!.getScreenSize(), view!!.getScreenSize(), false)
            }
        }
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
        fun updateBackgroundWithBitmap(bitmap: Bitmap)
        fun updateBackground(file: File, mediaType: MediaType)
        fun displayText(addr: String)
        fun getScreenSize(): Int
        fun getReady(): Boolean
        fun setReady(ready : Boolean)
        fun getContext(): Context

        /**
         * Types of media supported
         */
        enum class MediaType {
            IMAGE, GIF
        }
    }

    /**
     * Types of cover
     */
    enum class CoverType {
        BLACK, WHITE
    }



}