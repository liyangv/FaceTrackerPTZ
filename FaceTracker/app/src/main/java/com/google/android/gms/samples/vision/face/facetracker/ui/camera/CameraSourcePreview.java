/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker.ui.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Message;
import com.google.android.gms.common.images.Size;
//import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSource;
import com.google.android.gms.vision.face.Face;

import java.io.IOException;
import java.util.Vector;
import java.util.Calendar;
import java.lang.Math;
import java.util.concurrent.locks.Lock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";

    private Context mContext;
    private SurfaceView mSurfaceView;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;

    private GraphicOverlay mOverlay;

    private int mROIX;
    private int mROIY;
    private int mROIWidth;
    private int mROIHeight;
    public static final int REFRESH = 1;

    private double mFrameROIXSmooth = -1;
    private double mFrameROIYSmooth = -1;
    private double mFrameROIWidthSmooth = -1;
    private double mFrameROIHeightSmooth = -1;

    private double mFrameROIX = -1;
    private double mFrameROIY = -1;
    private double mFrameROIWidth = -1;
    private double mFrameROIHeight = -1;

    private double mLastFrameROIX = -1;
    private double mLastFrameROIY = -1;
    private double mLastFrameROIWidth = -1;
    private double mLastFrameROIHeight = -1;

    private int nWindowWidth;// = 1080;//1920;
    private int nWindowHeight;// = 1701;//1007;
    private int nFrameWidth;// = 960;  //1920
    private int nFrameHeight;// = 1280;//1080




    public long mStartMovingAnimationTime;
    public boolean mDuringAnimation = false;


    public int mAnimationStep = 0;
    public int mCurrentlayoutX = -1;
    public int mCurrentlayoutY = -1;
    public int mCurrentlayoutWidth = -1;
    public int mCurrentlayoutHeight = -1;
    public int nTargetX = -1;
    public int nTargetY = -1;
    public int nTargetWidth = -1;
    public int nTargetHeight = -1;

    Vector mVector;// = new Vector();
    public boolean mbFrontalCamera = true;
    public boolean mbMoving = false;
    class FaceInfo{
        public int mID;
        public int mFaceX;
        public int mFaceY;
        public int mFaceWidth;
        public int mFaceHeight;

        public long mLastDetectedTimeSecond;
        public void update(Face face){
            mFaceX = (int)(face.getPosition().x);
            mFaceY = (int)(face.getPosition().y);
            mFaceWidth = (int)(face.getWidth());
            mFaceHeight = (int)(face.getHeight());
            mID = face.getId();
            mLastDetectedTimeSecond = Calendar.getInstance().getTimeInMillis();
        }
        public int checkValid(){
            long nowInMilliSecond = Calendar.getInstance().getTimeInMillis();
            if(nowInMilliSecond - mLastDetectedTimeSecond > 2000){
                return 0;
            }else{
                return 1;
            }
        }
    }

    Handler handlerTimer=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            mROIX = 0;
            mROIY = 0;
            mROIWidth = 0;
            mROIHeight = 0;
           // invalidate();
            requestLayout();
            handlerTimer.postDelayed(this, 2000);
        }
    };


    public Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {

            Long currentTime = Calendar.getInstance().getTimeInMillis();
            if(currentTime - mStartMovingAnimationTime > 1000){
                switch (msg.what) {
                    case 1:
                        //invalidate();
                        mStartMovingAnimationTime = Calendar.getInstance().getTimeInMillis();
                        requestLayout();
                        handlerTimer.postDelayed(runnable, 2000);
                        break;
                }
            }else{
                Log.d(TAG, "waiting for animation finished");
            }


            super.handleMessage(msg);
        }
    };



    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);

        mROIX = 0;
        mROIY = 0;
        mROIWidth = 0;
        mROIHeight = 0;

        mVector = new Vector();
    }

    public void start(CameraSource cameraSource) throws IOException {
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;

        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {
        mOverlay = overlay;
        start(cameraSource);
    }

    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    private void startIfReady() throws IOException {
       // Log.d(TAG, "drawing");
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mSurfaceView.getHolder());
            if (mOverlay != null) {
                Size size = mCameraSource.getPreviewSize();
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());
                if (isPortraitMode()) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees
                    mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
                } else {
                    mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
                }
                mOverlay.clear();
            }
            mStartRequested = false;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (IOException e) {
                Log.e(TAG, "Could not start camera source.", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


        }
    }

    public void updateFace(Face face) {

        handlerTimer.removeCallbacks(runnable);
     //   ReadWriteLock rwl = new ReentrantReadWriteLock();
    //    rwl.writeLock().lock();
        int size = mVector.size();
        int bExit = -1;
        for (int i=0; i<size; i++) {
            FaceInfo value = (FaceInfo)mVector.get(i);
            if(face.getId() == value.mID){
                bExit = 1;
                value.update(face);
            }
        }
        if(bExit == -1){
            FaceInfo newValue = new FaceInfo();
            newValue.update(face);
            mVector.addElement(newValue);
        }

        size = mVector.size();
        for (int i = 0; i < size; i++) {
            FaceInfo value = (FaceInfo)mVector.get(i);
            if(value.checkValid() == 0){
                mVector.removeElement(value);
                break;
            }
        }

        int nLeftMost = -1;
        int nRightMost = -1;
        int nTopMost = -1;
        int nBottomMost = -1;
        size = mVector.size();
        for (int i = 0; i < size; i++) {
            FaceInfo value = (FaceInfo)mVector.get(i);
            if(i == 0){
                nLeftMost = value.mFaceX;
                nRightMost = value.mFaceX + value.mFaceWidth;
                nTopMost = value.mFaceY;
                nBottomMost = value.mFaceY + value.mFaceHeight;
            }else {
                nLeftMost = Math.min(nLeftMost, value.mFaceX);
                nRightMost = Math.max(nRightMost, value.mFaceX + value.mFaceWidth);
                nTopMost = Math.min(nTopMost, value.mFaceY);
                nBottomMost = Math.max(nBottomMost, value.mFaceY + value.mFaceHeight);
            }
        }

        boolean bMirror = true;
        if(mbFrontalCamera == true){
            bMirror = true;
        }else{
            bMirror = false;
        }
        if (bMirror){
            int nTmp = nFrameWidth - nLeftMost;
            nLeftMost =  nFrameWidth - nRightMost;
            nRightMost = nTmp;
        }

        if(Math.abs(mFrameROIXSmooth - mFrameROIX) < 5.0 && Math.abs(mFrameROIYSmooth - mFrameROIY) < 5.0 && Math.abs(mFrameROIWidthSmooth - mFrameROIWidth) < 5.0 && Math.abs(mFrameROIHeightSmooth - mFrameROIHeight) < 5.0){
            mbMoving = false;
        }

        if(mbMoving == false){
            if( mFrameROIWidthSmooth == -1 || mFrameROIHeightSmooth == -1){
                mFrameROIX = nLeftMost;
                mFrameROIY = nTopMost;
                mFrameROIWidth = nRightMost - nLeftMost;
                mFrameROIHeight = nBottomMost - nTopMost;

                mFrameROIXSmooth = nLeftMost;
                mFrameROIYSmooth = nTopMost;
                mFrameROIWidthSmooth = nRightMost - nLeftMost;
                mFrameROIHeightSmooth = nBottomMost - nTopMost;
            }else{
                int nNewX = nLeftMost;
                int nNewY = nTopMost;
                int nNewWidth = nRightMost - nLeftMost;
                int nNewHeight = nBottomMost - nTopMost;

                if(Math.abs(nNewX - mFrameROIX) > 100 || Math.abs(nNewY - mFrameROIY) > 100 || Math.abs(nNewWidth - mFrameROIWidth) > 100 || Math.abs(nNewHeight - mFrameROIHeight) > 100){
                    mFrameROIX = nNewX;
                    mFrameROIY = nNewY;
                    mFrameROIWidth = nNewWidth;
                    mFrameROIHeight = nNewHeight;

                    mLastFrameROIX = mFrameROIXSmooth;
                    mLastFrameROIY = mFrameROIYSmooth;
                    mLastFrameROIWidth = mFrameROIWidthSmooth;
                    mLastFrameROIHeight = mFrameROIHeightSmooth;
                    mbMoving = true;
                }
            }


        }else{

            mFrameROIXSmooth =  mFrameROIX;//mFrameROIXSmooth + (double)(mFrameROIX - mLastFrameROIX)/1.0;
            mFrameROIYSmooth = mFrameROIY;//mFrameROIYSmooth + (double)(mFrameROIY - mLastFrameROIY)/1.0;
            mFrameROIWidthSmooth = mFrameROIWidth;//mFrameROIWidthSmooth + (double)(mFrameROIWidth - mLastFrameROIWidth)/1.0;
            mFrameROIHeightSmooth = mFrameROIHeight;//mFrameROIHeightSmooth + (double)(mFrameROIHeight - mLastFrameROIHeight)/1.0;
        }
        int nFaceWidth = (int)(mFrameROIWidthSmooth) + 200;
        int nFaceHeight = (int)(mFrameROIHeightSmooth) + 200;
        if(nFaceWidth*nWindowHeight >= nFaceHeight*nWindowWidth){
            nFaceHeight = nFaceWidth*nWindowHeight/nWindowWidth;
        }else{
            nFaceWidth = nFaceHeight*nWindowWidth/nWindowHeight;
        }

        double dScale = nFaceWidth/(float)(nWindowWidth);
        int nFaceX = (int)((mFrameROIXSmooth + mFrameROIWidthSmooth/2)/dScale);
        int nFaceY = (int)((mFrameROIYSmooth + mFrameROIHeightSmooth/2)/dScale);

        mROIWidth = (int)(nFrameWidth/dScale);
        mROIHeight = (int)(nFrameHeight/dScale);
        mROIX = (int)(nWindowWidth/2.0) - nFaceX;
        mROIY = (int)(nWindowHeight/2.0) - nFaceY;

        mROIX = Math.min(mROIX,0);
        mROIY = Math.min(mROIY,0);
        mROIX = Math.max(mROIX, nWindowWidth - mROIWidth);
        mROIY = Math.max(mROIY,nWindowHeight - mROIHeight);
        //     rwl.writeLock().unlock();

    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;
        if (mCameraSource != null) {
            Size size = mCameraSource.getPreviewSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        nWindowWidth = layoutWidth;// = 1080;//1920;
        nWindowHeight = layoutHeight;// = 1701;//1007;
        nFrameWidth = width;// = 960;  //1920
        nFrameHeight = height;
        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Transition mTransition = new AutoTransition();
            mTransition.setDuration(2000);
            TransitionManager.beginDelayedTransition(this,mTransition);
        }
        Log.d(TAG, "layout");
        if(mROIWidth == 0 && mROIHeight == 0){
            for (int i = 0; i < getChildCount(); ++i) {
                getChildAt(i).layout(0, 0, 0 + childWidth, 0 + childHeight);
            }

            mCurrentlayoutX = 0;
            mCurrentlayoutY = 0;
            mCurrentlayoutWidth = childWidth;
            mCurrentlayoutHeight = childHeight;

        }else{
            if(mAnimationStep == 0){
               // mCurrentlayoutX = mROIX;
               // mCurrentlayoutY = mROIY;

                mCurrentlayoutX = nWindowWidth/2 - (nWindowWidth/2 - mROIX)*mCurrentlayoutWidth/mROIWidth;

                mCurrentlayoutY = nWindowHeight/2 - (nWindowHeight/2 - mROIY)*mCurrentlayoutHeight/mROIHeight;
                //mCurrentlayoutX = mROIX + mROIWidth/2 - mCurrentlayoutWidth/2;
                //mCurrentlayoutY = mROIY + mROIHeight/2 - mCurrentlayoutHeight/2;
                mCurrentlayoutWidth = mCurrentlayoutWidth;
                mCurrentlayoutHeight = mCurrentlayoutHeight;
                nTargetX = mROIX;
                nTargetY = mROIY;
                nTargetWidth = mROIWidth;
                nTargetHeight = mROIHeight;
                mAnimationStep = 1;
            }else{
                mCurrentlayoutX = nTargetX;
                mCurrentlayoutY = nTargetY;
                mCurrentlayoutWidth = nTargetWidth;
                mCurrentlayoutHeight = nTargetHeight;
                mAnimationStep = 0;
            }
            for (int i = 0; i < getChildCount(); ++i) {
                getChildAt(i).layout(mCurrentlayoutX, mCurrentlayoutY, mCurrentlayoutX + mCurrentlayoutWidth, mCurrentlayoutY + mCurrentlayoutHeight);
            }

          /*  for (int i = 0; i < getChildCount(); ++i) {
                getChildAt(i).layout(mROIX, mROIY, mROIX + mROIWidth, mROIY + mROIHeight);
            }
*/


        }

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }
}
