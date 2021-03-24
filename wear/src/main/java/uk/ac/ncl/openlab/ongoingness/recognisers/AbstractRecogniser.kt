package uk.ac.ncl.openlab.ongoingness.recognisers

import java.util.*

/**
 * Abstraction of a data recogniser. Analyses data from the watch sensors to trigger events. It is an observable.
 *
 * @author Luis Carvalho
 */
abstract class AbstractRecogniser : Observable() {

    /**
     * Start gathering data to be recognised.
     */
    abstract fun start()

    /**
     * Stop gathering data and terminate recogniser.
     */
    abstract fun stop()

    /**
     * Notifies a given event to its observables.
     *
     * @param event event of be notified.
     */
    fun notifyEvent(event: RecogniserEvent) {
        setChanged()
        notifyObservers(event)
    }

}