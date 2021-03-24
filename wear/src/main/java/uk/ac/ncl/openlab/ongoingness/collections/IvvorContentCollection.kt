package uk.ac.ncl.openlab.ongoingness.collections

import androidx.fragment.app.FragmentActivity
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import java.util.*

/**
 *  Sets which content is to be presented and how in the Ivvor flavour.
 *  The content to be set depends on the current date.
 *  If there is content allocated to the date, that content is set else all content belonging to the permanent collection is set.
 *
 * @author Luis Carvalho
 */
class IvvorContentCollection(activity: FragmentActivity) : AbstractContentCollection(activity) {

    override fun setContent(watchMediaViewModel: WatchMediaViewModel): List<WatchMedia> {

        var c = Calendar.getInstance()
        c.timeInMillis = System.currentTimeMillis()

        var temporaryList = watchMediaViewModel.getWatchMediaForDate(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH))

        if(temporaryList.isEmpty())
            return watchMediaViewModel.getCollection("permanent").sortedBy { it.createdAt }

        return temporaryList.sortedBy { it.createdAt }

        return mutableListOf()

    }

}