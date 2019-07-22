package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import java.io.*
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

const val minBandwidthKbps: Int = 320

/**
 * Clear contents of media folder
 *
 * @param context Context Application context
 */
fun clearMediaFolder(context: Context) {
    context.filesDir.listFiles().forEach { file: File? -> file?.delete() }
}

fun deleteFile(context: Context, filename: String) {
    File(context.filesDir, filename)?.delete()
}


/**
 * Store an array to file
 *
 * @param arr Array<String>
 * @param fileName string
 * @param context Context
 */
fun persistArray(arr : Array<String>, fileName : String, context: Context) {
    val file = File(context.filesDir, fileName)
    arr.forEach { id : String -> file.writeText("$id\n") }
    Log.d("Utils","stored file: ${file.absolutePath}")
}




fun persistBitmap(bitmap: Bitmap, filename:String, context: Context){
    val imageFile = File(context.filesDir, "$filename")
    Log.d("Utils","try to store image ${imageFile.absolutePath}")

    lateinit var stream: OutputStream
    try {
        stream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
    } catch (e: IOException){ // Catch the exception
        e.printStackTrace()
    } finally {
        stream.close()
        Log.d("Utils","stored image: ${imageFile.absolutePath}")
    }
}

/**
 * Load all bitmaps from file
 *
 * @param context Context
 */
fun loadBitmaps(context: Context) : ArrayList<Bitmap> {
    val bitmaps : ArrayList<Bitmap> = ArrayList()
    context.filesDir!!.listFiles().forEach { file: File -> run {
        if (file.name.contains(".txt")) return@run
            bitmaps.add(BitmapFactory.decodeFile(file.absolutePath))
    } }

    return bitmaps
}


fun hasLocalCopy(context: Context, fileName: String):Boolean{

    val file =  File(context.filesDir,fileName)
    Log.d("Utils","Checked for: ${file.absolutePath}")
    return file.exists()
}

/**
 * Get the stored bitmap from file.
 *
 * @param path
 * @return Bitmap
 */
fun getBitmapFromFile(context: Context,filename: String): Bitmap? {
    return try {
        val f = File(context.filesDir,filename)
        BitmapFactory.decodeStream(FileInputStream(f))
    } catch (e: FileNotFoundException) {
        Log.d("GETFILE", "$e")
        null
    }

}

/**
 * Check that the activity has an active network connection.
 *
 * @param context context to check connection from.
 * @return boolean if device has connection
 */
fun hasConnection(context: Context?): Boolean {
    // Check a network is available
    val mConnectivityManager: ConnectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: Network? = mConnectivityManager.activeNetwork

    if (activeNetwork != null) {
        val bandwidth = mConnectivityManager
                .getNetworkCapabilities(activeNetwork)
                .linkDownstreamBandwidthKbps
        if (bandwidth < minBandwidthKbps) {
            // Request a high-bandwidth network
            Log.d("OnCreate", "Request high-bandwidth network")
            return false
        }
        return true
    } else {
        return false
    }
}

/**
 * Get mac address from IPv6 address
 *
 * @return device mac address
 */
fun getMacAddress(): String {
    try {
        val all: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (nif: NetworkInterface in all) {
            if (nif.name.toLowerCase() != "wlan0") continue

            val macBytes: ByteArray = nif.hardwareAddress ?: return ""

            val res1 = StringBuilder()
            for (b: Byte in macBytes) {
                res1.append(String.format("%02X", b))
            }

            return res1.toString()
        }
    } catch (ex: Exception) {
        println(ex.stackTrace)
    }
    return ""
}



const val PREFS = "uk.ac.ncl.openlab.ongoingness.utilities.PREFS"
const val PREFS_CONFIGURED = "uk.ac.ncl.openlab.ongoingness.utilities.PREFS_CONFIGURED"

fun isConfigured(context: Context): Boolean{
    return context.getSharedPreferences(PREFS, 0).getBoolean(PREFS_CONFIGURED,false)
}

fun setConfigured(context: Context, configured:Boolean){
    context.getSharedPreferences(PREFS, 0).edit().putBoolean(PREFS_CONFIGURED,configured).apply()
}