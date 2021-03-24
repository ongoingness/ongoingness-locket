package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.utilities.isLogging
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsAsyncTask

/**
 * Controller used by the Refind flavour.
 *
 * @param pullContentOnWake flag to decide if the app pulls data from the server on start.
 * @author Luis Carvalho
 */
class RefindController(context: Context,
                       recogniser: AbstractRecogniser,
                       presenter: Presenter,
                       contentCollection: AbstractContentCollection,
                       private val pullContentOnWake: Boolean)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    /**
     * Registers if this controller has got data from the server already.
     */
    var gotData = false

    override fun onRotateUp() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {

                updateKillThread(System.currentTimeMillis())
                val content = getContentCollection().goToNextContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
            }

            else -> {}

        }
    }

    override fun onRotateDown() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {

                updateKillThread(System.currentTimeMillis())
                val content = getContentCollection().goToPreviousContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
            }

            else -> {}

        }

    }

    override fun onStartedEvent() {

        if(pullContentOnWake && !gotData && hasConnection(context) && isLogging(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().setup()
                val content = getContentCollection().getCurrentContent()
                if(content != null) {
                    getContentCollection().startLoggingFields(content)
                    getPresenter().displayContentPiece(content)
                }
                stopKillThread()
                Logger.log(LogType.WAKE_UP, listOf(), context)

                startKillThread(1000L * 30 * 1  , 5000L)

                updateState(ControllerState.ACTIVE)
            }

            getPresenter().displayCover(CoverType.WHITE)
            PullMediaPushLogsAsyncTask(postExecuteCallback = postExecuteCallback).execute(context)
            updateState(ControllerState.PULLING_DATA)

        } else {
            getContentCollection().restartIndex()
            val content = getContentCollection().getCurrentContent()
            if(content != null) {
                getContentCollection().startLoggingFields(content)
                getPresenter().displayContentPiece(content)
            }
            startKillThread(1000L * 30 * 1  , 5000L)
            updateState(ControllerState.ACTIVE)
        }

    }

    override fun setStartingState() {}
    override fun onStoppedEvent() {}
    override fun onUpEvent() {}
    override fun onDownEvent() {}
    override fun onTowardsEvent() {}
    override fun onAwayEvent() {}
    override fun onUnknownEvent() {}
    override fun onTapEvent() {}
    override fun onLongPressEvent() {}
    override fun onChargerConnectedEvent(battery: Float) {}
    override fun onChargerDisconnectedEvent() {}
    override fun onBatteryChangedEvent(battery: Float) {}
    override fun onRotateLeft() {}
    override fun onRotateRight() {}
    override fun onAwayLeft() {}
    override fun onAwayRight() {}
    override fun onAwayTowards() {}

}


