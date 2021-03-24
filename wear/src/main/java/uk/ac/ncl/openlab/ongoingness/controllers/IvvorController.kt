package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.utilities.isLogging
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsAsyncTask

/**
 * Controller used by the Ivvor flavour.
 *
 * @param faceState the state of the watch face at the start of the app.
 * @param battery the battery level at the start of the app.
 * @param pullContentOnWake flag to decide if the app pulls data from the server on start.
 * @author Luis Carvalho
 */
class IvvorController(context: Context,
                        recogniser: AbstractRecogniser,
                        presenter: Presenter,
                        contentCollection: AbstractContentCollection,
                        val faceState: String,
                        val battery: Float,
                      private val pullContentOnWake: Boolean) : AbstractController(context, recogniser, presenter, contentCollection) {

    /**
     * Registers if this controller has got data from the server already.
     */
    private var gotData = false

    override fun onStartedEvent() {

        startKillThread(30 * 1000L, 10 * 60 * 1000L)

        if(faceState == ControllerState.CHARGING.toString()) {
            stopKillThread()
            getPresenter().view!!.finishActivity()
        } else {
            getPresenter().displayCover(CoverType.BLACK)
            updateState(ControllerState.STANDBY)
        }

    }

    override fun onTowardsEvent() {

        when(getCurrentState()) {

            ControllerState.STANDBY -> awakeUpProcedures()
            else -> {}
        }

    }

    override fun onAwayEvent() {
        turnOff()
    }

    override fun onChargerConnectedEvent(battery: Float) {
        turnOff()
    }

    override fun onChargerDisconnectedEvent() {
        turnOff()
    }

    override fun onBatteryChangedEvent(battery: Float) {
        turnOff()
    }

    override fun onAwayLeft() {
        when(getCurrentState()) {
            ControllerState.READY -> awakeUpProcedures()
            ControllerState.ACTIVE -> nextImage()
            else ->  { }
        }
    }

    override fun onAwayRight() {
        when(getCurrentState()) {
            ControllerState.READY -> awakeUpProcedures()
            ControllerState.ACTIVE -> nextImage()
            else ->  { }
        }
    }

    override fun onAwayTowards() {
        when(getCurrentState()) {
            ControllerState.ACTIVE -> nextImage()
            else -> {
            }
        }
    }

    override fun setStartingState() {}
    override fun onStoppedEvent() {}
    override fun onUpEvent() {}
    override fun onDownEvent() {}
    override fun onUnknownEvent() {}
    override fun onTapEvent() {}
    override fun onLongPressEvent() {}
    override fun onRotateUp() {}
    override fun onRotateDown() {}
    override fun onRotateLeft() {}
    override fun onRotateRight() {}

    /**
     * Calls for new content from the server if allowed and starts displaying content in the screen.
     */
    private fun awakeUpProcedures() {

        if(pullContentOnWake && !gotData && hasConnection(context) && isLogging(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                updateKillThread(System.currentTimeMillis())
                gotData = it
                getContentCollection().setup()
                val content = getContentCollection().getCurrentContent()
                if(content != null) {
                    getContentCollection().startLoggingFields(content)
                    getPresenter().displayContentPiece(content)
                } else
                    getPresenter().displayWarning()
                stopKillThread()
                updateState(ControllerState.ACTIVE)
                Logger.setLogSessionToken()
                Logger.log(LogType.WAKE_UP, listOf(), context)
            }

            getPresenter().displayCover(CoverType.WHITE)
            PullMediaPushLogsAsyncTask(postExecuteCallback = postExecuteCallback).execute(context)
            updateState(ControllerState.PULLING_DATA)

        } else {
            val content = getContentCollection().getCurrentContent()
            if(content != null) {
                getContentCollection().startLoggingFields(content)
                getPresenter().displayContentPiece(content)
            } else {
                getPresenter().displayWarning()
            }
            updateState(ControllerState.ACTIVE)
            Logger.setLogSessionToken()
            Logger.log(LogType.WAKE_UP, listOf(), context)
        }
    }

    /**
     * Displays the next image in the content collection.
     */
    private fun nextImage() {
        updateKillThread(System.currentTimeMillis())
        val content = getContentCollection().goToNextContent()
        if(content != null)
            getPresenter().displayContentPiece(content)
        else
            getPresenter().displayWarning()
    }

    /**
     * Stops the termination thread and terminates the app.
     */
    private fun turnOff() {

        if(getCurrentState() != ControllerState.OFF) {
            updateState(ControllerState.OFF)
            stopKillThread()
            if(getPresenter().view != null)
                getPresenter().view!!.finishActivity()
        }

    }
}