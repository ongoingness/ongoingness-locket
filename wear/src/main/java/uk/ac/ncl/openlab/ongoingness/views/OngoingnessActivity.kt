package uk.ac.ncl.openlab.ongoingness.views

import android.app.Activity

import android.os.Bundle
import android.view.WindowManager
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.recognisers.LightRecogniser

class OngoingnessActivity : Activity() {

    val TAG = "ONGOING"

    /*
    private val revealObserver = Observer { _, arg ->

        if(arg is RevealRecogniserOld.State){
            Log.d(TAG, arg.name)
        }else {

            macAddress.text = (arg as RevealRecogniserOld.Events).name
            Log.d(TAG, macAddress.text.toString())

            when (arg) {
                RevealRecogniserOld.Events.STARTED -> {
                }
                RevealRecogniserOld.Events.COVERED -> {
                }
                RevealRecogniserOld.Events.AWAKE -> {
                }
                RevealRecogniserOld.Events.SHORT_REVEAL -> {
                }
                RevealRecogniserOld.Events.SLEEP -> {
                }
                RevealRecogniserOld.Events.STOPPED -> {
                }
            }
        }
    }

    private val revealRecogniser: RevealRecogniserOld = RevealRecogniserOld(this)
    */

    private val rec: LightRecogniser = LightRecogniser(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        var params: WindowManager.LayoutParams = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = params
        /*
               macAddress.visibility = View.VISIBLE
               Glide.with(this).load(R.drawable.placeholder).into(image)
               //Glide.with(this).load(R.drawable.test).into(image)

               revealRecogniser.addObserver(revealObserver)

               */
    }

    override fun onDestroy() {
        super.onDestroy()
        //revealRecogniser.deleteObserver(revealObserver)
    }

    override fun onResume() {
        super.onResume()
        //revealRecogniser.start()
        rec.start()
    }

    override fun onPause() {
        super.onPause()
        //revealRecogniser.stop()
        rec.stop()
    }


}
