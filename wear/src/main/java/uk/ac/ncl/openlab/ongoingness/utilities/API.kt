package uk.ac.ncl.openlab.ongoingness.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.*
import uk.ac.ncl.openlab.ongoingness.BuildConfig
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

interface OngoingnessService{

    @POST("auth/mac")
    fun requestToken(@Body mac:String): Observable<GenericResponse>

    @GET("media")
    fun fetchCollection(@Header("x-access-token") token: String?): Observable<MediaResponse>

    @GET("media/{link}")
    @Streaming
    fun fetchMedia(@Header("x-access-token") token: String?, @Path("link") link: String): Observable<ResponseBody>
}


class API2 {

    var retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .build()
    var service = retrofit.create(OngoingnessService::class.java)
    lateinit var token: String
    var disposables: ArrayList<Disposable> = ArrayList()

    /**
     * Generate a token from the api using the devices mac address.
     *
     * @param callback function to call after generating a token
     */
    fun generateToken(callback: (token: String?) -> Unit) {
        val mac: String = getMacAddress() // Get mac address
        val disposable = service.requestToken(mac)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response ->
                    token = response.payload
                    callback(token)
                }
        disposables.add(disposable)
    }


    /**
     * Fetch all media from the api.
     *
     * @param callback function to call after retrieving the media
     */
    fun fetchAllMedia(callback: (Array<Media>?) -> Unit) {
        val disposable = service.fetchCollection(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response -> callback(response.payload) }
        disposables.add(disposable)
    }


    /**
     * Fetch image from the link.
     *
     * @param links Array<String>
     */
    fun fetchBitmap(link: String, callback: (Bitmap?) -> Unit) {
        val disposable = service.fetchMedia(token, link)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response ->
                    callback(BitmapFactory.decodeStream(response.byteStream()))
                }
        disposables.add(disposable)
    }


    fun flush(){
        disposables.forEach { d -> d.dispose() }
    }
}

class API {

    val gson = Gson()

    private val apiUrl = BuildConfig.API_URL
    private var token: String? = null
    private val client: OkHttpClient = OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.MINUTES)
            .connectionSpecs(
                    Arrays.asList(ConnectionSpec.MODERN_TLS,
                            ConnectionSpec.CLEARTEXT))
            .addInterceptor { chain ->

                var request = chain.request()

                var response = chain.proceed(request)

                var tryCount = 0

                while(!response!!.isSuccessful && tryCount < 3) {
                    Log.d("intercept", "Request is not successful - $tryCount")
                    tryCount++
                    response = chain.proceed(request)
                }
                response

            }
            .build()

    init {
        generateToken (callback = {}, onFailureCallback = {})
    }

    /**
     * Generate a token from the api using the devices mac address.  ? = null
     *
     * @param callback function to call after generating a token
     */
    private fun generateToken(callback: (token:String?) -> Unit, onFailureCallback: (e: IOException) -> Unit) {

        if(token != null){
            callback(token)
            return
        }
        val mac: String = getMacAddress() // Get mac address
        Log.d("MAC Address", "$mac")
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
                if(response.code().toString().startsWith('5'))
                    onFailureCallback(IOException("No server"))
                else {
                    val genericResponse: GenericResponse = gson.fromJson(
                            response.body()?.string(),
                            GenericResponse::class.java)

                    // Set token
                    token = genericResponse.payload
                    callback(token)
                }
            }
        })
    }


    /**
     * Fetch all media from the api.
     *
     * @param callback function to call after retrieving the media
     */
    fun fetchMedia(callback: (Array<Media>?) -> Unit) {

        generateToken(callback = { token ->  Log.d("API",token)

            val url = "media"
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
                    val mediaResponse: MediaResponse = gson.fromJson(
                            response.body()?.string(),
                            MediaResponse::class.java)
                    callback(mediaResponse.payload)
                }
            })
        }, onFailureCallback = {})

    }

    fun fetchMediaPayload(callback: (Response?) -> Unit, failure: (e: IOException) -> Unit) {

        generateToken( callback = { token ->  Log.d("API",token)

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
     * Fetch Inferred media from the api.
     *
     * @param callback function to call after retrieving the media
     */
    fun fetchInferredMedia(mediaID: String, callback: (Response?) -> Unit) {

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
        }, onFailureCallback = {} )
    }

    /**
     * Fetch image from the link.
     *
     * @param links Array<String>
     */
    fun fetchBitmap(link:String, size:Int = 300, callback: (ResponseBody?) -> Unit) {

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
                }

                override fun onResponse(call: Call?, response: Response) {
                    Log.d("API","Got image: "+response.body().toString())
                    callback(response.body())
                }
            })
        }, onFailureCallback = {})
    }

    /**
     * Fetch image from the link.
     *
     * @param links Array<String>
     */
    fun sendLogs(logs:String, callback: (ResponseBody?) -> Unit, failure: (e: IOException) -> Unit) {

        generateToken( callback =  { token ->
            Log.d("API", token)

            val formBody = FormBody.Builder()
                    .add("logs", logs)
                    .build()

            val url = "log"
            val request = Request.Builder()
                    .url(apiUrl + url)
                    .post(formBody)
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

