package com.example.luffy.directundist;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Luffy on 11/4/2016.
 */
public class CamRenderer implements GLSurfaceView.Renderer{
    private boolean mFirstDraw;
    private boolean mSurfaceCreated;
    private int mWidth;
    private int mHeight;
    private long mLastTime;
    private int mFPS;

    public CamRenderer(){
        super();
        mFirstDraw = true;
        mSurfaceCreated = false;
        mWidth = -1;
        mHeight = -1;
        mLastTime = System.currentTimeMillis();
        mFPS = 0;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("chao:", "gl suface is created");
        mSurfaceCreated = true;
        mWidth = -1;
        mHeight = -1;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (!mSurfaceCreated && width == mWidth
                && height == mHeight){
            Log.i("chao", "Surface changed but already handled.");
        }

        String msg = "Surface changed width:" + width
                + " height:" + height;
        if (mSurfaceCreated) {
            msg += " context lost.";
        } else {
            msg += ".";
        }
        Log.i("chao", msg);

        mWidth = width;
        mHeight = height;

        onCreate(mWidth, mHeight, mSurfaceCreated);
        mSurfaceCreated = false;

    }

    @Override
    public void onDrawFrame(GL10 notUsed) {
        onDrawFrame(mFirstDraw);


        mFPS++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastTime >= 1000) {
            mFPS = 0;
            mLastTime = currentTime;
        }
        if (mFirstDraw) {
            mFirstDraw = false;
        }
    }
    public int getFPS() {
        return mFPS;
    }

    public void onCreate(int width, int height,
                                  boolean contextLost){
        System.loadLibrary("yscl_undist_interface");
        initDraw(width, height);
    }

    public void onDrawFrame(boolean firstDraw) {
        drawFrame();
    }



    private native void drawFrame();
    private native void initDraw(int w, int h);
}
