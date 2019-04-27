package uk.ac.ncl.openlab.ongoingness

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.*

class API {

    private val apiUrl = BuildConfig.API_URL
    private var token: String? = null
    private val client: OkHttpClient = OkHttpClient
            .Builder()
            .connectionSpecs(
                    Arrays.asList(ConnectionSpec.MODERN_TLS,
                            ConnectionSpec.CLEARTEXT))
            .build()

    init {
        generateToken { Log.d("API",token) }
    }

    /**
     * Generate a token from the api using the devices mac address.
     *
     * @param callback function to call after generating a token
     */
    fun generateToken(callback: (token:String?) -> Unit) {

        if(token != null){
            callback(token)
            return
        }

        val url = "$apiUrl/auth/mac"
        val gson = Gson()

        val mac: String = getMacAddress() // Get mac address
        val formBody = FormBody.Builder()
                .add("mac", mac)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val genericResponse: GenericResponse = gson.fromJson(
                        response.body()?.string(),
                        GenericResponse::class.java)

                // Set token
                token = genericResponse.payload
                callback(token)
            }
        })
    }


    /**
     * Fetch all media from the api.
     *
     * @param callback function to call after retrieving the media
     */
    fun fetchAllMedia(callback: (Array<Media>?) -> Unit) {

        generateToken {
            if(token == null) {
                println("No Token")
                return@generateToken
            }
        }


        val gson = Gson()
        val url = "$apiUrl/media"

        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token!!)
                .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val mediaResponse: MediaResponse = gson.fromJson(
                        response.body()?.string(),
                        MediaResponse::class.java)
                callback(mediaResponse.payload)
            }
        })
    }



    /**
     * Fetch image from the link.
     *
     * @param links Array<String>
     */
    fun fetchBitmap(link:String, callback: (Bitmap?) -> Unit) {

        if(token == null) {
            callback(null)
            println("No Token")
            return
        }

        val url = "$apiUrl/media/$link/"
        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token!!)
                .build()

        Log.d("fetchBitmaps", "Fetching bitmap from $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("API",e.toString())
            }

            /**
             * Create a bitmap from the returned image.
             */
            override fun onResponse(call: Call?, response: Response) {
                try {
                    // Get an input stream
                    val inputStream = response.body()?.byteStream()
                    BitmapFactory.decodeStream(inputStream)
                    callback(BitmapFactory.decodeStream(inputStream))
                } catch (error: Error) {
                    error.printStackTrace()
                    callback(null)
                }
            }
        })
        }
}
