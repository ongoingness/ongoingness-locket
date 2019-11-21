package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log
import uk.ac.ncl.openlab.ongoingness.database.repositories.LogRepository
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaRoomDatabase

object Logger {

    private lateinit var repository: LogRepository

    fun start(context: Context) {
        if (!(Logger::repository.isInitialized))
            repository = LogRepository(WatchMediaRoomDatabase.getDatabase(context).logDao())
    }

    fun log(type: LogType, content: List<String>, context: Context)  {

        val mutableContentList = content.toMutableList()
        mutableContentList.add("battery:${getBatteryLevel(context)}")

        val currentTime = System.currentTimeMillis().toString()
        var message = ""

        when(type) {

            LogType.WAKE_UP -> {
                message = "Device was awaken."
            }

            LogType.NEXT_IMAGE -> {
                message = "User went to the next image."

            }

            LogType.PREV_IMAGE -> {
                message = "User went to the next image."
            }

            LogType.SLEEP -> {
                message = "Device went to sleep."
            }

            LogType.ACTIVITY_STARTED -> {
                message = "Activity Started."
            }

            LogType.ACTIVITY_TERMINATED -> {
                message = "Activity Terminated."
            }

            LogType.CHARGER_CONNECTED -> {
                message = "Charger Connected.."
            }

            LogType.CHARGER_DISCONNECTED -> {
                message = "Charger Disconnected."
            }


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
        val sb = StringBuilder()
        sb.append("[")

        for(log in repository.getAll()) {
            sb.append("{")
            sb.append("\"level\": \"${log.level}\",")
            sb.append("\"code\": \"${log.code}\",")
            sb.append("\"content\": ${log.content},")
            sb.append("\"message\": \"${log.message}\",")
            sb.append("\"timestamp\": \"${log.timestamp}\"")
            sb.append("}")

            if(repository.getAll().last() != log)
                sb.append(",")
        }
        sb.append("]")

        return sb.toString()
    }

    fun clearLogs() {
        repository.deleteAll()
    }

    fun sendLogs(): String {
        val logs = formatLogs()
        val api = API()

        GlobalScope.launch {
            api.sendLogs(logs, callback = { repository.deleteAll()}, failure = {})
        }

        return logs
    }
}

enum class LogType {
    ACTIVITY_STARTED,
    WAKE_UP,
    NEXT_IMAGE,
    PREV_IMAGE,
    SLEEP,
    ACTIVITY_TERMINATED,
    CHARGER_CONNECTED,
    CHARGER_DISCONNECTED
}

