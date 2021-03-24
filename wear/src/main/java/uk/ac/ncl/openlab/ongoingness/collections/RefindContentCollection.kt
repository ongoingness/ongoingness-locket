package uk.ac.ncl.openlab.ongoingness.collections

import androidx.fragment.app.FragmentActivity
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia

/**
 *  Sets which content is to be presented and how in the Refind flavour.
 *  The content to be present depends on the collection it belongs to and the preset order.
 *
 * @author Luis Carvalho
 */
class RefindContentCollection(activity: FragmentActivity) : AbstractContentCollection(activity) {

    override fun setContent(watchMediaViewModel: WatchMediaViewModel): List<WatchMedia> {
        return  watchMediaViewModel.allWatchMedia().sortedWith(compareBy({it.collection}, {it.order})).reversed()
    }

}