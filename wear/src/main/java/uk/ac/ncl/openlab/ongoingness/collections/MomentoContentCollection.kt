package uk.ac.ncl.openlab.ongoingness.collections

import android.util.Log
import androidx.fragment.app.FragmentActivity
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import java.sql.Date
import java.util.*

class MomentoContentCollection(activity: FragmentActivity) : AbstractContentCollection(activity) {

    override fun setContent(watchMediaViewModel: WatchMediaViewModel): List<WatchMedia> {

        var c = Calendar.getInstance()
        c.timeInMillis = System.currentTimeMillis()

        var temporaryList = watchMediaViewModel.getWatchMediaForDate(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH))

        if(temporaryList.isEmpty())
            return watchMediaViewModel.getCollection("permanent").sortedBy { it.createdAt }

        return temporaryList.sortedBy { it.createdAt }

    }

}