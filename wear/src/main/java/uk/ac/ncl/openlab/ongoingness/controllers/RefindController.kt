package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
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

const val REFIND_PULL_CONTENT_ON_WAKE = true

class RefindController(context: Context,
                       recogniser: AbstractRecogniser,
                       presenter: Presenter,
                       contentCollection: AbstractContentCollection)
    : AbstractController(context, recogniser, presenter, contentCollection) {

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

        if(REFIND_PULL_CONTENT_ON_WAKE && !gotData && hasConnection(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().setup()
                val content = getContentCollection().getCurrentContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
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
            if(content != null)
                getPresenter().displayContentPiece(content)
            startKillThread(1000L * 30 * 1  , 5000L)
            updateState(ControllerState.ACTIVE)
        }

    }

    override fun setStatingState() {}

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

}


