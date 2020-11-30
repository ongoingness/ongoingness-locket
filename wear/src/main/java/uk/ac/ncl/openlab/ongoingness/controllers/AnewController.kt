package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaAsyncTask
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsAsyncTask

class AnewController(context: Context,
                     recogniser: AbstractRecogniser,
                     presenter: Presenter,
                     contentCollection: AbstractContentCollection,
                     val startedWitTap: Boolean,
                     val faceState: String,
                     val battery: Float,
                    private val pullContentOnWake: Boolean)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    private var gotData = false

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


    override fun onTowardsEvent() {

        when(getCurrentState()) {

            ControllerState.STANDBY -> updateState(ControllerState.READY)
            else -> {}
        }

    }

    override fun onAwayEvent() {
        //stopKillThread()
        //getPresenter().view!!.finishActivity()

        if(getCurrentState() != ControllerState.OFF) {
            updateState(ControllerState.OFF)
            stopKillThread()
            if(getPresenter().view != null)
                getPresenter().view!!.finishActivity()
        }
    }

    override fun onUnknownEvent() {}

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
                //getPresenter().displayCover(CoverType.BLACK)
                //Logger.log(LogType.SLEEP, listOf(), context)
                //updateState(ControllerState.READY)

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

        //getPresenter().displayChargingCover(battery)
        //updateState(ControllerState.CHARGING)

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
        /*
        when(getCurrentState()) {
           ControllerState.CHARGING -> getPresenter().displayChargingCover(battery)
            else -> {}
        }*/
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
    override fun onAwayTowards() {
        TODO("Not yet implemented")
    }

    private fun awakeUpProcedures() {

        /*
        if(pullContentOnWake && !gotData && hasConnection(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().setup()
                val content = getContentCollection().getCurrentContent()
                if(content != null) {
                    getContentCollection().startLoggingFields(content)
                    getPresenter().displayContentPiece(content)
                }
                stopKillThread()
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
            stopKillThread()
            updateState(ControllerState.ACTIVE)

        }
        Logger.log(LogType.WAKE_UP, listOf(), context)
        */
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


