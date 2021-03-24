package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log
import uk.ac.ncl.openlab.ongoingness.database.repositories.LogRepository
import uk.ac.ncl.openlab.ongoingness.database.WatchMediaRoomDatabase

/**
 * In charge of all logging related tasks: recording, parsing, erasing.
 *
 * @author Luis Carvalho
 */
object Logger {

    /**
     * Local database repository of logs.
     */
    private lateinit var repository: LogRepository

    /**
     * Session token of the interaction current session.
     * Serves to denote if a set of logs happen during the same interaction session with the piece.
     */
    private var sessionToken: Long? = null

    /**
     * Wifi mac address of the device.
     */
    private var macAddress: String? = null

    /**
     * Flag declining if logs should be recorded.
     */
    private var logging: Boolean = true

    /**
     * Start the logger by checking the prefs for settings, setting the mac address and starting the log repository.
     *
     * @param context context of the application.
     */
    fun start(context: Context) {

        val pref: SharedPreferences = context.getSharedPreferences("OngoingnessPrefs", 0)
        logging = pref.getBoolean("logging", true)

        android.util.Log.d("logging start", logging.toString())

        if(!logging)
            return

        macAddress = getMacAddress()

        if (!(Logger::repository.isInitialized))
            repository = LogRepository(WatchMediaRoomDatabase.getDatabase(context).logDao())
    }

    /**
     *  Set the session token.
     */
    fun setLogSessionToken() {
        sessionToken = System.currentTimeMillis()
    }

    /**
     * Deletes the current session token.
     */
    fun deleteLogSessionToken() {
        sessionToken = null
    }

    /**
     * Creates a log given a type a set of data to be stored with.
     *
     * @param type the type of the log.
     * @param content custom data to be attached to this log.
     * @param context context of the application.
     */
    fun log(type: LogType, content: List<String>, context: Context)  {

        if(!logging)
            return

        if(macAddress == null)
            macAddress = getMacAddress()

        val mutableContentList = content.toMutableList()
        mutableContentList.add("battery:${getBatteryLevel(context)}")

        if(macAddress != null)
            mutableContentList.add("device:${macAddress}")

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

            LogType.AWAY_LEFT_DURATION -> {
                message = "Device was away on the left."
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

            LogType.AWAY_RIGHT_DURATION -> {
                message = "Device was away on the right."
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

            LogType.TRIED_CONNECTION -> {
                message ="Tried to connect to the server"
            }

            else -> {
                message = "New log"
                if(sessionToken != null) mutableContentList.add("session:$sessionToken")
            }

        }

        GlobalScope.launch {
            repository.insert(Log("info", type.toString(), stringListToJson(mutableContentList), message, currentTime))
        }

    }

    /**
     * Gets all logs from the local database.
     *
     * @return list of all existing logs.
     */
    fun getAll(): List<Log> {
        return repository.getAll()
    }

    /**
     * Converts a list of string logs into one JSON formatted string.
     *
     * @param list list of logs as strings.
     * @return one string with all logs formatted in JSON.
     */
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

    /**
     * Converts all logs in the local database into a string in JSON format.
     *
     * @return JSON formatted string with all logs.
     */
    fun formatLogs(): String {
        return Gson().toJson(repository.getAll())
    }

    /**
     * Deletes all logs in the local database.
     */
    fun clearLogs() {
        repository.deleteAll()
    }

    /**
     * Disables logging by setting a flag in the app preferences file.
     *
     * @param context context of the application.
     */
    fun disableLogging(context: Context) {
        android.util.Log.d("disableLogging", "true")


        val pref: SharedPreferences = context.getSharedPreferences(BuildConfig.ONGOINGNESS_PREFS, 0)
        with (pref.edit()) {
            putBoolean(BuildConfig.LOGGING_KEY, false)
            apply()
        }

        val failures2 = pref.getBoolean(BuildConfig.LOGGING_KEY, true)
        android.util.Log.d("disableLogging", "$failures2")

        if (!(Logger::repository.isInitialized))
            repository = LogRepository(WatchMediaRoomDatabase.getDatabase(context).logDao())

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
    AWAY_LEFT_DURATION,
    AWAY_RIGHT_DURATION,
    AWAY_TOWARDS_DURATION,
    TRIED_CONNECTION,
}

