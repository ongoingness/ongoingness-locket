package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.utilities.*
import java.io.File
import java.lang.Exception
import kotlin.collections.ArrayList

class MainPresenter {

    private var view: View? = null
    private var context: Context? = null
    private var mediaCollection: List<WatchMedia>? = null
    private var currentIndex = 0;
    private lateinit var watchMediaViewModel: WatchMediaViewModel
    private var displayContent: Boolean = false

    private var coverWhiteBitmap: Bitmap? = null
    private var coverBitmap: Bitmap? = null

    fun setWatchMediaRepository(activity: FragmentActivity) {

        watchMediaViewModel = ViewModelProviders.of(activity).get(WatchMediaViewModel::class.java)

        watchMediaViewModel.allWatchMedia.observe(activity, Observer { watchMedia ->
            mediaCollection = watchMedia
            if(displayContent)
                setNewBitmap(mediaCollection)
        })

    }

    fun pullingData(state: Boolean) {
        if(state) {
            displayContent = false
            view?.updateBackground(coverWhiteBitmap!!)
        } else {
            displayContent()
        }
    }

    fun goToNextImage(){
        currentIndex++
        if(displayContent) {

            setNewBitmap(mediaCollection)
            Log.d("Presenter", "Going to next image")
        } else {
            setScreenBlack(1)
        }
    }

    fun goToPreviousImage() {
        currentIndex--
        if(displayContent) {
            setNewBitmap(mediaCollection)
            Log.d("Presenter", "Going to previous image")
        } else {
            setScreenBlack(1)
        }
    }

    private fun setScreenBlack(type: Int) {
        if(type == 1)
            view?.updateBackground(coverBitmap!!)
        else
            view?.updateBackground(coverWhiteBitmap!!)
        view?.setReady(true)
    }

    private fun setNewBitmap(localCollection: List<WatchMedia>?) {
        var bitmap: Bitmap? = null
        if (localCollection.isNullOrEmpty() || !displayContent) {
            bitmap = coverBitmap
            currentIndex = 0
        } else {
            if(currentIndex >= localCollection.size)
                currentIndex %= localCollection.size
            else if(currentIndex < 0)
                currentIndex = localCollection.size-1
            bitmap = getBitmapFromFile(this.context!!, localCollection[currentIndex].path)
        }

        if(bitmap == null) {
            //Just in case something goes wrong with the file
            watchMediaViewModel.delete(localCollection!![currentIndex], view!!.getContext())
            goToNextImage()
        } else {
            view?.updateBackground(bitmap!!)
            view?.setReady(true)
        }
    }

    fun displayContent() {
        this.displayContent = true
        setNewBitmap(mediaCollection)
    }

    fun hideContent(type: Int) {
        this.displayContent = false
        setScreenBlack(type)
    }

    /**
     * Attach the view to the presenter
     *
     * @param view View to attach
     */
    fun attachView(view: View) {
        this.view = view

        when(FLAVOR){
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
        fun updateBackground(bitmap: Bitmap)
        fun displayText(addr: String)
        fun getScreenSize(): Int
        fun getReady(): Boolean
        fun setReady(ready : Boolean)
        fun getContext(): Context
    }
}