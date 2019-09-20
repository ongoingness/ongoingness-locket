package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.BatteryManager
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.R
import java.io.ByteArrayOutputStream

const val BROADCAST_INTENT_NAME: String = "BATTERY_INFO"

class BatteryInfoReceiver {

    private var chargeReceiver: BroadcastReceiver
    private var batteryReceiver: BroadcastReceiver
    private var context: Context
    private var screenSize: Int

    private var lastBatteryValue: Float = 0f
    private var checkingChargeReceiverOn = false
    private var checkingBatteryReceiverOn: Boolean = false

    private val CHARGE_DELTA: Float = 0.01f


    private val TAG = "BatteryInfoReceiver"
    var currentBitmapByteArray: ByteArray

    private var n = 0

    constructor(context: Context, screenSize: Int) {
        this.context = context
        this.screenSize = screenSize
        this.currentBitmapByteArray = getDefaultCover()

        this.chargeReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                if(intent.action == Intent.ACTION_POWER_CONNECTED) {
                    Log.d(TAG, " Connected")
                    startCheckingBatteryValues()

                } else if(intent.action == Intent.ACTION_POWER_DISCONNECTED) {
                    Log.d(TAG, " Disconnected")
                    if(checkingBatteryReceiverOn)
                        stopCheckingBatteryValues()
                    sendBackgroundBroadcast(getDefaultCover())
                }

            }
        }

        this.batteryReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                val batteryPct: Float? = intent?.let { intent ->
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    level / scale.toFloat()
                }

                Log.d(TAG, "Battery $batteryPct")

                if(batteryPct != null && Math.abs(batteryPct - lastBatteryValue) > CHARGE_DELTA) {
                    sendBackgroundBroadcast(getChargingBackground(batteryPct))
                    lastBatteryValue = batteryPct
                }
            }
        }

    }

    private fun getDefaultCover(): ByteArray {
        var bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.cover), screenSize, screenSize, false)
        var bs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs)
        currentBitmapByteArray = bs.toByteArray()
        return currentBitmapByteArray
    }

    fun start() {
        startCheckingChargeState()
    }

    fun stop() {
        if(checkingChargeReceiverOn)
            stopCheckingChargeState()
        if(checkingBatteryReceiverOn)
            stopCheckingBatteryValues()
    }

    private fun sendBackgroundBroadcast(bitmap: ByteArray) {
        var broadcastIntent = Intent(BROADCAST_INTENT_NAME)
        broadcastIntent.putExtra("background", bitmap)
        context.sendBroadcast(broadcastIntent)



        Log.d(TAG, "Broadcast sent $n")
        n++

    }

    private fun startCheckingChargeState() {

        val filter = IntentFilter(BatteryManager.ACTION_CHARGING).apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(chargeReceiver, filter)
        checkingChargeReceiverOn = true

    }

    private fun stopCheckingChargeState() {
        context.unregisterReceiver(chargeReceiver)
        checkingChargeReceiverOn = false
    }

    private fun startCheckingBatteryValues() {

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {}
        context.registerReceiver(batteryReceiver, filter)
        checkingBatteryReceiverOn = true

    }

    private fun stopCheckingBatteryValues() {
        context.unregisterReceiver(batteryReceiver)
        checkingBatteryReceiverOn = false
        lastBatteryValue = 0f
    }

    private fun getChargingBackground(battery: Float): ByteArray {

        //First layer
        var transparent = Bitmap.createBitmap(screenSize, screenSize,Bitmap.Config.ARGB_8888)
        var canvasT = Canvas(transparent)
        canvasT.drawColor(Color.BLACK)

        //Second Layer
        var mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.flower_pattern_white), screenSize, screenSize, false)

        //Third Layer
        var blue = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)

        var canvasB = Canvas(blue)

        var circleSize = (battery * screenSize) / 2

        var circlePaint = Paint().apply { color = Color.parseColor("#009FE3")}
        //circlePaint.shader = LinearGradient(0f, 0f, 0f, screenSize.toFloat(), Color.BLACK, Color.parseColor("#009FE3"), Shader.TileMode.MIRROR)
        circlePaint.shader = RadialGradient(screenSize / 2F, screenSize / 2F, circleSize + circleSize / 2, Color.parseColor("#009FE3"), Color.BLACK, Shader.TileMode.MIRROR)

        canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize,  circlePaint)

        var alphaPaint = Paint()
        alphaPaint.alpha = 250
        alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

        canvasB.drawBitmap(mBackgroundBitmap, Matrix(), alphaPaint)

        var bitmap = overlayBitmaps(transparent, mBackgroundBitmap, blue, screenSize)

        var bs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs);

        currentBitmapByteArray = bs.toByteArray()

        return currentBitmapByteArray

    }

    private fun overlayBitmaps(b1: Bitmap, b2: Bitmap, b3: Bitmap, screenSize: Int): Bitmap {

        var bmOverlay: Bitmap  = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bmOverlay)

        var secondLayerAlphaPaint = Paint()
        secondLayerAlphaPaint.alpha = 80

        var alphaPaint = Paint()
        alphaPaint.alpha = 250
        alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(b1, Matrix(), null)
        canvas.drawBitmap(b2, Matrix(), secondLayerAlphaPaint)
        canvas.drawBitmap(b3, Matrix(), null)

        return Bitmap.createScaledBitmap(bmOverlay, screenSize, screenSize, false)

    }



}