package com.moonware.cameraplus;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;
import android.content.Context;
import android.content.res.AssetManager;


/**
 * This class echoes a string called from JavaScript.
 */

public class CameraPlus extends CordovaPlugin {

    /** Common tag used for logging statements. */
    private static final String LOGTAG = "CameraPlus";

    /** Cordova Actions. */

    private static final String ACTION_START_CAMERA = "startCamera";
    private static final String ACTION_STOP_CAMERA = "stopCamera";

    private static final String ACTION_GET_JPEG_IMAGE = "getJpegImage";
    private static final String ACTION_GET_VIDEO_FORMATS = "getVideoFormats";
    private static final String ACTION_SET_VIDEO_FORMATS = "setVideoFormat";
    private static final String ACTION_SET_TEXT = "setText";


    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_START_CAMERA.equals(action)) {
            result = startCamera(args, callbackContext);

        } else if (ACTION_STOP_CAMERA.equals(action)) {
            result = stopCamera(args, callbackContext);

        } else if (ACTION_GET_JPEG_IMAGE.equals(action)) {
            result = getJpegImage(args, callbackContext);

        } else if (ACTION_SET_TEXT.equals(action)) {
            result = setText(args, callbackContext);

        } else if (ACTION_GET_VIDEO_FORMATS.equals(action)) {
            result = getVideoFormats(args, callbackContext);

        } else if (ACTION_SET_VIDEO_FORMATS.equals(action)) {
            result = setVideoFormats(args, callbackContext);

        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(Status.INVALID_ACTION);
        }

        if(result != null) callbackContext.sendPluginResult( result );

        return true;
    }


    private PluginResult startCamera(CordovaArgs inputs, CallbackContext callbackContext) {
        Log.w(LOGTAG, "startCamera");

         // initialize the camera manager :)
         CameraManager.init(cordova.getActivity().getApplicationContext());
         startCapture();

         return null;
    }

    private PluginResult stopCamera(CordovaArgs inputs, CallbackContext callbackContext) {
        Log.w(LOGTAG, "stopCamera");

        // stop Capturing but do not we cannot free the CameraManager :s
        stopCapture();

        return null;
    }

    private PluginResult getJpegImage(CordovaArgs inputs, CallbackContext callbackContext) {
    	Log.w(LOGTAG, "getJpegImage");

        byte[] bArray = CameraManager.lastFrame();

        if (bArray != null)
        {
        	Log.w(LOGTAG, "Received " + String.valueOf(bArray.length) + " bytes...");

        	String imageEncoded = Base64.encodeToString(bArray,Base64.NO_WRAP);

        	callbackContext.success( imageEncoded );
        }
        else
        {
        	callbackContext.error(0);
        }

        return null;
    }

    private PluginResult getVideoFormats(CordovaArgs inputs, CallbackContext callbackContext) {
        Log.w(LOGTAG, "getVideoFormats");

        callbackContext.success( "0" );
        return null;
    }

    private PluginResult setVideoFormats(CordovaArgs inputs, CallbackContext callbackContext) {
        Log.w(LOGTAG, "setVideoFormats");

        callbackContext.success( "false" );
        return null;
    }

    private boolean startCapture(){
        Log.w(LOGTAG, "startCapture");

        if (false){
            CameraManager.setDesiredPreviewSize(1280, 720);
        } else {
            CameraManager.setDesiredPreviewSize(800, 480);
        }

        try {
			      CameraManager.get().openDriver();
		    } catch (IOException e) {
			      Log.w(LOGTAG, "Exception in openDriver");
		    }

        return true;
    }

    private boolean stopCapture(){
        Log.w(LOGTAG, "stopCapture");

        CameraManager.get().stopPreview();

        try {
			      CameraManager.get().closeDriver();
		    } catch (Exception e) {
			      Log.w(LOGTAG, "Exception in closeDriver");
		    }

        return true;
    }

    private PluginResult setText(CordovaArgs args, CallbackContext callbackContext) {

        String text = args.getString(0);
        boolean res = CameraManager.setText(text);

        if (res == true)
        {
        	callbackContext.success( 1 );
        }
        else
        {
        	callbackContext.error( 0 );
        }

        return null;
    }


    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    public void onPause(boolean multitasking) {
    	//if(! multitasking) __stopServer();
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    public void onResume(boolean multitasking) {
    	//if(! multitasking) __startServer();
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    public void onDestroy() {
    	//__stopServer();
    }
}
