package com.moonware.cameraplus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.ImageFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public final class CameraManager {

    private static CameraManager cameraManager;

    private final Context context;
    public final CameraConfigurationManager configManager;
    public Camera camera;
    private boolean initialized;
    public boolean previewing;

    private SurfaceTexture surfaceTexture;

    public static int mDesiredWidth = 1280;
    public static int mDesiredHeight = 720;

    public static boolean DEBUG = true;
    public static String TAG = "CameraManager";

    public static void setDesiredPreviewSize(int width, int height) {
        mDesiredWidth = width;
        mDesiredHeight = height;
    }

    public Point getMaxResolution() {

        if (camera != null)
            return CameraConfigurationManager.getMaxResolution(camera.getParameters());
        else
            return null;
    }

    public Point getNormalResolution(Point normalRes) {

        if (camera != null)
            return CameraConfigurationManager.getCameraResolution(camera.getParameters(), normalRes);
        else
            return null;
    }

    public final PreviewCallback previewCallback;

    public static void init(Context context) {
        if (cameraManager == null) {
            cameraManager = new CameraManager(context);
        }
    }

    public static CameraManager get() {
        return cameraManager;
    }

    public static byte[] lastFrame() {
        return cameraManager.previewCallback.getLastFrame();
    }

    private CameraManager(Context context) {

        Log.i(TAG, "Creating instance of CameraManager...");

        this.surfaceTexture = new SurfaceTexture(10);

        this.context = context;
        this.configManager = new CameraConfigurationManager(context);

        previewCallback = new PreviewCallback(configManager, this);
    }

    public void openDriver() throws IOException {

        if (camera == null) {
            if (DEBUG) Log.i(TAG, "Camera opening...");
            camera = Camera.open();
            if (camera == null) {
                if (DEBUG) Log.i(TAG, "First camera open failed");
                camera = Camera.open(0);

                if (camera == null){
                    if (DEBUG) Log.i(TAG, "Second camera open failed");
                    throw new IOException();
                }
            }

            if (DEBUG) Log.i(TAG, "Camera open success");

            camera.setPreviewDisplay(null);

            if (android.os.Build.VERSION.SDK_INT >= 9) {
                setCameraDisplayOrientation(0, camera);
            }

            if (surfaceTexture != null){
                camera.setPreviewTexture(surfaceTexture);
                if (DEBUG) Log.i(TAG, "Set camera current texture");
            } else {

                if (DEBUG) Log.i(TAG, "Camera texture is NULL");
            }

            if (!initialized) {
                initialized = true;
                configManager.initFromCameraParameters(camera);
                if (DEBUG) Log.i(TAG, "configManager initialized");
            }
            configManager.setDesiredCameraParameters(camera);
            if (DEBUG) Log.i(TAG, "Camera set desired parameters");


        } else {
            if (DEBUG) Log.i(TAG, "Camera already opened");
        }
    }

    public int getMaxZoom() {

        if (camera == null)
            return -1;

        Parameters cp = camera.getParameters();
        if (!cp.isZoomSupported()){
            return -1;
        }

        List<Integer> zoomRatios =  cp.getZoomRatios();

        return zoomRatios.get(zoomRatios.size()-1);
    }

    public void setZoom(int zoom){

        if (camera == null)
            return;

        final Parameters cp = camera.getParameters();

        int minDist = 100000;
        int bestIndex = 0;

        if (zoom == -1) {
            int zoomIndex = cp.getZoom() - 1;

            if (zoomIndex >= 0){
                zoom = cp.getZoomRatios().get(zoomIndex);
            }
        }

        List<Integer> zoomRatios =  cp.getZoomRatios();

        for (int i = 0; i < zoomRatios.size(); i++){
            int z = zoomRatios.get(i);

            if (Math.abs(z - zoom) < minDist){
                minDist = Math.abs(z - zoom);
                bestIndex = i;
            }
        }

        final int fBestIndex = bestIndex;

        cp.setZoom(fBestIndex);
        camera.setParameters(cp);
    }

    public boolean isTorchAvailable() {

        if (camera == null)
            return false;

        Parameters cp = camera.getParameters();
        List<String> flashModes = cp.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Parameters.FLASH_MODE_TORCH))
            return true;
        else
            return false;
    }

    public void setTorch(final boolean enabled) {

        if (camera == null)
            return;

        try {
            final Parameters cp = camera.getParameters();

            List<String> flashModes = cp.getSupportedFlashModes();

            if (flashModes != null && flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                //camera.cancelAutoFocus();

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (camera != null){
                            if (enabled)
                                cp.setFlashMode(Parameters.FLASH_MODE_TORCH);
                            else
                                cp.setFlashMode(Parameters.FLASH_MODE_OFF);
                            camera.setParameters(cp);
                        }

                    }
                }, 300);
            }
        } catch (Exception e) {

        }
    }

    public float[] getExposureCompensationRange(){

        if (camera == null)
            return null;

        try{

            Parameters cp = camera.getParameters();

            float ecStep = cp.getExposureCompensationStep();
            float minEC = cp.getMinExposureCompensation();
            float maxEC = cp.getMaxExposureCompensation();

            float[] res = new float[3];
            res[0] = minEC;
            res[1] = maxEC;
            res[2] = ecStep;

            return res;

        } catch (Exception e) {

            return null;
        }
    }

    public void setExposureCompensation(float value) {

        if (camera == null)
            return;

        try{

            Parameters cp = camera.getParameters();

            //int currentEC = cp.getExposureCompensation();
            //float ecStep = cp.getExposureCompensationStep();

            float minEC = cp.getMinExposureCompensation();
            float maxEC = cp.getMaxExposureCompensation();

            if (value > maxEC)
                value = maxEC;
            if (value < minEC)
                value = minEC;

            cp.setExposureCompensation((int) value);

            camera.setParameters(cp);

            //Log.d("exposure compensation", String.valueOf(value));

        } catch (Exception e) {
            //Log.d("exposure compensation", "failed to set");
        }
    }

    public void closeDriver() {

        if (camera != null) {

            camera.release();
            camera = null;
        }
    }

    public void startPreview() {
        if (camera != null && !previewing) {

            if (DEBUG) Log.i(TAG, "Starting preview");

            camera.startPreview();

            int targetRotation = 0;

            if (android.os.Build.VERSION.SDK_INT >= 9) {
                targetRotation = getDisplayOrientation(0);
            }
            previewCallback.setRotation(targetRotation);

            Camera.Parameters parameters = camera.getParameters();
            int previewFormat = parameters.getPreviewFormat();
            int previewWidth = parameters.getPreviewSize().width;
            int previewHeight = parameters.getPreviewSize().height;

            previewCallback.setPreviewDimensions(previewWidth, previewHeight);
            previewCallback.setPreviewFormat(previewFormat);

            if (DEBUG) Log.i(TAG, "Setting Standard Callback");
            camera.setPreviewCallback(previewCallback);

            previewing = true;
        }
    }

    public void stopPreview() {
        if (camera != null && previewing) {

            if (DEBUG) Log.i(TAG, "Stopping preview");

            camera.setPreviewCallback(null);

            camera.stopPreview();

            previewing = false;
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public int getDisplayOrientation(int cameraId) {

         android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
         android.hardware.Camera.getCameraInfo(cameraId, info);

         Log.d(TAG, "Camera Orientation: " + info.orientation);

         Display d = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

         int rotation = d.getRotation();

         Log.d(TAG, "Display Rotation: " + rotation);

         int degrees = 0;
         switch (rotation) {
             case Surface.ROTATION_0: degrees = 0; break;
             case Surface.ROTATION_90: degrees = 90; break;
             case Surface.ROTATION_180: degrees = 180; break;
             case Surface.ROTATION_270: degrees = 270; break;
         }

         int result;
         if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
             result = (info.orientation + degrees) % 360;
             result = (360 - result) % 360;  // compensate the mirror
         } else {  // back-facing
             result = (info.orientation - degrees + 360) % 360;
         }

         Log.d(TAG, "Image must be rotated to: " + result);

         return result;
     }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public int setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {

         int result = getDisplayOrientation(cameraId);
         camera.setDisplayOrientation(result);

         return result;
     }

}

