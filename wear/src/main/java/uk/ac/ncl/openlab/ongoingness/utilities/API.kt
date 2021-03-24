package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit

/**
 * Allows interactions with the Ongoingness API.
 *
 * @author Luis Carvalho, Daniel Welsh
 */
class API(val context: Context) {

    val gson = Gson()

    /**
     * API server base end-point.
     */
    private val apiUrl = BuildConfig.API_URL

    /**
     * User authentication token.
     */
    private var token: String? = null

    /**
     * Client in charge of handling the communications.
     */
    private val client: OkHttpClient = OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.MINUTES)
            .connectionSpecs(
                    listOf(ConnectionSpec.MODERN_TLS,
                            ConnectionSpec.CLEARTEXT))
            .addInterceptor { chain ->


                var request = chain.request()

                var response : Response? = null

                try {
                    response = chain.proceed(request)

                    var tryCount = 0

                    while (!response!!.isSuccessful && tryCount < 3) {
                        Log.d("intercept", "Request is not successful - $tryCount")
                        tryCount++
                        response = chain.proceed(request)
                    }
                } catch (e: Exception) {
                    recordFailure(context)
                    throw e
                }
                response!!

            }
            .build()

    /**
     * Generate a authentication token from the api using the devices wifi mac address.
     *
     * @param callback function to call after generating a token
     */
    private fun generateToken(callback: (token:String?) -> Unit, onFailureCallback: (e: IOException) -> Unit) {

        Log.d("generateToken", "called")

        if(token != null){
            callback(token)
            return
        }
        val mac: String = getMacAddress() // Get mac address
        Log.d("MAC Address", mac)
        Log.d("API URL", apiUrl)
        val formBody = FormBody.Builder()
                .add("mac", mac)
                .build()
        val request = Request.Builder()
                .url(apiUrl+"auth/mac")
                .post(formBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onFailureCallback(e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("ttt", response.toString())
                if(response.code().toString().startsWith('5') || response.code().toString().startsWith('4') ) {
                    val code = response.code().toString()
                    response.close()
                    onFailureCallback(IOException(code))
                } else {
                    try {
                        val genericResponse: GenericResponse = gson.fromJson(
                                response.body()?.string(),
                                GenericResponse::class.java)

                        // Set token
                        token = genericResponse.payload
                        callback(token)
                    } catch (e: Exception) {
                        onFailureCallback(IOException(e))
                    }
                }
            }
        })
    }


    /**
     *  Fetch all media items of the authenticated user.
     *
     *  @param callback function called after fetching the media.
     *  @param failure function called if the fetching fails.
     *
     */
    fun fetchMediaPayload(callback: (Response?) -> Unit, failure: (e: IOException) -> Unit) {

        generateToken( callback = { token ->

            Log.d("API",token)

            val url = "media"
            val request = Request.Builder()
                    .url(apiUrl + url)
                    .get()
                    .header("x-access-token", token!!)
                    .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    failure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback(response)
                }


            })
        }, onFailureCallback = { e ->
           failure(e)
        })
    }


    /**
     * Fetch inferred media of the authenticated user.
     *
     * @param mediaID id of the current media item in device belonging to present collection.
     * @param callback function called after fetching the media.
     * @param failure function called if the fetching fails.
     */
    fun fetchInferredMedia(mediaID: String, callback: (Response?) -> Unit, failure: (e: IOException) -> Unit) {

        generateToken( callback =  { token ->  Log.d("API",token)

            val url = "media/linkedMediaAll_Weighted/?mediaId=$mediaID&drawIfNew=1"
            val request = Request.Builder()
                    .url(apiUrl + url)
                    .get()
                    .header("x-access-token", token!!)
                    .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    callback(response)
                }
            })
        }, onFailureCallback = { e ->
            failure(e)
        })
    }

    /**
     * Fetch media file from a given link.
     *
     * @param link link of the media file.
     * @param size dimensions of the media file.
     * @param callback function called after fetching the media file.
     * @param failure function called if the fetching fails.
     */
    fun fetchBitmap(link:String, size:Int = 300, callback: (ResponseBody?) -> Unit, failure: (e: IOException) -> Unit) {

        generateToken( callback =  { token ->
            Log.d("API", token)

            val url = "media/$link/?size=$size"
            val request = Request.Builder()
                    .url(apiUrl + url)
                    .get()
                    .header("x-access-token", token!!)
                    .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    Log.e("API", "Error here:"+e.toString())
                    failure(e!!)
                }

                override fun onResponse(call: Call?, response: Response) {
                    Log.d("API","Got image: "+response.body().toString())
                    callback(response.body())
                }
            })
        }, onFailureCallback = { e ->
            failure(e)
        })
    }

    /**
     * Sends a given string of logs.
     *
     * @param logs JSON formatted string of logs to be sent.
     * @param callback function called after successfully sending the logs.
     * @param failure function called if the operation fails.
     */
    fun sendLogs(logs:String, callback: (ResponseBody?) -> Unit, failure: (e: IOException) -> Unit) {

        generateToken( callback =  { token ->
            Log.d("API", token)

            val JSON: MediaType? = MediaType.parse("application/json; charset=utf-8")
            val jsonObject = JSONObject();
            jsonObject.put("logs", JSONArray(logs))
            val body : RequestBody  = RequestBody.create(JSON, jsonObject.toString());

            val url = "log"
            val request = Request.Builder()
                    .url(apiUrl + url)
                    .post(body)
                    .header("x-access-token", token!!)
                    .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    Log.e("API", "Error here:"+e.toString())
                    failure(e!!)
                }

                override fun onResponse(call: Call?, response: Response) {
                    callback(response.body())
                }
            })
        }, onFailureCallback = {
            failure(it)
        })
    }
}

