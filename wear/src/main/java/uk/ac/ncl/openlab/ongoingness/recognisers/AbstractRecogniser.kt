package uk.ac.ncl.openlab.ongoingness.recognisers

import android.content.Context
import android.util.Log
import java.util.*

abstract class AbstractRecogniser(val content: Context) : Observable() {

    abstract fun start()

    abstract fun stop()

    fun notifyEvent(event: RecogniserEvent) {
        setChanged()

        Log.d("Recognizer", "$event")

        notifyObservers(event)
    }

}