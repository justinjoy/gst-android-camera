package org.freedesktop.gstreamer.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import org.freedesktop.gstreamer.GStreamer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public class GstAhc implements Closeable, SurfaceHolder.Callback {

    private final static String TAG = GstAhc.class.getName();

    private native void nativeInit();

    private native void nativeFinalize();

    private native void nativePlay();

    private native void nativePause();

    private static native boolean nativeClassInit();

    private native void nativeSurfaceInit(Object surface);

    private native void nativeSurfaceFinalize();

    private native void nativeChangeResolution(int width, int height);

    private native void nativeSetRotateMethod(int orientation);

    private native void nativeSetAutoFocus(boolean enabled);

    public enum Rotate {
        NONE,
        CLOCKWISE,
        ROTATE_180,
        COUNTERCLOCKWISE,
        HORIZONTAL_FLIP,
        VERTICAL_FLIP,
        UPPER_LEFT_DIAGONAL,
        UPPER_RIGHT_DIAGONAL,
        AUTOMATIC
    }

    private static final Rotate[] rotateMap = {
            Rotate.NONE,
            Rotate.CLOCKWISE,
            Rotate.ROTATE_180,
            Rotate.COUNTERCLOCKWISE,
            Rotate.HORIZONTAL_FLIP,
            Rotate.VERTICAL_FLIP,
            Rotate.UPPER_LEFT_DIAGONAL,
            Rotate.UPPER_RIGHT_DIAGONAL,
            Rotate.AUTOMATIC
    };

    private static final String[] whiteBalanceMap = {
            Camera.Parameters.WHITE_BALANCE_AUTO,
            Camera.Parameters.WHITE_BALANCE_DAYLIGHT,
            Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT,
            Camera.Parameters.WHITE_BALANCE_TWILIGHT,
            Camera.Parameters.WHITE_BALANCE_INCANDESCENT,
            Camera.Parameters.WHITE_BALANCE_FLUORESCENT,
            "manual",
            Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT,
            Camera.Parameters.WHITE_BALANCE_SHADE
    };

    private native void nativeSetWhiteBalance(int wb);

    private long native_custom_data;

    private String whiteBalanceMode;
    private Context context;

    private GstAhc(Context context) {
        nativeInit();
        this.context = context;
    }

    public static GstAhc init(Context context) throws Exception {

        System.loadLibrary("gstreamer_android");
        System.loadLibrary("android_camera");

        GStreamer.init(context);

        if (!nativeClassInit()) {
            throw new Exception("Failed to load application jni library.");
        }

        return new GstAhc(context);
    }

    private static final State[] stateMap = {
            State.VOID,
            State.NULL,
            State.READY,
            State.PAUSED,
            State.PLAYING
    };

    public enum State {
        VOID,
        NULL,
        READY,
        PAUSED,
        PLAYING
    }

    private State state = State.VOID;

    public static interface StateChangedListener {
        abstract void stateChanged(GstAhc gstAhc, State state);
    }

    private StateChangedListener stateChangedListener;

    public void setStateChangedListener(StateChangedListener listener) {
        stateChangedListener = listener;
    }

    private void onStateChanged(int stateIdx) {
        if (stateChangedListener != null) {
            state = stateMap[stateIdx];
            stateChangedListener.stateChanged(this, state);
        }
    }

    public void togglePlay() {
        if (state == State.PLAYING) {
            nativePause();
        } else {
            nativePlay();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Surface created: " + surfaceHolder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.d(TAG, "Surface changed to format " + format + " width "
                + width + " height " + height);
        nativeSurfaceInit(surfaceHolder.getSurface());

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Surface destroyed");
        nativeSurfaceFinalize();
    }

    public void setAutoFocus(boolean enabled) {
        Log.d(TAG, "AutoFocus: " + enabled);
        nativeSetAutoFocus (enabled);
    }

    public void setWhiteBalanceMode(String mode) {
        Log.d(TAG, "WhiteBlanceMode: " + mode);

        int idx = Arrays.asList(whiteBalanceMap).indexOf(mode);

        if (idx == -1 || idx >= whiteBalanceMap.length) {
            Log.d(TAG, "Invalid white balance mode. Try to use 'auto'");
            idx = 0;
        }

        nativeSetWhiteBalance(idx);
    }

    public void setRotateMethod(Rotate rotate) {
        nativeSetRotateMethod(Arrays.asList(rotateMap).indexOf(rotate));
    }

    public void changeResolutionTo(int width, int height) {
        Log.d(TAG, "Trying to set resolution to (w: " + width + " h: " + height + ")");
        nativePause();

        nativeChangeResolution(width, height);

        nativePlay();
    }

    @Override
    public void close() throws IOException {
        nativeFinalize();
    }

    /* Called from native code */
    private void onGStreamerInitialized() {
        Log.d(TAG, "Playing Camera!");
        nativePlay();
    }

    public static interface ErrorListener {
        abstract void error(GstAhc gstAhc, String errorMessage);
    }

    private ErrorListener errorListener;

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    private void onError(String errorMessage) {
        if (errorListener != null) {
            errorListener.error(this, errorMessage);
        }
    }
}
