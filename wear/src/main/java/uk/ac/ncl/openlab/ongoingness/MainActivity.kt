package uk.ac.ncl.openlab.ongoingness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.support.wear.widget.BoxInsetLayout
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.*


class MainActivity : WearableActivity() {

    var mBackgroundBitmap: Bitmap? = null
    private val URL = "http://46.101.47.18:3000/api"
    private var token = ""
    private var links: Array<String> = arrayOf("") // Array of linked media to present
    private var presentId: String = "" // Present Id to show.
    private var linkIdx: Int = -1
    private var client: OkHttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mBackgroundBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                resources, R.drawable.bg), 400, 400, true)
        updateBackground(mBackgroundBitmap!!)

        client = OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                .build()

        Toast.makeText(this, "in activity", Toast.LENGTH_SHORT).show()

        System.out.println("Getting connection")

        val minBandwidthKbps = 320
        val mConnectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network? = mConnectivityManager.activeNetwork

        if (activeNetwork == null) {
            val bandwidth = mConnectivityManager.getNetworkCapabilities(activeNetwork).linkDownstreamBandwidthKbps
            if (bandwidth < minBandwidthKbps) {
                // Request a high-bandwidth network
                System.out.println("Request high-bandwidth network")
            }
        } else {
            // You already are on a high-bandwidth network, so start your network request
            System.out.println("Got network")
            getToken(client)
        }

        rotationRecogniser = RotationRecogniser(this)
    }


    override fun onResume() {
        super.onResume()
        rotationRecogniser?.start(rotationListener)
    }

    override fun onPause() {
        super.onPause()
        rotationRecogniser?.stop()
    }

    /**
     * Download an image from a URL.
     */
    private fun downloadImage (url: String, client: OkHttpClient?) {
        val request = Request.Builder().url(url).build()

        client?.newCall(request)?.enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                e?.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response) {
                try {
                    val inputStream = response.body()?.byteStream()
                    val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), 400, 400, false)

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
    private fun getToken (client: OkHttpClient?) {
        val url = "$URL/auth/mac"
        val gson = Gson()

        println("Getting MAC address")
        val mac: String = getMacAddr()

        val formBody = FormBody.Builder()
                .add("mac", mac)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

        client?.newCall(request)?.enqueue(object : Callback {
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

    private fun updateSemanticContext(client: OkHttpClient?) {
        val url = "$URL/media/request/present"
        val gson = Gson()

        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token)
                .build()

        client?.newCall(request)
                ?.enqueue(object : Callback {
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
        val background = findViewById<BoxInsetLayout>(R.id.background)
        background.background = BitmapDrawable(resources, bitmap)
//        background.setImageBitmap(bitmap)
    }

    private fun cycle(client: OkHttpClient?) {
        // Return if there are no more images in cluster
        if(links.isEmpty() || links[0].isBlank()) {
            return
        }

        // Increment index with direction
        linkIdx++

        // If imageIndex rolls over array size then show the present image.
        if (linkIdx == links.size) {
            linkIdx = -1
        }

        val imgId: String = if (linkIdx == -1) {
            presentId
        } else {
            links[linkIdx]
        }

        // Download image and draw a new watch face
        downloadImage("$URL/media/show/$imgId/$token", client)
    }

    var rotationRecogniser: RotationRecogniser? = null
    val rotationListener = object:RotationRecogniser.Listener{
        override fun onOrientationChange(orientation: RotationRecogniser.Orientation) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onRotateUp() {
            cycle(client)
        }

        override fun onRotateDown() {
            cycle(client)
        }

        override fun onRotateLeft() {
            updateSemanticContext(client)
        }

        override fun onRotateRight() {
            updateSemanticContext(client)
        }

        override fun onStandby() {
            finish()
        }
    }
}