final class CameraConfigurationManager {

    private static final String TAG = CameraConfigurationManager.class.getSimpleName();

    private final Context context;
    public static Point screenResolution;
    public Point cameraResolution;
    private int previewFormat;
    private String previewFormatString;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        previewFormat = parameters.getPreviewFormat();
        previewFormatString = parameters.get("preview-format");
        Log.d(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        screenResolution = new Point(display.getWidth(), display.getHeight());
        Log.d(TAG, "Screen resolution: " + screenResolution);
        cameraResolution = getCameraResolution(parameters, new Point(CameraManager.mDesiredWidth, CameraManager.mDesiredHeight));
        Log.d(TAG, "Camera resolution: " + cameraResolution);

    }

    void setDesiredCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        cameraResolution = getCameraResolution(parameters, new Point(CameraManager.mDesiredWidth, CameraManager.mDesiredHeight));
        Log.d(TAG, "Setting preview size: " + cameraResolution);
        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);

        try {
            String vs =  parameters.get("anti-shake");
            if (vs != null) {
                parameters.set("anti-shake", "1");
            }
        } catch (Exception e){
        }

        try {
            String vs = parameters.get("preview-fps-range");
            if (vs != null) {
                Log.d(TAG, "Setting fps preview range");

                parameters.set("preview-fps-range", "30000,30000");
            }

        } catch (Exception e){
        }

        try {
            String vs = parameters.get("video-stabilization");
            if (vs != null) {
                parameters.set("video-stabilization", "true");
            }
        } catch (Exception e){
        }

        try {
            String vs = parameters.get("video-stabilization-ocr");
            if (vs != null) {
                parameters.set("video-stabilization-ocr", "true");
            }
        } catch (Exception e){
        }

        try {
            String vs = parameters.get("touch-af-aec-values");
            if (vs != null) {
                parameters.set("touch-af-aec-values", "touch-on");
            }
        } catch (Exception e){
        }

        try {
             String vs =  parameters.get("metering-areas");
             if (vs != null) {
                   parameters.set("metering-areas", "(-200,-10,200,50,1)");
             }
        } catch (Exception e){
        }

        try {
             String vs =  parameters.get("focus-areas");
             if (vs != null) {
                   parameters.set("focus-areas", "(-200,-10,200,50,1)");
             }
        } catch (Exception e){
        }

        //String focusMode = parameters.getFocusMode();

        try{
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
        } catch (Exception e){

            try{
                parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            } catch (Exception e2){

            }
        }

        Log.d(TAG, "Camera parameters flat: " + parameters.flatten());
        camera.setParameters(parameters);
    }

    Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return screenResolution;
    }

    int getPreviewFormat() {
        return previewFormat;
    }

    String getPreviewFormatString() {
        return previewFormatString;
    }

    public static Point getMaxResolution(Camera.Parameters parameters) {

        List<Size> sizes = parameters.getSupportedPreviewSizes();

        int maxIndex = -1;
        int maxSize = 0;

        for (int i = 0; i < sizes.size(); i++) {
            int size = sizes.get(i).width * sizes.get(i).height;
            if (size > maxSize) {
                maxSize = size;
                maxIndex = i;
            }
        }

        return new Point(sizes.get(maxIndex).width, sizes.get(maxIndex).height);
    }

    public static Point getCameraResolution(Camera.Parameters parameters, Point desiredResolution) {

        String previewSizeValueString = parameters.get("preview-size-values");

        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }

        Point cameraResolution = null;

        /*
        if (CameraManager.mDesiredWidth == 0)
            CameraManager.mDesiredWidth = desiredResolution.x;
        if (CameraManager.mDesiredHeight == 0)
            CameraManager.mDesiredHeight = desiredResolution.y;*/

        List<Size> sizes = parameters.getSupportedPreviewSizes();

        int minDif = 99999;
        int minIndex = -1;

        float screenAR = ((float)CameraConfigurationManager.screenResolution.x) / CameraConfigurationManager.screenResolution.y;

        for (int i = 0; i < sizes.size(); i++) {

            //float resAR = ((float)sizes.get(i).width) / sizes.get(i).height;

            int dif = Math.abs(sizes.get(i).width - desiredResolution.x) + Math.abs(sizes.get(i).height - desiredResolution.y);

            //int dif = Math.abs((sizes.get(i).width * sizes.get(i).height) - (CameraManager.mDesiredWidth * CameraManager.mDesiredHeight));

            if (dif < minDif) {
                minDif = dif;
                minIndex = i;
            }
        }

        float desiredTotalSize = desiredResolution.x * desiredResolution.y;
        float bestARdifference = 100;


        for (int i = 0; i < sizes.size(); i++) {

            float resAR = ((float)sizes.get(i).width) / sizes.get(i).height;

            float totalSize = sizes.get(i).width * sizes.get(i).height;

            float difference;

            if (totalSize >= desiredTotalSize){
                difference = totalSize / desiredTotalSize;
            } else {
                difference = desiredTotalSize / totalSize;
            }

            float ARdifference;

            if (resAR >= screenAR){
                ARdifference = resAR / screenAR;
            } else {
                ARdifference = screenAR / resAR;
            }

            if (difference < 1.1 && ARdifference < bestARdifference){
                bestARdifference = ARdifference;
                minIndex = i;
            }
        }

        cameraResolution = new Point(sizes.get(minIndex).width, sizes.get(minIndex).height);

        return cameraResolution;
    }
}

