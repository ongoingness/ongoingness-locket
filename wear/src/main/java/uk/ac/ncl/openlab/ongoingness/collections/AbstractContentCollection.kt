package uk.ac.ncl.openlab.ongoingness.collections

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import java.io.File

abstract class AbstractContentCollection(activity: FragmentActivity) {

    private var context = activity.applicationContext
    private var currentIndex = 0
    private var watchMediaViewModel =  ViewModelProviders.of(activity).get(WatchMediaViewModel::class.java)
    private var contentList: List<WatchMedia> = listOf()

    private  var newContentTimestamp: Long? = null
    private  var indexTimestamp: Int? = null

    abstract fun setContent(watchMediaViewModel : WatchMediaViewModel) : List<WatchMedia>

    fun setup() {
        contentList = setContent(watchMediaViewModel)
        restartIndex()
    }

    fun stop() {
        logTransition(LogType.NEXT_IMAGE)
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

        logTransition(LogType.NEXT_IMAGE)

        return contentPiece
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

        logTransition(LogType.PREV_IMAGE)

        return contentPiece
    }

    private fun packageContent(content: WatchMedia): ContentPiece? {
        val file  = File(context!!.filesDir, content.path)
        if(file.exists()) {
            val type = if (content.mimetype.contains("gif")) ContentType.GIF else ContentType.IMAGE
            return ContentPiece(file, type)
        }
        return null
    }

    private fun logTransition(type: LogType) {
        if(newContentTimestamp == null) {
            newContentTimestamp = System.currentTimeMillis()
            indexTimestamp = currentIndex
        } else {
            val timePassed = System.currentTimeMillis() - newContentTimestamp!!

            val content = listOf("imageID:${contentList[currentIndex]._id}", "displayedTime:$timePassed")

            Logger.log( type, content, context!! )

            newContentTimestamp = System.currentTimeMillis()
            indexTimestamp = currentIndex

        }
    }

}