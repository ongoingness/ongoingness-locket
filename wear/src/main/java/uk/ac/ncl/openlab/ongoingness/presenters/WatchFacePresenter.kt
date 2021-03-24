package uk.ac.ncl.openlab.ongoingness.presenters

import android.content.Context
import android.graphics.*
import uk.ac.ncl.openlab.ongoingness.utilities.getChargingBackground

/**
 * Connects the watchface view to rest of the app, allowing content to be displayed on screen.
 *
 * @param context context of the application
 * @param coverBitmap bitmap to be displayed as the cover when the device is not in use.
 * @param coverWhiteBitmap bitmap to be displayed as the cover when de device is fetching data.
 *
 * @author Luis Carvalho
 */
class WatchFacePresenter(private val context: Context,
                private val coverBitmap: Bitmap,
                private val coverWhiteBitmap: Bitmap) {

    /**
     * View where content is rendered to.
     */
    private var watchFaceView: WatchFaceView? = null

    /**
     * Display a cover given a type.
     *
     * @param type type of cover to be displayed.
     */
    fun displayCover(type: CoverType) {
        when(type) {
            CoverType.BLACK -> watchFaceView?.updateBackgroundWithBitmap(coverBitmap)
            CoverType.WHITE -> watchFaceView?.updateBackgroundWithBitmap(coverWhiteBitmap)
        }
    }

    /**
     * Display charging cover given a battery value.
     *
     * @param battery the quantity of battery to be displayed.
     */
    fun displayChargingCover(battery: Float) {
        watchFaceView?.updateBackgroundWithBitmap(getChargingBackground(battery, watchFaceView!!.getScreenSize(), context))
    }

    /**
     * Attach the view to the presenter.
     *
     * @param watchFaceView View to attach.
     */
    fun attachView(watchFaceView: WatchFaceView) {
        this.watchFaceView = watchFaceView
    }

    /**
     * Control the view, must implement these methods.
     */
    interface WatchFaceView {
        fun updateBackgroundWithBitmap(newBackground: Bitmap)
        fun getScreenSize(): Int
    }

}