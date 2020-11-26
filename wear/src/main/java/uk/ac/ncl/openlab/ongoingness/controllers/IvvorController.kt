package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsAsyncTask

class IvvorController(context: Context,
                        recogniser: AbstractRecogniser,
                        presenter: Presenter,
                        contentCollection: AbstractContentCollection,
                        val faceState: String,
                        val battery: Float,
                      private val pullContentOnWake: Boolean) : AbstractController(context, recogniser, presenter, contentCollection) {

    private var gotData = false

    override fun setStartingState() {}

    override fun onStartedEvent() {

        startKillThread(30 * 1000L, 5 * 60 * 1000L)

        if(faceState == ControllerState.CHARGING.toString()) {
            stopKillThread()
            getPresenter().view!!.finishActivity()
            //getPresenter().displayChargingCover(battery)
            //updateState(ControllerState.CHARGING)
        } else {
            getPresenter().displayCover(CoverType.BLACK)
            updateState(ControllerState.STANDBY)
        }

    }

    override fun onStoppedEvent() {}

    override fun onUpEvent() {}

    override fun onDownEvent() {}

    override fun onTowardsEvent() {

        when(getCurrentState()) {

            ControllerState.STANDBY -> awakeUpProcedures()
            else -> {}
        }

    }

    override fun onAwayEvent() {
        turnOff()
    }

    override fun onUnknownEvent() {}

    override fun onTapEvent() {}

    override fun onLongPressEvent() {}

    override fun onChargerConnectedEvent(battery: Float) {
        turnOff()
    }

    override fun onChargerDisconnectedEvent() {
        turnOff()
    }

    override fun onBatteryChangedEvent(battery: Float) {
        turnOff()
    }

    override fun onRotateUp() {}

    override fun onRotateDown() {}

    override fun onRotateLeft() {}

    override fun onRotateRight() {}

    override fun onAwayLeft() {
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

    override fun onAwayRight() {
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

    private fun awakeUpProcedures() {

        if(pullContentOnWake && !gotData && hasConnection(context)) {

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
            Log.d("s","$content ${getPresenter()}")
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

    private fun turnOff() {

        if(getCurrentState() != ControllerState.OFF) {
            updateState(ControllerState.OFF)
            stopKillThread()
            if(getPresenter().view != null)
                getPresenter().view!!.finishActivity()
        }

    }
}