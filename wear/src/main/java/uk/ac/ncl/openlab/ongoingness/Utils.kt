package uk.ac.ncl.openlab.ongoingness

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
}

/**
 * Store bitmaps to file
 *
 * @param bitmaps Array<Bitmap>
 * @param context Context
 */
fun persistBitmaps(bitmaps : Array<Bitmap>, context: Context) {
    bitmaps.forEach { bitmap : Bitmap ->
        run {
            val imageFile = File(context.filesDir, "${bitmap.hashCode()}.jpg")
            var bos: ByteArrayOutputStream? = null
            try {
                bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                imageFile.writeBytes(bos.toByteArray())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bos?.close()
            }
        }
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

/**
 * Get the stored bitmap from file.
 *
 * @param path
 * @return Bitmap
 */
fun getBitmapFromFile(path: String): Bitmap? {
    try {
        val f = File(path, "last-image.png")
        return BitmapFactory.decodeStream(FileInputStream(f))
    } catch (e: FileNotFoundException) {
    }
    return null
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