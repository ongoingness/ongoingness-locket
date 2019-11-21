package uk.ac.ncl.openlab.ongoingness.presenters

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.collections.ContentPiece
import uk.ac.ncl.openlab.ongoingness.collections.ContentType

import uk.ac.ncl.openlab.ongoingness.utilities.*
import java.io.File


/**
 * Connects the activity view to the media in the local database, allowing it to be displayed on screen.
 */
class Presenter(private val context: Context,
                private val coverBitmap: Bitmap,
                private val coverWhiteBitmap: Bitmap) {

    var view: View? = null

    fun displayContentPiece(contentPiece: ContentPiece) {
        view?.updateBackground(contentPiece.file, contentPiece.type, contentPiece.bitmapDrawable)
    }

    fun displayCover(type: CoverType) {
        when(type) {
            CoverType.BLACK -> view?.updateBackgroundWithBitmap(coverBitmap)
            CoverType.WHITE -> view?.updateBackgroundWithBitmap(coverWhiteBitmap)
        }
    }

    fun displayChargingCover(battery: Float) {
        view?.updateBackgroundWithBitmap(getAnewChargingBackground(battery, view!!.getScreenSize(), context))
    }

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
     * Display the devices mac address
     */
    fun displayCode() {
        val mac = getMacAddress()
        val txt = "Device Code:\n$mac"
        Log.d("MAC",mac)
        view?.displayText(txt)
    }


    /**
     * Control the view, must implement these methods
     */
    interface View {
        fun updateBackgroundWithBitmap(bitmap: Bitmap)
        fun updateBackground(file: File, contentType: ContentType, bitmap: BitmapDrawable)
        fun displayText(addr: String)
        fun getScreenSize(): Int
        fun finishActivity()
    }

}