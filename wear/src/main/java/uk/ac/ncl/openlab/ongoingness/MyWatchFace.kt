package uk.ac.ncl.openlab.ongoingness

import android.content.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.*


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

/**
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {
        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false
        private var mMuteMode: Boolean = false

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap
        private lateinit var mGrayBackgroundBitmap: Bitmap

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false

        private var mCanvas: Canvas = Canvas()

        /* Handler to update the time once a second in interactive mode. */
        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build())

            mCalendar = Calendar.getInstance()

            initializeBackground()
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.placeholder)
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
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

            if (!inAmbientMode) {
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
            }
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
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    // The user has completed the tap gesture.
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            mCanvas = canvas

            drawBackground(canvas)
        }

        /**
         * Draw the background of the watch face.
         * @param canvas
         */
        private fun drawBackground(canvas: Canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                // Draw the background when the watch is awake.
                val cw = ContextWrapper(applicationContext)
                val directory: File = cw.getDir("imageDir", Context.MODE_PRIVATE)
                val bitmap = getBitmapFromFile(directory.absolutePath)

                if (bitmap != null) {
                    val resized = Bitmap.createScaledBitmap(bitmap, 400, 400, false)
                    canvas.drawBitmap(resized, 0f, 0f, mBackgroundPaint)
                } else {
                    val placeholder = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.placeholder), 400, 400, false)
                    canvas.drawBitmap(placeholder, 0f, 0f, mBackgroundPaint)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        /**
         * Get the stored bitmap from file.
         *
         * @param path
         * @return Bitmap
         */
        private fun getBitmapFromFile(path: String): Bitmap? {
            try {
                val f = File(path, "last-image.png")
                return BitmapFactory.decodeStream(FileInputStream(f))
            } catch (e: FileNotFoundException) {
//                e.printStackTrace()
            }
            return null
        }
    }
}


