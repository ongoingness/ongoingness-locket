package uk.ac.ncl.openlab.ongoingness.presenters

import android.content.Context
import android.graphics.*
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.utilities.getAnewChargingBackground
import uk.ac.ncl.openlab.ongoingness.utilities.getChargingBackground

/**
 * Connects the activity view to the media in the local database, allowing it to be displayed on screen.
 */
class WatchFacePresenter(private val context: Context,
                private val coverBitmap: Bitmap,
                private val coverWhiteBitmap: Bitmap) {

    var watchFaceView: WatchFaceView? = null

    fun displayCover(type: CoverType) {
        when(type) {
            CoverType.BLACK -> watchFaceView?.updateBackgroundWithBitmap(coverBitmap)
            CoverType.WHITE -> watchFaceView?.updateBackgroundWithBitmap(coverWhiteBitmap)
        }
    }

    fun displayChargingCover(battery: Float) {
        watchFaceView?.updateBackgroundWithBitmap(getChargingBackground(battery, watchFaceView!!.getScreenSize(), context))
    }

    /**
     * Attach the view to the presenter
     *
     * @param view View to attach
     */
    fun attachView(watchFaceView: WatchFaceView) {
        this.watchFaceView = watchFaceView
    }

    /**
     * Control the view, must implement these methods
     */
    interface WatchFaceView {
        fun updateBackgroundWithBitmap(newBackground: Bitmap)
        fun getScreenSize(): Int
    }

}