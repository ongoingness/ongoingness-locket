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
import kotlin.math.abs

const val BROADCAST_INTENT_NAME: String = "BATTERY_INFO"

class BatteryInfoReceiver(private var context: Context, private var screenSize: Int) {

    private var chargeReceiver: BroadcastReceiver
    private var batteryReceiver: BroadcastReceiver

    private var lastBatteryValue: Float = 0f
    private var checkingChargeReceiverOn = false
    private var checkingBatteryReceiverOn: Boolean = false

    private val chargeDelta: Float = 0.01f
    
    private val tag = "BatteryInfoReceiver"
    var currentBitmapByteArray: ByteArray

    private var n = 0

    init {
        this.currentBitmapByteArray = getDefaultCover()
        this.chargeReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                if(intent.action == Intent.ACTION_POWER_CONNECTED) {
                    Log.d(tag, " Connected")
                    Logger.log(LogType.CHARGER_CONNECTED, listOf(), context)
                    startCheckingBatteryValues()
                } else if(intent.action == Intent.ACTION_POWER_DISCONNECTED) {
                    Log.d(tag, " Disconnected")
                    Logger.log(LogType.CHARGER_DISCONNECTED, listOf(), context)
                    if(checkingBatteryReceiverOn)
                        stopCheckingBatteryValues()
                    sendBackgroundBroadcast(getDefaultCover(), false)
                }

            }
        }
        this.batteryReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                val batteryPct: Float? = intent.let { i ->
                    val level: Int = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    level / scale.toFloat()
                }

                Log.d(tag, "Battery $batteryPct")

                if(batteryPct != null && abs(batteryPct - lastBatteryValue) > chargeDelta) {
                    sendBackgroundBroadcast(getChargingBackground(batteryPct), true)
                    lastBatteryValue = batteryPct
                }
            }
        }
    }

    private fun getDefaultCover(): ByteArray {
        val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.cover), screenSize, screenSize, false)
        val bs = ByteArrayOutputStream()
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

    private fun sendBackgroundBroadcast(bitmap: ByteArray, chargingState: Boolean) {
        val broadcastIntent = Intent(BROADCAST_INTENT_NAME)
        broadcastIntent.putExtra("background", bitmap)
        broadcastIntent.putExtra("chargingState", chargingState)
        context.sendBroadcast(broadcastIntent)

        Log.d(tag, "Broadcast sent $n")
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
        val transparent = Bitmap.createBitmap(screenSize, screenSize,Bitmap.Config.ARGB_8888)
        val canvasT = Canvas(transparent)
        canvasT.drawColor(Color.BLACK)

        //Second Layer
        val mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.flower_pattern_white), screenSize, screenSize, false)

        //Third Layer
        val blue = Bitmap.createBitmap(screenSize, screenSize, Bitmap.Config.ARGB_8888)

        val canvasB = Canvas(blue)

        val circleSize = (battery * screenSize) / 2

        val circlePaint = Paint().apply { /*color = Color.parseColor("#009FE3")*/}
        //circlePaint.shader = LinearGradient(0f, 0f, 0f, screenSize.toFloat(), Color.BLACK, Color.parseColor("#009FE3"), Shader.TileMode.MIRROR)
        //circlePaint.shader = RadialGradient(screenSize / 2F, screenSize / 2F, circleSize + circleSize / 2, Color.parseColor("#009FE3"), Color.BLACK, Shader.TileMode.MIRROR)

        //circlePaint.style = Paint.Style.STROKE

        canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize,  circlePaint)

        val borderPaint = Paint().apply {color = Color.parseColor("#009FE3"); style = Paint.Style.STROKE; strokeWidth = 10f }
        canvasB.drawCircle(screenSize / 2F, screenSize / 2F ,  circleSize, borderPaint)



        val alphaPaint = Paint()
        alphaPaint.alpha = 250
        alphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

        canvasB.drawBitmap(mBackgroundBitmap, Matrix(), alphaPaint)


        val bitmap = overlayBitmaps(transparent, mBackgroundBitmap, blue, screenSize)

        val bs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs)

        currentBitmapByteArray = bs.toByteArray()

        return currentBitmapByteArray

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



}