package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import android.os.AsyncTask
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR

class PullMediaPushLogsAsyncTask(private val preExecuteCallback: () -> Unit = {}, private val postExecuteCallback: (Boolean) -> Unit = {}) : AsyncTask<Context, Void, Boolean>() {

    override fun onPreExecute() {
        super.onPreExecute()
        preExecuteCallback()
    }

    override fun doInBackground(vararg contexts: Context?): Boolean {

        if(contexts.isNotEmpty() && contexts[0] != null) {
            var pullMediaSuccess = false

            when (FLAVOR) {
                "locket_touch", "locket_touch_inverted" -> pullMediaSuccess = AsyncHelper.pullMediaLocket(contexts[0]!!)
                "refind" -> pullMediaSuccess = AsyncHelper.pullMediaRefind(contexts[0]!!)
            }

            return pullMediaSuccess && AsyncHelper.pushLogs(contexts[0]!!)
        }
        return false

    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result != null)
            postExecuteCallback(result)
    }

}