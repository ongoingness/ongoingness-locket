package uk.ac.ncl.openlab.ongoingness.views

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.recognisers.RevealRecogniser
import java.util.*

class OngoingnessActivity : Activity() {

    val TAG = "ONGOING"

    private val revealObserver = Observer { _, arg ->

        if(arg is RevealRecogniser.State){
            Log.d(TAG, arg.name)
        }else {

            macAddress.text = (arg as RevealRecogniser.Events).name
            Log.d(TAG, macAddress.text.toString())

            when (arg) {
                RevealRecogniser.Events.COVERED -> {
                }
                RevealRecogniser.Events.SHORT_REVEAL -> {
                }
                RevealRecogniser.Events.LONG_REVEAL -> {
                }
                RevealRecogniser.Events.STARTED -> {
                }
                RevealRecogniser.Events.STOPPED -> {
                }
            }
        }
    }

    private val revealRecogniser: RevealRecogniser = RevealRecogniser(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        macAddress.visibility = View.VISIBLE
        Glide.with(this).load(R.drawable.placeholder).into(image)

        revealRecogniser.addObserver(revealObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        revealRecogniser.deleteObserver(revealObserver)
    }

    override fun onResume() {
        super.onResume()
        revealRecogniser.start()
    }

    override fun onPause() {
        super.onPause()
        revealRecogniser.stop()
    }


}
