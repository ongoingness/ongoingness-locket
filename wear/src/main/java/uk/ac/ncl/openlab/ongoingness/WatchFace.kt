package uk.ac.ncl.openlab.ongoingness

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.ActivityCompat.startActivityForResult
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.utilities.*
import uk.ac.ncl.openlab.ongoingness.views.MainActivity
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass


// The authority for the sync adapter's content provider
const val AUTHORITY = "uk.ac.ncl.openlab.ongoingness"
// An account type, in the form of a domain name
const val ACCOUNT_TYPE = "ongoingness.com"
// The account name
const val ACCOUNT = "ongoingnessaccount"

// Sync interval constants
const val SECONDS_PER_MINUTE = 60L
const val SYNC_INTERVAL_IN_MINUTES = 60L
const val SYNC_INTERVAL = 5L //SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE

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

    inner class Engine : CanvasWatchFaceService.Engine() {
        private var mMuteMode: Boolean = false
        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap
        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false

        private lateinit var  batteryInfoReceiver: BatteryInfoReceiver
        private lateinit var bitmapReceiver: BroadcastReceiver

        private lateinit var mAccount: Account
        private lateinit var mResolver: ContentResolver

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@WatchFace)
                    .setAcceptsTapEvents(true)
                    .setHideStatusBar(true)
                    .setShowUnreadCountIndicator(false)
                    .setHideNotificationIndicator(true)
                    .build())


            // Create the dummy account
            mAccount = createSyncAccount()

            mResolver = contentResolver
            /*
             * Turn on periodic syncing
             */
            ContentResolver.addPeriodicSync(
                    mAccount,
                    AUTHORITY,
                    Bundle.EMPTY,
                    SYNC_INTERVAL)

            Logger.start(applicationContext)

            initializeBackground()

            when(FLAVOR) {
                "locket_touch" -> {
                    batteryInfoReceiver = BatteryInfoReceiver(applicationContext, getScreenSize())
                    batteryInfoReceiver.start()

                    bitmapReceiver = object
                        : BroadcastReceiver() {

                        override fun onReceive(context: Context, intent: Intent) {

                            if (intent.hasExtra("background")) {
                                var bitmap = BitmapFactory.decodeByteArray(
                                        intent.getByteArrayExtra("background"), 0,
                                        intent.getByteArrayExtra("background").size)

                                mBackgroundBitmap = bitmap

                                invalidate()

                            }
                        }
                    }
                    val filter = IntentFilter(BROADCAST_INTENT_NAME).apply {}
                    registerReceiver(bitmapReceiver, filter)
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            when(FLAVOR) {
                "locket_touch" -> {
                    batteryInfoReceiver.stop()
                    unregisterReceiver(bitmapReceiver)
                }
            }
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.RED
            }
            mBackgroundBitmap = getCoverBitmap()
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
                launchActivity()
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
            launchActivity()
        }


        override fun onDraw(canvas: Canvas, bounds: Rect) {
            drawBackground(canvas)
        }

        /**
         * Draw the background of the watch face.
         * @param canvas
         */
        private fun drawBackground(canvas: Canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else if (mAmbient) {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            }
        }

        private fun getCoverBitmap(): Bitmap {

            var coverID: Int? = null

            when(FLAVOR){
                "locket" ->{ coverID = R.drawable.cover }
                "locket_touch" ->{ coverID = R.drawable.cover }
                "refind" -> { coverID = R.drawable.refind_cover }
            }

            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(applicationContext.resources, coverID!! ), getScreenSize(), getScreenSize(), false)
            return bitmap ?: Bitmap.createBitmap(getScreenSize(), getScreenSize(), Bitmap.Config.ARGB_8888)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                invalidate()
            }
        }

        private fun launchActivity() {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            when(FLAVOR) {
                "locket_touch" -> {
                    intent.putExtra("background", batteryInfoReceiver.currentBitmapByteArray)
                    intent.putExtra("broadcastName", BROADCAST_INTENT_NAME)
                }
            }

            startActivity(intent)
        }

        /**
         * Get the screen size of a device.
         */
        private fun getScreenSize(): Int {
            val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            return size.x
        }

        /**
         * Create a new dummy account for the sync adapter
         */
        private fun createSyncAccount(): Account {
            val accountManager = getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
            return Account(ACCOUNT, ACCOUNT_TYPE).also { newAccount ->
                /*
                 * Add the account and account type, no password or user data
                 * If successful, return the Account object, otherwise report an error.
                 */
                if (accountManager.addAccountExplicitly(newAccount, null, null)) {
                    /*
                     * If you don't set android:syncable="true" in
                     * in your <provider> element in the manifest,
                     * then call context.setIsSyncable(account, AUTHORITY, 1)
                     * here.
                     */
                } else {
                    /*
                     * The account exists or some other error occurred. Log this, report it,
                     * or handle it internally.
                     */
                }
            }
        }

    }
}


