package uk.ac.ncl.openlab.ongoingness.viewmodel

import androidx.fragment.app.FragmentActivity
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia

class RefindContentCollection(activity: FragmentActivity) : AbstractContentCollection(activity) {

    override fun setContent(watchMediaViewModel: WatchMediaViewModel): List<WatchMedia> {
        return  watchMediaViewModel.allWatchMedia().sortedWith(compareBy({it.collection}, {it.order})).reversed()
    }

}