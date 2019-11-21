package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import android.os.AsyncTask
import uk.ac.ncl.openlab.ongoingness.BuildConfig.FLAVOR

class PullMediaAsyncTask(private val preExecuteCallback: () -> Unit = {}, private val postExecuteCallback: (Boolean) -> Unit = {}) : AsyncTask<Context, Void, Boolean>() {

    override fun onPreExecute() {
        super.onPreExecute()
        preExecuteCallback()
    }

    override fun doInBackground(vararg contexts: Context?): Boolean {
        if(contexts.isNotEmpty() && contexts[0] != null) {

            when(FLAVOR) {

                "locket_touch", "locket_touch_inverted" -> {
                    return AsyncHelper.pullMediaLocket(contexts[0]!!)
                }

                "refind" -> {
                   return AsyncHelper.pullMediaRefind(contexts[0]!!)
                }

            }
        }
        return  false
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result != null)
            postExecuteCallback(result)
    }

}