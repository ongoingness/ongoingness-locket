package uk.ac.ncl.openlab.ongoingness

import android.content.*
import android.graphics.*
import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.work.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.utilities.*
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.controllers.WatchFaceController
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.presenters.WatchFacePresenter

/**
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */

/**
 * Main watch face class. Chooses what code to run based on the selected flavour.
 * In charge of starting the presenter class and hooking it to a controller.
 *
 * @author Luis Carvalho
 */
class WatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : CanvasWatchFaceService.Engine(), WatchFacePresenter.WatchFaceView {

        private var mMuteMode: Boolean = false

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap

        private lateinit var controller: WatchFaceController

        private var falseStartReleaseInterval = 4000L
        private var previousTapTime: Long? = null

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@WatchFace)
                    .setAcceptsTapEvents(true)
                    .setHideStatusBar(true)
                    .setShowUnreadCountIndicator(false)
                    .setHideNotificationIndicator(true)
                    .build())

            //Start the logger
            Logger.start(applicationContext)
            Logger.log(LogType.STARTED_WATCHFACE, listOf(), applicationContext)

            //Set the workers to pull and push data
            setWorkManager()

            when(FLAVOR) {

                //Anew
                "locket_touch", "locket_touch_inverted" -> {

                    val presenter = WatchFacePresenter(applicationContext,
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover), getScreenSize(), getScreenSize(), false),
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover_white), getScreenSize(), getScreenSize(), false))
                    presenter.attachView(this)
                    presenter.displayCover(CoverType.BLACK)

                    controller = WatchFaceController(applicationContext, true, presenter)

                }

                "refind" -> {

                    val presenter = WatchFacePresenter(applicationContext,
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.refind_cover), getScreenSize(), getScreenSize(), false),
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.refind_cover_white), getScreenSize(), getScreenSize(), false))
                    presenter.attachView(this)
                    presenter.displayCover(CoverType.BLACK)

                    controller = WatchFaceController(applicationContext, false, presenter)

                }

                //Ivvor
                "locket_touch_s" -> {

                    val presenter = WatchFacePresenter(applicationContext,
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.s_cover), getScreenSize(), getScreenSize(), false),
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.s_cover_white), getScreenSize(), getScreenSize(), false))
                    presenter.attachView(this)
                    presenter.displayCover(CoverType.BLACK)

                    controller = WatchFaceController(applicationContext, true, presenter)

                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            controller.stop()
            Logger.log(LogType.STOPPED_WATCHFACE, listOf(), applicationContext)
        }

        override fun updateBackgroundWithBitmap(newBackground: Bitmap) {
            mBackgroundBitmap = newBackground
            invalidate()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            val mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            val mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)

            controller.lowBitAmbientChanged(mLowBitAmbient)
            controller.burnInProtectionChanged(mBurnInProtection)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            controller.ambientModeChanged(inAmbientMode)
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode
                invalidate()
            }
        }

        /**
         * Captures onTapEvent event (and onTapEvent type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {

            if(previousTapTime == null || (previousTapTime != null && eventTime - previousTapTime!! > falseStartReleaseInterval)) {
                previousTapTime = eventTime
                controller.tapEvent()
            }

        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            drawBackground(canvas)
        }

        /**
         * Draw the background of the watch face.
         *
         * @param canvas canvas to be drawn.
         */
        private fun drawBackground(canvas: Canvas) {

            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                invalidate()
            }
        }

        /**
         * Get the screen size of a device.
         * @return the size of the screen.
         */
        override fun getScreenSize(): Int {
            val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            return size.x
        }

        /**
         * Starts by cancelling all scheduled workers and schedules a new one.
         */
        private fun setWorkManager() {
            WorkManager.getInstance(applicationContext).cancelAllWork()
            if(isLogging(applicationContext))
                addPullMediaPushLogsWorkRequest(applicationContext)
        }

    }
}


