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
 * Connects the activity view to rest of the app, allowing content to be displayed on screen.
 *
 * @param context context of the application
 * @param coverBitmap bitmap to be displayed as the cover when the device is not in use.
 * @param coverWhiteBitmap bitmap to be displayed as the cover when de device is fetching data.
 *
 * @author Luis Carvalho
 */
class Presenter(private val context: Context,
                private val coverBitmap: Bitmap,
                private val coverWhiteBitmap: Bitmap) {

    /**
     * View where content is rendered to.
     */
    var view: View? = null

    /**
     * Displays the content in screen given a ContentPiece object.
     *
     * @param contentPiece Object containing the file to displayed.
     */
    fun displayContentPiece(contentPiece: ContentPiece) {
        view?.updateBackground(contentPiece.file, contentPiece.type, contentPiece.bitmapDrawable)
    }

    /**
     * Display a cover given a type.
     *
     * @param type type of cover to be displayed.
     */
    fun displayCover(type: CoverType) {
        when(type) {
            CoverType.BLACK -> view?.updateBackgroundWithBitmap(coverBitmap)
            CoverType.WHITE -> view?.updateBackgroundWithBitmap(coverWhiteBitmap)
        }
    }

    /**
     * Display charging cover given a battery value.
     *
     * @param battery the quantity of battery to be displayed.
     */
    fun displayChargingCover(battery: Float) {
        view?.updateBackgroundWithBitmap(getChargingBackground(battery, view!!.getScreenSize(), context))
    }

    /**
     * Display warning screen, containing the device WIFI mac address.
     */
    fun displayWarning() {
        view?.updateBackgroundWithBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.problem), view!!.getScreenSize(), view!!.getScreenSize(), false))
        view?.displayText(getMacAddress())
    }


    /**
     * Attach the view to the presenter.
     * @param view View to attach.
     */
    fun attachView(view: View) {
        this.view = view
    }

    /**
     * Detach the view from the presenter.
     * Call this on view's onDestroy method.
     */
    fun detachView() {
        this.view = null
    }


    /**
     * Display the devices WIFI mac address.
     */
    fun displayCode() {
        val mac = getMacAddress()
        val txt = "Device Code:\n$mac"
        Log.d("MAC",mac)
        view?.displayText(txt)
    }


    /**
     * Control the view, must implement these methods.
     */
    interface View {
        fun updateBackgroundWithBitmap(bitmap: Bitmap)
        fun updateBackground(file: File, contentType: ContentType, bitmap: BitmapDrawable)
        fun displayText(addr: String)
        fun getScreenSize(): Int
        fun finishActivity()
    }

}