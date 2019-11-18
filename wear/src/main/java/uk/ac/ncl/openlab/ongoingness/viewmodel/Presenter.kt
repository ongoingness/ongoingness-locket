package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import android.graphics.*
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.R

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
        view?.updateBackground(contentPiece.file, contentPiece.type)
    }

    fun displayCover(type: CoverType) {
        when(type) {
            CoverType.BLACK -> view?.updateBackgroundWithBitmap(coverBitmap)
            CoverType.WHITE -> view?.updateBackgroundWithBitmap(coverWhiteBitmap)
        }
    }

    fun displayChargingCover(battery: Float) {
        view?.updateBackgroundWithBitmap(getChargingBackground(battery))
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


    private fun getChargingBackground(battery: Float): Bitmap {

        //First layer
        val transparent = Bitmap.createBitmap(view!!.getScreenSize(), view!!.getScreenSize(),Bitmap.Config.ARGB_8888)
        val canvasT = Canvas(transparent)
        canvasT.drawColor(Color.BLACK)

        //Second Layer
        val mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.flower_pattern_white), view!!.getScreenSize(), view!!.getScreenSize(), false)

        //Third Layer
        val blue = Bitmap.createBitmap(view!!.getScreenSize(), view!!.getScreenSize(), Bitmap.Config.ARGB_8888)

        val canvasB = Canvas(blue)

        val circleSize = (battery * view!!.getScreenSize()) / 2

        val circlePaint = Paint().apply { /*color = Color.parseColor("#009FE3")*/}
        //circlePaint.shader = LinearGradient(0f, 0f, 0f, screenSize.toFloat(), Color.BLACK, Color.parseColor("#009FE3"), Shader.TileMode.MIRROR)
        //circlePaint.shader = RadialGradient(screenSize / 2F, screenSize / 2F, circleSize + circleSize / 2, Color.parseColor("#009FE3"), Color.BLACK, Shader.TileMode.MIRROR)

        //circlePaint.style = Paint.Style.STROKE

        canvasB.drawCircle(view!!.getScreenSize() / 2F, view!!.getScreenSize() / 2F ,  circleSize,  circlePaint)

        val borderPaint = Paint().apply {color = Color.parseColor("#009FE3"); style = Paint.Style.STROKE; strokeWidth = 10f }
        canvasB.drawCircle(view!!.getScreenSize() / 2F, view!!.getScreenSize() / 2F ,  circleSize, borderPaint)



        val alphaPaint = Paint()
        alphaPaint.alpha = 250
        alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

        canvasB.drawBitmap(mBackgroundBitmap, Matrix(), alphaPaint)


        val bitmap = overlayBitmaps(transparent, mBackgroundBitmap, blue, view!!.getScreenSize())

        return bitmap
        /*
        val bs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs)

        currentBitmapByteArray = bs.toByteArray()

        return currentBitmapByteArray

         */

    }

    private fun overlayBitmaps(b1: Bitmap, b2: Bitmap, b3: Bitmap, screenSize: Int): Bitmap {

        val bmOverlay: Bitmap  = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmOverlay)

        val secondLayerAlphaPaint = Paint()
        secondLayerAlphaPaint.alpha = 80

        val alphaPaint = Paint()
        alphaPaint.alpha = 250
        alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(b1, Matrix(), null)
        canvas.drawBitmap(b2, Matrix(), secondLayerAlphaPaint)
        canvas.drawBitmap(b3, Matrix(), null)

        return Bitmap.createScaledBitmap(bmOverlay, screenSize, screenSize, false)

    }



    /**
     * Control the view, must implement these methods
     */
    interface View {
        fun updateBackgroundWithBitmap(bitmap: Bitmap)
        fun updateBackground(file: File, contentType: ContentType)
        fun displayText(addr: String)
        fun getScreenSize(): Int
        fun finishActivity()
    }


}