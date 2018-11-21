package uk.ac.ncl.openlab.ongoingness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.support.wear.widget.BoxInsetLayout
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.*


class MainActivity : WearableActivity() {

    private val URL = "http://46.101.47.18:3000/api"
    private var token = ""
    private var links: Array<String> = arrayOf("") // Array of linked media to present
    private var presentId: String = "" // Present Id to show.
    private var linkIdx: Int = -1
    private var client: OkHttpClient? = null
    private var screenSize: Int = 400
    private val minBandwidthKbps: Int = 320
    private var mediaList = ArrayList<Bitmap>()
    private var presentImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Create a background bit map from drawable, and overdraw to 400
        updateBackground(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                resources, R.drawable.bg), screenSize, screenSize, true)!!)

        // Build a OK HTTP client
        client = OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                .build()

        Log.d("OnCreate", "Getting a connection")

        // Check a network is available
        val mConnectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network? = mConnectivityManager.activeNetwork

        if (activeNetwork == null) {
            val bandwidth = mConnectivityManager.getNetworkCapabilities(activeNetwork).linkDownstreamBandwidthKbps
            if (bandwidth < minBandwidthKbps) {
                // Request a high-bandwidth network
                Log.d("OnCreate", "Request high-bandwidth network")
            }
        } else {
            // You already are on a high-bandwidth network, so start your network request
            Log.d("OnCreate", "Got a network")
            getToken(client)
        }

        rotationRecogniser = RotationRecogniser(this)
    }

    // Restart the activity recogniser
    override fun onResume() {
        super.onResume()
        rotationRecogniser?.start(rotationListener)
    }

    // Pause the activity recogniser
    override fun onPause() {
        super.onPause()
        rotationRecogniser?.stop()
    }

    /**
     * Get linked images for the presently shown image.
     * Store links in an array
     *
     * @param presentId Id of item of media to collect linked images from.
     * @param client client to make http requests.
     */
    private fun getImageIdsInSet (presentId: String, client: OkHttpClient) {
        if (token.isEmpty()) throw Error("Token cannot be empty")

        val url = "$URL/media/links/$presentId"
        val gson = Gson()

        Log.d("getImageIdsInSet", "Getting ids in set")

        val request = Request.Builder()
                .url(url)
                .header("x-access-token", token)
                .build()

        client.newCall(request)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("getImageIdsInSet", "Error in request")
                        e.printStackTrace()
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val linkResponse: LinkResponse = gson.fromJson(response.body()?.string(), LinkResponse::class.java)
                        val rawLinks = linkResponse.payload
                        links = rawLinks

                        // Clear array of image bitmaps.
                        mediaList.clear()
                        linkIdx = -1

                        // Pre fetch images
                        fetchBitmaps(client)
                    }
                })
    }

    /**
     * Get a token using mac address
     *
     * @param client OkHttpClient requires http client to authenticate the device and fetch a token
     * from the API.
     */
    private fun getToken (client: OkHttpClient?) {
        val url = "$URL/auth/mac"
        val gson = Gson()

        // Get mac address
        val mac: String = getMacAddr()

        val formBody = FormBody.Builder()
                .add("mac", mac)
                .build()
        val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

        client!!.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                val genericResponse: GenericResponse = gson.fromJson(response.body()?.string(), GenericResponse::class.java)

                // Set token
                token = genericResponse.payload

                // Update the semantic context, force background to be an image of the present on boot.
                updateSemanticContext(client)
            }
        })
    }

    /**
     * Get an id of an image from the present,
     * get links of new item of media
     * draw the image of the present on the background.
     *
     * Requires a client to get the next present image id from api, get new images, and download
     * the next image to show.
     *
     * @param client OkHttpClient
     */
    private fun updateSemanticContext(client: OkHttpClient?) {
        val url = "$URL/media/request/present"
        val gson = Gson()

        Log.d("updateSemanticContext", "Updating semantic context")

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
                        fetchPresentImage(client)
                    }
                })
    }

    /**
     * Update the background of the watch face.
     *
     * @param bitmap The bitmap to set the background to.
     * @return Unit
     */
    private fun updateBackground(bitmap: Bitmap) {
        runOnUiThread {
            // Stuff that updates the UI
            val background = findViewById<BoxInsetLayout>(R.id.background)
            background.background = BitmapDrawable(resources, bitmap)
        }
    }

    /**
     * Cycle to next image in the semantic set.
     */
    private fun cycle(direction: Int) {
        // Return if there are no more images in cluster
        if(mediaList.isEmpty()) {
            return
        }

        linkIdx += direction

        when(direction) {
            -1 -> {
                if (linkIdx < 0) {
                    updateBackground(presentImage!!)
                    linkIdx = -1
                } else {
                    updateBackground(mediaList[linkIdx])
                }
            }
            1 -> {
                if (linkIdx == mediaList.size) {
                    linkIdx = mediaList.size - 1
                    updateBackground(mediaList[linkIdx])
                } else {
                    updateBackground(mediaList[linkIdx])
                }
            }
        }
    }

    /**
     * Fetch images of of the past and add to an array list.
     */
    private fun fetchBitmaps(client: OkHttpClient?) {
        for (id in links) {
            val url = "$URL/media/show/$id/$token"
            val request = Request.Builder().url(url).build()

            Log.d("fetchBitmaps", "Fetching bitmap from $url")

            client?.newCall(request)?.enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    e?.printStackTrace()
                }

                override fun onResponse(call: Call?, response: Response) {
                    try {
                        val inputStream = response.body()?.byteStream()
                        mediaList.add(Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), screenSize, screenSize, false))
                    } catch (error: Error) {
                        error.printStackTrace()
                    }
                }
            })
        }
    }

    /**
     * Fetch and set the present image.
     *
     * @param client httpClient
     */
    private fun fetchPresentImage(client: OkHttpClient) {
        val url = "$URL/media/show/$presentId/$token"
        val request = Request.Builder().url(url).build()

        Log.d("fetchPresentImage", "Fetching image from $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                e?.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response) {
                try {
                    val inputStream = response.body()?.byteStream()
                    presentImage = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), screenSize, screenSize, false)

                    updateBackground(presentImage!!)
                } catch (error: Error) {
                    error.printStackTrace()
                }
            }
        })
    }

    var rotationRecogniser: RotationRecogniser? = null
    val rotationListener = object:RotationRecogniser.Listener{

        override fun onRotateUp() {
            cycle(1)
        }

        override fun onRotateDown() {
            cycle(-1)
        }

        override fun onRotateLeft() {
            updateSemanticContext(client)
        }

        override fun onRotateRight() {
            updateSemanticContext(client)
        }

        override fun onStandby() {
            Log.d("WATCH","standby")
            finish()
        }
    }
}
