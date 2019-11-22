package uk.ac.ncl.openlab.ongoingness.collections

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.getBitmapFromFile
import java.io.File

abstract class AbstractContentCollection(activity: FragmentActivity) {

    private var context = activity.applicationContext
    private var currentIndex = 0
    private var watchMediaViewModel =  ViewModelProviders.of(activity).get(WatchMediaViewModel::class.java)
    private var contentList: List<WatchMedia> = listOf()

    private var newContentTimestamp: Long? = null
    private var indexTimestamp: Int? = null
    private var contentPieceTimestamp: ContentPiece? = null
    private var actionTimestamp: ContentLogAction? = null

    private var stopped = false

    abstract fun setContent(watchMediaViewModel : WatchMediaViewModel) : List<WatchMedia>

    fun setup() {
        stopped = false
        contentList = setContent(watchMediaViewModel)
        restartIndex()
    }

    fun stop() {

        if(!stopped) {
            logTransition(ContentLogAction.STOPPED, null)
            stopped = true
        }

    }

    fun restartIndex() {
        currentIndex = 0
    }

    fun getCurrentContent(): ContentPiece? {
        if(contentList.isNullOrEmpty())
            return null

        return packageContent(contentList[currentIndex])
    }

    fun goToNextContent(): ContentPiece? {
        if(contentList.isNullOrEmpty())
            return null

        var contentPiece: ContentPiece? = null

        //TODO It may loop infinitely if no images were downloaded but are in the database
        while(contentPiece == null) {

            if(currentIndex == contentList.size-1)
                currentIndex = 0
            else
                currentIndex++

            contentPiece = packageContent(contentList[currentIndex])

        }

        logTransition(ContentLogAction.NEXT, contentPiece)

        return contentPiece
    }

    fun getNextContent():ContentPiece? {
        var nextIndex = if (currentIndex == contentList.size - 1) 0 else currentIndex + 1
        return packageContent(contentList[nextIndex])
    }

    fun goToPreviousContent(): ContentPiece? {
        if(contentList.isEmpty())
            return null

        var contentPiece: ContentPiece? = null

        //TODO It may loop infinitely if no images were downloaded but are in the database
        while(contentPiece == null) {

            if (currentIndex == 0)
                currentIndex = contentList.size - 1
            else
                currentIndex--

            contentPiece = packageContent(contentList[currentIndex])
        }

        logTransition(ContentLogAction.PREVIOUS, contentPiece)

        return contentPiece
    }

    fun getPreviousContent(): ContentPiece? {
        var previousIndex = if (currentIndex == 0) contentList.size - 1 else currentIndex - 1
        return packageContent(contentList[previousIndex])
    }

    private fun packageContent(content: WatchMedia): ContentPiece? {
        val file  = File(context!!.filesDir, content.path)
        if(file.exists()) {
            val type = if (content.mimetype.contains("gif")) ContentType.GIF else ContentType.IMAGE
            val bitmap = BitmapDrawable(getBitmapFromFile(context, file.name))
            return ContentPiece(file, type, bitmap)
        }
        return null
    }

    fun startLoggingFields(contentPiece: ContentPiece) {
        newContentTimestamp = System.currentTimeMillis()
        indexTimestamp = currentIndex
        contentPieceTimestamp = contentPiece
        actionTimestamp = ContentLogAction.STARTED
    }

    private fun logTransition(contentLogAction: ContentLogAction, contentPiece: ContentPiece?) {

        if(newContentTimestamp != null) {

            val timePassed = System.currentTimeMillis() - newContentTimestamp!!

            var content = mutableListOf("contentID:${contentList[indexTimestamp!!]._id}",
                    "displayedTime:$timePassed",
                    "action:$actionTimestamp",
                    "type:${contentPieceTimestamp!!.type}")

            when(contentPieceTimestamp!!.type) {

                ContentType.GIF -> {

                    content.add("duration: ${contentList[indexTimestamp!!].duration}")
                    //content.add("type:GIF")
                }

                ContentType.IMAGE -> {
                    //content.add("type:IMAGE")
                }

            }

            content.add( if(contentLogAction == ContentLogAction.STOPPED)
                "lastContentSeen:true" else "lastContentSeen:false" )

            Logger.log( LogType.CONTENT_DISPLAYED, content, context!! )

            Log.d("content", "$content")

            if(contentPiece != null) {
                newContentTimestamp = System.currentTimeMillis()
                indexTimestamp = currentIndex
                contentPieceTimestamp = contentPiece
                actionTimestamp = contentLogAction
            }
        }

    }

    private enum class ContentLogAction {
        STARTED, NEXT, PREVIOUS, STOPPED
    }

}