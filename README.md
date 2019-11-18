# Ongoingness Locket
This is a neck worn locket, that updates the image every time the locket is opened.
The locket is built on an Android watch.

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
