package uk.ac.ncl.openlab.ongoingness.utilities

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MediaSyncService : Service() {

    override fun onCreate() {
        synchronized(sSyncAdapterLock) {
            sSyncAdapter = sSyncAdapter ?: MediaSyncAdapter(applicationContext, true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return sSyncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var sSyncAdapter : MediaSyncAdapter? = null
        private var sSyncAdapterLock = Any()
    }

}