final class PreviewCallback implements Camera.PreviewCallback {

    int fpscount;
    long lasttime = 0;
    public static float currentFPS = 0f;

    private final CameraConfigurationManager configManager;

    public byte[][] frameBuffers;
    public int fbCounter = 0;

    private CameraManager parentManager;
    private int imageRotation = 0;
    private byte[] lastImage;

    private boolean newImageNeeded;
    private long lastImageRequest = 0;

    private int previewFormat;
    private int previewWidth;
    private int previewHeight;

    public static String TAG = "PreviewCallback";

    PreviewCallback(CameraConfigurationManager configManager, CameraManager parent) {
        this.configManager = configManager;
        this.parentManager = parent;
        this.imageRotation = 0;

        ///this.outputStream = new ByteArrayOutputStream();
        this.newImageNeeded = true;
    }

    public boolean setRotation(int rotation)
    {
        this.imageRotation = rotation;

        return true;
    }

    public boolean setPreviewDimensions(int width, int height)
    {
        this.previewWidth = width;
        this.previewHeight = height;

        return true;
    }

    public boolean setPreviewFormat(int format)
    {
        this.previewFormat = format;

        return true;
    }

    public byte[] getLastFrame()
    {
        newImageNeeded = true;

        lastImageRequest = System.currentTimeMillis();

        if (!this.parentManager.previewing)
        {
            Log.i("PreviewCallback:getLastFrame", "Starting preview automatically");
            this.parentManager.startPreview();
        }

        synchronized (this)
        {
            return this.lastImage;
        }
    }

    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (!newImageNeeded)
        {
            // Are we inactive since more than 5 seconds ??
            if (System.currentTimeMillis() - lastImageRequest > 5000)
            {
                if (this.parentManager.previewing)
                {
                    Log.i("PreviewCallback:getLastFrame", "Stopping preview due to inactivity");

                    this.parentManager.stopPreview();
                }
            }

            return;
        }

