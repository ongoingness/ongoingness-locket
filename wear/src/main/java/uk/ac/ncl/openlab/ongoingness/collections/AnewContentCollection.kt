package uk.ac.ncl.openlab.ongoingness.collections

import androidx.fragment.app.FragmentActivity
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia

class AnewContentCollection(activity: FragmentActivity) : AbstractContentCollection(activity) {

    override fun setContent(watchMediaViewModel: WatchMediaViewModel): List<WatchMedia> {
        return watchMediaViewModel.getCollection("permanent").sortedBy { it.createdAt }.reversed() +
                watchMediaViewModel.getCollection("temporary").sortedBy { it.createdAt }.reversed()
    }

}