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
 * Controller used by the Inverted Anew flavour.
 *
 * @param startedWithTap true if the app was started by tapping the screen.
 * @param faceState the state of the watch face at the start of the app.
 * @param battery the battery level at the start of the app.
 * @param pullContentOnWake flag to decide if the app pulls data from the server on start.
 * @author Luis Carvalho
 */
class InvertedAnewController(context: Context,
                             recogniser: AbstractRecogniser,
                             presenter: Presenter,
                             contentCollection: AbstractContentCollection,
                             val startedWitTap: Boolean,
                             val faceState: String,
                             val battery: Float,
                             private val pullContentOnWake : Boolean)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    /**
     * Registers if this controller has got data from the server already.
     */
    var gotData = false

    override fun setStartingState() {
        startKillThread(30 * 1000L, 5 * 60 * 1000L)

        if(faceState == ControllerState.CHARGING.toString()) {
            getPresenter().displayChargingCover(battery)
            updateState(ControllerState.CHARGING)
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
        if(getCurrentState() != ControllerState.OFF) {
            updateState(ControllerState.OFF)
            stopKillThread()
            if(getPresenter().view != null)
                getPresenter().view!!.finishActivity()
        }
    }

    override fun onTapEvent() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {

                val content = getContentCollection().goToNextContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
                else
                    getPresenter().displayWarning()

            }

            else ->  {}
        }
    }

    override fun onLongPressEvent() {

        when(getCurrentState()) {

            ControllerState.READY -> awakeUpProcedures()

            ControllerState.ACTIVE -> {
                getPresenter().displayCover(CoverType.BLACK)
                getContentCollection().stop()
                Logger.log(LogType.SLEEP, listOf(), context)
                Logger.deleteLogSessionToken()
                updateState(ControllerState.READY)
            }

            else ->{}

        }

    }

    override fun onChargerConnectedEvent(battery: Float) {
        getPresenter().displayChargingCover(battery)
        updateState(ControllerState.CHARGING)
    }

    override fun onChargerDisconnectedEvent() {
        when(getCurrentState()) {
            ControllerState.CHARGING -> {
                getPresenter().displayCover(CoverType.BLACK)
                updateState(ControllerState.READY)
            }
            else -> {}
        }
    }

    override fun onBatteryChangedEvent(battery: Float) {
        when(getCurrentState()) {
           ControllerState.CHARGING -> getPresenter().displayChargingCover(battery)
            else -> {}
        }
    }

    override fun onStartedEvent() {}
    override fun onStoppedEvent() {}
    override fun onUpEvent() {}
    override fun onDownEvent() {}
    override fun onRotateUp() {}
    override fun onRotateDown() {}
    override fun onRotateLeft() {}
    override fun onRotateRight() {}
    override fun onAwayLeft() {}
    override fun onAwayRight() {}
    override fun onAwayTowards() {}
    override fun onUnknownEvent() {}

    /**
     * Calls for new content from the server if allowed and starts displaying content in the screen.
     */
    private fun awakeUpProcedures() {

        if(pullContentOnWake && !gotData && hasConnection(context) && isLogging(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
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
            getContentCollection().setup()
            val content = getContentCollection().getCurrentContent()
            if(content != null) {
                getContentCollection().startLoggingFields(content)
                getPresenter().displayContentPiece(content)
            } else {
                getPresenter().displayWarning()
            }
            stopKillThread()
            updateState(ControllerState.ACTIVE)
            Logger.setLogSessionToken()
            Logger.log(LogType.WAKE_UP, listOf(), context)
        }

    }

}


