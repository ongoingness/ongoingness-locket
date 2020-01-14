package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser

class MomentoController(context: Context,
                        recogniser: AbstractRecogniser,
                        presenter: Presenter,
                        contentCollection: AbstractContentCollection,
                        val statedWithTap: Boolean,
                        val faceState: String,
                        val battery: Float) : AbstractController(context, recogniser, presenter, contentCollection) {

    override fun setStartingState() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartedEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStoppedEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUpEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDownEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTowardsEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAwayEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUnknownEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTapEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLongPressEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onChargerConnectedEvent(battery: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onChargerDisconnectedEvent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBatteryChangedEvent(battery: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRotateUp() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRotateDown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}