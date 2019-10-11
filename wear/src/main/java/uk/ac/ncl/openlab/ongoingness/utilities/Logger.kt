package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object Logger {

    private lateinit var repository: LogRepository

    fun start(context: Context) {
        if (!(::repository.isInitialized))
            repository = LogRepository( WatchMediaRoomDatabase.getDatabase(context).logDao())
    }

    fun log(type: LogType, content: List<String>, context: Context)  {

        var mutableContentList = content.toMutableList()
        mutableContentList.add("battery:${getBatteryLevel(context)}")

        var currentTime = System.currentTimeMillis().toString()
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
            var splitted = elem.split(":")
            if(splitted.size == 3)
                sb.append("\"${splitted[0]}\": \"${splitted[1]}:${splitted[2]}\"")
            else
                sb.append("\"${splitted[0]}\": \"${splitted[1]}\"")
            if(list.last() != elem)
                sb.append(", ")
        }

        sb.append("}")

       return sb.toString()
    }

    fun sendLogs(): String {

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


        var api = API()

        GlobalScope.launch {
            api.sendLogs(sb.toString(), callback = { response -> repository.deleteAll()})
        }

        return sb.toString()
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

