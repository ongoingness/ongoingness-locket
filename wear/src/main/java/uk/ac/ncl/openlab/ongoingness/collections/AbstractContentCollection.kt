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

/**
 * Manages a list of content given by a database. Works as a circular list where only one element is accessible at a time.
 *
 * @author Luis Carvalho
 */
abstract class AbstractContentCollection(activity: FragmentActivity) {

    /**
     * Context of the application.
     */
    private var context = activity.applicationContext

    /**
     * Index of the currently displayed content.
     */
    private var currentIndex = 0

    /**
     * ViewModel connected to the database.
     */
    private var watchMediaViewModel =  ViewModelProviders.of(activity).get(WatchMediaViewModel::class.java)

    /**
     * List of content to be displayed.
     */
    private var contentList: List<WatchMedia> = listOf()

    /**
     * Timestamp of when a new current content is set.
     */
    private var newContentTimestamp: Long? = null

    /**
     * Index of the content attributed to a timestamp.
     */
    private var indexTimestamp: Int? = null

    /**
     * Content attributed to a timestamp.
     */
    private var contentPieceTimestamp: ContentPiece? = null

    /**
     * ContentLogAction attributed to a timestamp.
     */
    private var actionTimestamp: ContentLogAction? = null

    /**
     * Sets the content to be managed from a database.
     *
     *  @param watchMediaViewModel ViewModel of the database to be used by this ContentCollection.
     *  @return List containing WatchMedia for this class to manage.
     */
    abstract fun setContent(watchMediaViewModel : WatchMediaViewModel) : List<WatchMedia>

    /**
     * Sets the content to be managed and validates content.
     */
    fun setup() {

        //Check if there are watchMedia without the file
        val tempContentList = setContent(watchMediaViewModel)

        for(wm in tempContentList) {
            if(!contentFileExists(wm)) {
                watchMediaViewModel.delete(wm, context)
            }
        }
        contentList = tempContentList
        restartIndex()

    }

    /**
     * Logs the termination of this class usage.
     */
    fun stop() {
        logTransition(ContentLogAction.STOPPED, null)
    }

    /**
     * Sets the current index to zero.
     */
    fun restartIndex() {
        currentIndex = 0
    }

    /**
     * Packages the current element to be displayed in a ContentPiece object.
     *
     * @return A ContentPiece object with the current element data.
     */
    fun getCurrentContent(): ContentPiece? {
        if(contentList.isNullOrEmpty())
            return null

        return packageContent(contentList[currentIndex])
    }

    /**
     * Changes the current content element to its next.
     *
     * @return the new current element.
     */
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

    /**
     * Gets the next content element.
     *
     * @return A ContentPiece object with the next element data.
     */
    fun getNextContent():ContentPiece? {
        var nextIndex = if (currentIndex == contentList.size - 1) 0 else currentIndex + 1
        return packageContent(contentList[nextIndex])
    }

    /**
     * Changes the current content element to its previous.
     *
     * @return the new current element.
     */
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

    /**
     * Gets the previous content element.
     *
     * @return A ContentPiece object with the previous element data.
     */
    fun getPreviousContent(): ContentPiece? {
        var previousIndex = if (currentIndex == 0) contentList.size - 1 else currentIndex - 1
        return packageContent(contentList[previousIndex])
    }

    /**
     * Packages an WatchMedia object into an ContentPiece object.
     *
     * @param content object to be packaged into an ContentPiece object
     * @return A ContentPiece object with the data present in the given WatchMedia.
     */
    private fun packageContent(content: WatchMedia): ContentPiece? {
        val file  = File(context!!.filesDir, content.path)
        if(file.exists() && file.length() > 0) {
            try {
                val type = if (content.mimetype.contains("gif")) ContentType.GIF else ContentType.IMAGE
                val bitmap = BitmapDrawable(getBitmapFromFile(context, file.name))
                return ContentPiece(file, type, bitmap)
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    /**
     * Checks if a file referenced by a given WatchMedia object exists.
     *
     * @return true if the file exists, false otherwise
     */
    private fun contentFileExists(content: WatchMedia): Boolean {
        val file  = File(context!!.filesDir, content.path)
        return file.exists() && file.length() > 0
    }

    /**
     * Records timestamps and objects needed to start logging interaction around a ContentPiece
     *
     * @param contentPiece object to be logged
     */
    fun startLoggingFields(contentPiece: ContentPiece) {
        newContentTimestamp = System.currentTimeMillis()
        indexTimestamp = currentIndex
        contentPieceTimestamp = contentPiece
        actionTimestamp = ContentLogAction.STARTED
    }

    /**
     * Logs the changes of when the current element changes.
     *
     * @param contentLogAction type of action the triggered the logging.
     * @param contentPiece new ContentPiece to start logging.
     */
    private fun logTransition(contentLogAction: ContentLogAction, contentPiece: ContentPiece?) {

        if(newContentTimestamp != null) {

            val timePassed = System.currentTimeMillis() - newContentTimestamp!!

            var content = mutableListOf("contentID:${contentList[indexTimestamp!!]._id}",
                    "displayedTime:$timePassed",
                    "action:$actionTimestamp",
                    "type:${contentPieceTimestamp!!.type}")

            if(contentPieceTimestamp!!.type == ContentType.GIF)
                content.add("duration: ${contentList[indexTimestamp!!].duration}")

            content.add( if(contentLogAction == ContentLogAction.STOPPED)
                "lastContentSeen:true" else "lastContentSeen:false" )

            Runnable { Logger.log( LogType.CONTENT_DISPLAYED, content, context!! ) }.run()

            Log.d("content", "$content")

            if(contentPiece != null) {
                newContentTimestamp = System.currentTimeMillis()
                indexTimestamp = currentIndex
                contentPieceTimestamp = contentPiece
                actionTimestamp = contentLogAction
            }
        }

    }

    /**
     * Performed actions to be logged.
     */
    private enum class ContentLogAction {
        STARTED, NEXT, PREVIOUS, STOPPED
    }

}