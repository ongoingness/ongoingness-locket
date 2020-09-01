package uk.ac.ncl.openlab.ongoingness

import android.content.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.work.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
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
class WatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : CanvasWatchFaceService.Engine(), WatchFacePresenter.WatchFaceView {

        private var mMuteMode: Boolean = false

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false

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


            setRemoteConfig()

            Logger.start(applicationContext)
            Logger.log(LogType.STARTED_WATCHFACE, listOf(), applicationContext)

            setWorkManager()

            when(FLAVOR) {

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

                "locket_touch_s" -> {

                    val presenter = WatchFacePresenter(applicationContext,
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover), getScreenSize(), getScreenSize(), false),
                            Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.cover_white), getScreenSize(), getScreenSize(), false))
                    presenter.attachView(this)
                    presenter.displayCover(CoverType.BLACK)

                    controller = WatchFaceController(applicationContext, false, presenter)

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
            mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            this.mAmbient = inAmbientMode

            if (!inAmbientMode)
                controller.ambientModeChanged()
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
         * @param canvas
         */
        private fun drawBackground(canvas: Canvas) {

            mBackgroundPaint = Paint().apply {
                color = Color.RED
            }

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else if (mAmbient) {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                invalidate()
            }
        }

        /**
         * Get the screen size of a device.
         */
        override fun getScreenSize(): Int {
            val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            return size.x
        }


        private fun setWorkManager() {
            WorkManager.getInstance(applicationContext).cancelAllWork()
            addPullMediaPushLogsWorkRequest(applicationContext)
        }

        private fun setRemoteConfig() {
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 60
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }
}


