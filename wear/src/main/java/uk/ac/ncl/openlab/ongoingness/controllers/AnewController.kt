package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import android.util.Log
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
 * Controller used by the Anew flavour.
 *
 * @param startedWithTap true if the app was started by tapping the screen.
 * @param faceState the state of the watch face at the start of the app.
 * @param battery the battery level at the start of the app.
 * @param pullContentOnWake flag to decide if the app pulls data from the server on start.
 * @author Luis Carvalho
 */
class AnewController(context: Context,
                     recogniser: AbstractRecogniser,
                     presenter: Presenter,
                     contentCollection: AbstractContentCollection,
                     val startedWithTap: Boolean,
                     val faceState: String,
                     val battery: Float,
                    private val pullContentOnWake: Boolean)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    /**
     * Registers if this controller has got data from the server already.
     */
    private var gotData = false

    override fun onStartedEvent() {

        startKillThread(30 * 1000L, 5 * 60 * 1000L)

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

            ControllerState.STANDBY -> updateState(ControllerState.READY)
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

            ControllerState.READY -> awakeUpProcedures()

            ControllerState.ACTIVE -> {
                val content = getContentCollection().goToNextContent()
                Log.d("whar", "$content")
                if(content != null)
                    getPresenter().displayContentPiece(content)
                else
                    getPresenter().displayWarning()

            }

            else ->  { }
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
        stopKillThread()
        getPresenter().view!!.finishActivity()
    }

    override fun onChargerDisconnectedEvent() {
        when(getCurrentState()) {
            ControllerState.CHARGING -> {
                getPresenter().displayCover(CoverType.BLACK)
                updateState(ControllerState.STANDBY)
            }
            else -> {}
        }
    }

    override fun onBatteryChangedEvent(battery: Float) {
        stopKillThread()
        getPresenter().view!!.finishActivity()

    }

    override fun setStartingState() {}
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


