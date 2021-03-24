package uk.ac.ncl.openlab.ongoingness.collections

import androidx.fragment.app.FragmentActivity
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaViewModel
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia

/**
 *  Sets which content is to be presented and how in the Anew flavour.
 *  The content is set by reverse order of creation, presenting first the set of content belonging to the permanent collection and then the one belonging to the temporary collection.
 *
 * @author Luis Carvalho
 */
class AnewContentCollection(activity: FragmentActivity) : AbstractContentCollection(activity) {

    override fun setContent(watchMediaViewModel: WatchMediaViewModel): List<WatchMedia> {
        return watchMediaViewModel.getCollection("permanent").sortedBy { it.createdAt }.reversed() +
                watchMediaViewModel.getCollection("temporary").sortedBy { it.createdAt }.reversed()
    }

}