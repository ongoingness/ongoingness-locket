# Ongoingness Android Wear App

Android Wear application and watch face for the multiple pieces of the Enabling Ongoingness 
project. Each piece is present in the code as distinct build variants.
The app displays media content (Images and GIFs) one at a time in the piece screen, periodicaly 
checks in background for new content in the server and Provides a way for user to navigate 
through the media content.


## Build Variants

### refind
Used in the Refind piece, it displays a total of 6 media items, one belonging to the present collection
and 5 beloging to the past collection. The media belonging to the past collection are chosen based
on the lastest media item added to the present collection, its tags and the tags of the media in the
past collection.

In regards to interaction, the app starts when the screen is tapped or the device is picked up. To move through
the media content, the user can rotate the piece vertically away to get the next media content, and
rotate the piece vertically towards to get the previous media content in the screen.

### locket_touch
Used in the Anew piece, it displays a maximum of 20 media items, 7 beloging to the permanent collection and
13 beloging to the temporary collection. The permanent collection media is always displayed first followed by
the media in the temporary collection.

In regards to interaction, the apps starts by long pressing the screen. To mov ethourgh the media content, the user
can tap the screen.

### locket_touch_inverted
Used in the Anew piece,

### locket_touch_s
Used in the Ivvor piece,


## Description
### The Watch Face
The locket defaults to the normal Android watch face. This displays the Ongoingness logo
but launches the main activity when leaving ambient mode.

### Main Activity
The main activity authenticates the watch using its MAC address. The user must register
this before starting using the web interface. It will then fetch all of the user's media,
splitting them into two collections. It will keep one of these as a permanent collection
and store this to the device. This is updated every time the user updates the collection
on the web interface. The activity will then start the `SensorListeners` after all media
has been downloaded.

### SensorListeners
These are found in `RotationRecogniserOld.kt` and `LightEventListener`. The
`RotationRecogniser` is used to detect a period of inactivity, which will then put the
device into standby. The `LightEventListener` will continually poll the ambient light
sensor. When the light reaches below a certain threshold the locket will be closed,
this will set the screen brightness to `0`. If the light reading is greater than the
threshold it will set screen brightness to max, and then update the image. It will cycle
through all media, alternating between temporary and permanent collections.

## Registering the Device
User's first need to get the MAC address of the device. After this they can register an
account, then register the device.

Register an account at:
```
curl -d "username=<USERNAME>&password=<PASSWORD>" -X POST https://ongoingness-api.openlab.ncl.ac.uk/api/auth/register/
```

Get the authentication token from the return, then register the device at:
```
curl -d "mac=<MAC_ADDRESS>1" -X POST https://ongoingness-api.openlab.ncl.ac.uk/api/devices/
```

## Adding Media
Login to:
```
https://ongoingness.openlab.ncl.ac.uk
```
And then upload your images!
