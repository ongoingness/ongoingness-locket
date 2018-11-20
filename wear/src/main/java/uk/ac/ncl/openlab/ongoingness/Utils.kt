package uk.ac.ncl.openlab.ongoingness
import java.net.NetworkInterface
import java.util.*

fun getMacAddr(): String {
    try {
        val all: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
        for(nif: NetworkInterface in all) {
            if (nif.name.toLowerCase() != "wlan0") continue

            val macBytes: ByteArray = nif.hardwareAddress ?: return ""

            val res1 = StringBuilder()
            for (b: Byte in macBytes) {
                res1.append(String.format("%02X", b))
            }

            println("MAC:" + res1.toString())
            return res1.toString()
        }
    } catch (ex: Exception) {
        println(ex.stackTrace)
    }
    return ""
}