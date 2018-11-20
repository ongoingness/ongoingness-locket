package uk.ac.ncl.openlab.ongoingness

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.WindowManager
import android.widget.ImageView
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException


class MainActivity : WearableActivity() {

    var mBackgroundBitmap: Bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
            resources, R.drawable.bg), 350, 350, true)
    var macAddress: String = ""

    private val URL = "http://46.101.47.18:3000/api"
    private var token = ""
    private var links: Array<String> = arrayOf("") // Array of linked media to present
    private var presentId: String = "" // Present Id to show.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateBackground(mBackgroundBitmap)

        macAddress = getMacAddr()
    }

    /**
     * Download an image from a URL.
     */
    private fun downloadImage (url: String, client: OkHttpClient) {
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                e?.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response) {
                try {
                    val inputStream = response.body()?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    updateBackground(bitmap)
                } catch (error: Error) {
                    error.printStackTrace()
                }
            }
        })
    }

    /**
     * Get linked images for the presently shown image.
     */
    private fun getImageIdsInSet (presentId: String, client: OkHttpClient) {
        if (token.isEmpty()) throw Error("Token cannot be empty")

        val url = "$URL/media/links/$presentId"
        val gson = Gson()

        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token)
                .build()

        client.newCall(request)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        System.out.println("error in request")
                        e.printStackTrace()
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val linkResponse: LinkResponse = gson.fromJson(response.body()?.string(), LinkResponse::class.java)
                        val rawLinks = linkResponse.payload
                        links = rawLinks
                    }
                })
    }

    /**
     * Get a token using mac address
     * TODO: Call this when watch face is initialised
     * TODO: Regenerate token on 401
     */
    private fun getToken (client: OkHttpClient) {
        val url = "$URL/auth/mac"
        val gson = Gson()

        println("Getting MAC address")
        val mac: String = getMacAddr()

        // "98:29:A6:BB:F6:72" - default mac
        val formBody = FormBody.Builder()
                .add("mac", mac)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                System.out.println("error in request")
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                val genericResponse: GenericResponse = gson.fromJson(response.body()?.string(), GenericResponse::class.java)
                token = genericResponse.payload

                updateSemanticContext(client)
            }
        })
    }

    private fun updateSemanticContext(client: OkHttpClient) {
        val url = "$URL/media/request/present"
        val gson = Gson()

        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token)
                .build()

        client.newCall(request)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val genericResponse: GenericResponse = gson.fromJson(response.body()?.string(), GenericResponse::class.java)
                        val id = genericResponse.payload
                        presentId = id

                        getImageIdsInSet(presentId, client)
                    }
                })
    }

    private fun updateBackground(bitmap: Bitmap) {
        val background = findViewById<ImageView>(R.id.background)
        background.setImageBitmap(bitmap)
    }
}
