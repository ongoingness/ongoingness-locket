package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR
import uk.ac.ncl.openlab.ongoingness.utilities.addPullMediaWorkRequest

/**
 * Worker the pulls media from the server.
 *
 * @author Luis Carvalho
 */
class PullMediaWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {

        var result = Result.failure()

        when (FLAVOR) {
            "locket_touch", "locket_touch_inverted" -> {
                if(AsyncHelper.pullMediaLocket(ctx))
                    result = Result.success()
            }

            "refind" -> {
                if(AsyncHelper.pullMediaRefind(ctx))
                    result = Result.success()
            }
        }

        addPullMediaWorkRequest(ctx)

        return result
    }
}