        newImageNeeded = false;

        try {
           synchronized (this) {
               lastImage = getFramePicture(data, camera);
           }
        }
        catch(Exception e) {
            Log.e("onPreviewFrame", "Exception: " + e.getMessage());
        }

    }

    private byte[] getFramePictureOrg(byte[] data, Camera camera) {
        //YUV formats require conversion
        if (previewFormat == ImageFormat.NV21 || previewFormat == ImageFormat.YUY2 || previewFormat == ImageFormat.NV16) {
            // Get the YuV image
            YuvImage yuvImage = new YuvImage(data, previewFormat, previewWidth, previewHeight, null);
            // Convert YuV to Jpeg
            Rect rect = new Rect(0, 0, previewWidth, previewHeight);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(rect, 10, outputStream);
            return outputStream.toByteArray();
        }

        return data;
    }

    private byte[] getFramePicture(byte[] data, Camera camera) {
        if (previewFormat == ImageFormat.NV21 || previewFormat == ImageFormat.YUY2 || previewFormat == ImageFormat.NV16) {
            YuvImage yuvImage = new YuvImage(data, previewFormat, previewWidth, previewHeight, null);
            // Convert YuV to Jpeg
            Rect rect = new Rect(0, 0, previewWidth, previewHeight);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(rect, 10, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            // Decode the JPEG byte array from 'output' to 'Bitmap' object
            Bitmap immutableBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            Bitmap bmp = immutableBitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Use 'Canvas' to draw text onto 'Bitmap'
            Canvas cv = new Canvas(bmp);

            // Prepare 'Paint' for text drawing
            Paint mPaint = new Paint();
            mPaint.setColor( Color.WHITE );
            mPaint.setStyle( Style.STROKE );
            mPaint.setTextSize(80);

            // Draw text on the 'Bitmap' image
            cv.drawText("TEXT To SHOW", 50, 50, mPaint);

            // Reset the stream of 'output' for output writing.
            //data.reset();

            // Compress current 'Bitmap' to 'output' as JPEG format
            ByteArrayOutputStream outputStreamFinal = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, outputStreamFinal);

            return outputStreamFinal.toByteArray();
        }

        return data;
    }

}
