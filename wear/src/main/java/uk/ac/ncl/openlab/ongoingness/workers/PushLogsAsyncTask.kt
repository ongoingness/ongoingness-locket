package uk.ac.ncl.openlab.ongoingness.workers

import android.content.Context
import android.os.AsyncTask

/**
 * Async Task that pushes logs into the server.
 *
 * @author Luis Carvalho
 */
class PushLogsAsyncTask(private val preExecuteCallback: () -> Unit = {}, private val postExecuteCallback: (Boolean) -> Unit = {}) : AsyncTask<Context, Void, Boolean>() {

    override fun onPreExecute() {
        super.onPreExecute()
        preExecuteCallback()
    }

    override fun doInBackground(vararg contexts: Context?): Boolean {
        if(contexts.isNotEmpty() && contexts[0] != null)
            return AsyncHelper.pushLogs(contexts[0]!!)
        return  false
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (result != null)
            postExecuteCallback(result)
    }

}