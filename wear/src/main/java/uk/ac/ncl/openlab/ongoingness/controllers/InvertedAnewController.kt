package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.crashlytics.android.Crashlytics
import uk.ac.ncl.openlab.ongoingness.R
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaAsyncTask
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsAsyncTask

const val INVERTED_PULL_CONTENT_ON_WAKE = false

class InvertedAnewController(context: Context,
                             recogniser: AbstractRecogniser,
                             presenter: Presenter,
                             contentCollection: AbstractContentCollection,
                             val startedWitTap: Boolean,
                             val faceState: String,
                             val battery: Float,
                             private val pullContentOnWake : Boolean)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    var gotData = false
    var start = true

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

    override fun onStartedEvent() {}

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

    override fun onStoppedEvent() {}

    override fun onUpEvent() {}

    override fun onDownEvent() {}

    override fun onRotateUp() {}

    override fun onRotateDown() {}
    override fun onRotateLeft() {
        TODO("Not yet implemented")
    }

    override fun onRotateRight() {
        TODO("Not yet implemented")
    }

    override fun onAwayLeft() {
        TODO("Not yet implemented")
    }

    override fun onAwayRight() {
        TODO("Not yet implemented")
    }

    override fun onAwayTowards() {
        TODO("Not yet implemented")
    }

    override fun onUnknownEvent() {}

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


