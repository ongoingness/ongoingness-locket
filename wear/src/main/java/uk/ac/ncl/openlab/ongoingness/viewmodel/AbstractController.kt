package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.recognisers.RecogniserEvent
import java.util.Observer

abstract class AbstractController(
        var context: Context,
        private var recogniser: AbstractRecogniser,
        private var presenter: AbstractPresenter) {

    private var currentState: State = State.UNKNOWN
    private var previousState: State = State.UNKNOWN

    private val recogniserObserver = Observer { _, arg ->

        when(arg) {

            RecogniserEvent.STARTED -> started()
            RecogniserEvent.AWAKE -> awake()
            RecogniserEvent.SLEEP -> sleep()
            RecogniserEvent.STOPPED -> stopped()
            RecogniserEvent.NEXT -> next()
            RecogniserEvent.UP -> up()
            RecogniserEvent.DOWN -> down()
            RecogniserEvent.TOWARDS -> towards()
            RecogniserEvent.AWAY -> away()
            RecogniserEvent.UNKNOWN -> unknown()
            RecogniserEvent.TAP -> tap()
            RecogniserEvent.LONG_PRESS -> longPress()

        }

    }

    fun updateState(state: State){
        synchronized(currentState) {
            if (state == currentState)
                return //no change, so no need to notify of change

            previousState = currentState
            currentState = state
        }
    }

    fun getCurrentState(): State {
        return currentState
    }

    fun getPreviousState(): State {
        return previousState
    }

    fun getRecogniser(): AbstractRecogniser {
        return recogniser
    }

    fun getPresenter(): AbstractPresenter{
        return presenter
    }

    abstract fun started()
    abstract fun awake()
    abstract fun sleep()
    abstract fun stopped()
    abstract fun next()
    abstract fun up()
    abstract fun down()
    abstract fun towards()
    abstract fun away()
    abstract fun unknown()
    abstract fun tap()
    abstract fun longPress()

}