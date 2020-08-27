package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import android.util.Log.d
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log
import uk.ac.ncl.openlab.ongoingness.database.repositories.LogRepository
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaRoomDatabase

object Logger {

    private lateinit var repository: LogRepository

    private var sessionToken: Long? = null

    fun start(context: Context) {
        if (!(Logger::repository.isInitialized))
            repository = LogRepository(WatchMediaRoomDatabase.getDatabase(context).logDao())
    }

    fun setLogSessionToken() {
        sessionToken = System.currentTimeMillis()
    }

    fun deleteLogSessionToken() {
        sessionToken = null
    }

    fun log(type: LogType, content: List<String>, context: Context)  {

        val mutableContentList = content.toMutableList()
        mutableContentList.add("battery:${getBatteryLevel(context)}")

        val currentTime = System.currentTimeMillis().toString()
        var message = ""

        when(type) {

            LogType.WAKE_UP -> {
                message = "Device was awaken."
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

            LogType.CONTENT_DISPLAYED -> {
                message = "Device displayed content."
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

            LogType.SLEEP -> {
                message = "Device went to sleep."
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

            LogType.ACTIVITY_STARTED -> message = "Activity Started."

            LogType.ACTIVITY_TERMINATED -> {
                message = "Activity Terminated."
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

            LogType.CHARGER_CONNECTED -> message = "Charger Connected."

            LogType.CHARGER_DISCONNECTED -> message = "Charger Disconnected."

            LogType.PULLED_CONTENT -> message = "Pulled Content From Server."

            LogType.PUSHED_LOGS -> message = "Pushed Logs To Server."

            LogType.NEW_CONTENT_ADDED -> message = "New Content Added."

            LogType.CONTENT_REMOVED -> message = "Content Removed."

            LogType.STARTED_WATCHFACE -> message = "WatchFace Started."

            LogType.STOPPED_WATCHFACE -> message = "WatchFace Stopped."

        }

        GlobalScope.launch {
            repository.insert(Log("info", type.toString(), stringListToJson(mutableContentList), message, currentTime))
        }

    }

    fun getAll(): List<Log> {
        return repository.getAll()
    }

    private fun stringListToJson(list: List<String>): String {

        val sb = StringBuilder()
        sb.append("{")

        for(elem in list) {
            val splits = elem.split(":")
            if(splits.size == 3)
                sb.append("\"${splits[0]}\": \"${splits[1]}:${splits[2]}\"")
            else
                sb.append("\"${splits[0]}\": \"${splits[1]}\"")
            if(list.last() != elem)
                sb.append(", ")
        }

        sb.append("}")

       return sb.toString()
    }

    fun formatLogs(): String {
        return Gson().toJson(repository.getAll())
    }

    fun clearLogs() {
        repository.deleteAll()
    }

}

enum class LogType {
    ACTIVITY_STARTED,
    WAKE_UP,
    CONTENT_DISPLAYED,
    SLEEP,
    ACTIVITY_TERMINATED,
    CHARGER_CONNECTED,
    CHARGER_DISCONNECTED,
    PULLED_CONTENT,
    PUSHED_LOGS,
    NEW_CONTENT_ADDED,
    CONTENT_REMOVED,
    STARTED_WATCHFACE,
    STOPPED_WATCHFACE,
    ERROR,
}

