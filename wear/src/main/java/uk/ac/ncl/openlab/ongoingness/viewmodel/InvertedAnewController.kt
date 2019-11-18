package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.utilities.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaAsyncTask

const val INVERTED_PULL_CONTENT_ON_WAKE = true

class InvertedAnewController(context: Context,
                     recogniser: AbstractRecogniser,
                     presenter: Presenter,
                     contentCollection: AbstractContentCollection,
                     val startedWitTap: Boolean,
                     val faceState: String,
                     val battery: Float)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    override fun onRotateUp() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRotateDown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPickUp() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    var gotData = false
    var start = true

    override fun setStatingState() {

    }

    override fun onStartedEvent() {

        startKillThread(30 * 1000L, 5 * 60 * 1000L)

        if(faceState == ControllerState.CHARGING.toString()) {
            getPresenter().displayChargingCover(battery)
            updateState(ControllerState.CHARGING)
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

            ControllerState.STANDBY -> {

                if(start) {
                    awakeUpProcedures()
                    start = false
                } else {
                    updateState(ControllerState.READY)
                }
            }
            else -> {}
        }


    }

    override fun onAwayEvent() {
        Logger.log(LogType.ACTIVITY_TERMINATED, listOf(), context)
        stopKillThread()
        getPresenter().view!!.finishActivity()
    }

    override fun onUnknownEvent() {}

    override fun onTapEvent() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {

                val content = getContentCollection().goToNextContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)

            }

            else ->  { }
        }
    }

    override fun onLongPressEvent() {

        when(getCurrentState()) {

            ControllerState.READY -> awakeUpProcedures()

            ControllerState.ACTIVE -> {
                getPresenter().displayCover(CoverType.BLACK)
                Logger.log(LogType.SLEEP, listOf(), context)
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
                updateState(ControllerState.STANDBY)
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

    private fun awakeUpProcedures() {
        if(PULL_CONTENT_ON_WAKE && !gotData && hasConnection(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().restartIndex()
                val content = getContentCollection().getCurrentContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
                stopKillThread()
                updateState(ControllerState.ACTIVE)
            }

            getPresenter().displayCover(CoverType.WHITE)
            PullMediaAsyncTask(postExecuteCallback = postExecuteCallback).execute(context)
            updateState(ControllerState.PULLING_DATA)

        } else {

            getContentCollection().restartIndex()
            val content = getContentCollection().getCurrentContent()
            if(content != null)
                getPresenter().displayContentPiece(content)
            stopKillThread()
            updateState(ControllerState.ACTIVE)

        }
        Logger.log(LogType.WAKE_UP, listOf(), context)
    }

}


