package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import android.graphics.*
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.utilities.*

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
        watchFaceView?.updateBackgroundWithBitmap(getChargingBackground(battery))
    }

    /**
     * Attach the view to the presenter
     *
     * @param view View to attach
     */
    fun attachView(watchFaceView: WatchFaceView) {
        this.watchFaceView = watchFaceView
    }

    private fun getChargingBackground(battery: Float): Bitmap {

        //First layer
        val transparent = Bitmap.createBitmap(watchFaceView!!.getScreenSize(), watchFaceView!!.getScreenSize(),Bitmap.Config.ARGB_8888)
        val canvasT = Canvas(transparent)
        canvasT.drawColor(Color.BLACK)

        //Second Layer
        val mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.flower_pattern_white), watchFaceView!!.getScreenSize(), watchFaceView!!.getScreenSize(), false)

        //Third Layer
        val blue = Bitmap.createBitmap(watchFaceView!!.getScreenSize(), watchFaceView!!.getScreenSize(), Bitmap.Config.ARGB_8888)

        val canvasB = Canvas(blue)

        val circleSize = (battery * watchFaceView!!.getScreenSize()) / 2

        val circlePaint = Paint().apply { /*color = Color.parseColor("#009FE3")*/}
        //circlePaint.shader = LinearGradient(0f, 0f, 0f, screenSize.toFloat(), Color.BLACK, Color.parseColor("#009FE3"), Shader.TileMode.MIRROR)
        //circlePaint.shader = RadialGradient(screenSize / 2F, screenSize / 2F, circleSize + circleSize / 2, Color.parseColor("#009FE3"), Color.BLACK, Shader.TileMode.MIRROR)

        //circlePaint.style = Paint.Style.STROKE

        canvasB.drawCircle(watchFaceView!!.getScreenSize() / 2F, watchFaceView!!.getScreenSize() / 2F ,  circleSize,  circlePaint)

        val borderPaint = Paint().apply {color = Color.parseColor("#009FE3"); style = Paint.Style.STROKE; strokeWidth = 10f }
        canvasB.drawCircle(watchFaceView!!.getScreenSize() / 2F, watchFaceView!!.getScreenSize() / 2F ,  circleSize, borderPaint)



        val alphaPaint = Paint()
        alphaPaint.alpha = 250
        alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

        canvasB.drawBitmap(mBackgroundBitmap, Matrix(), alphaPaint)


        return overlayBitmaps(transparent, mBackgroundBitmap, blue, watchFaceView!!.getScreenSize())

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
    interface WatchFaceView {
        fun updateBackgroundWithBitmap(newBackground: Bitmap)
        fun getScreenSize(): Int
    }

}