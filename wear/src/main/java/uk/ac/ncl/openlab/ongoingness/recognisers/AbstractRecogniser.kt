package uk.ac.ncl.openlab.ongoingness.recognisers

import android.content.Context
import java.util.*

abstract class AbstractRecogniser(val content: Context) : Observable() {

    abstract fun start()

    abstract fun stop()

    fun notifyEvent(event: RecogniserEvent) {
        setChanged()
        notifyObservers(event)
    }

}