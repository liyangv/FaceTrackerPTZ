package com.example.luffy.directundist;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by Luffy on 11/4/2016.
 */
public class CamSurface extends SurfaceView implements SurfaceHolder.Callback{
    SurfaceHolder m_surHolder;
    public Camera m_camera;
    private Handler m_handler;

    public SurfaceTexture m_surTex;
    private static final int MAGIC_TEXTURE_ID = 10;

    CamSurface(Context context, Handler handler){
        super(context);
        m_surHolder = getHolder();
        m_surHolder.addCallback(this);
        m_surHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        m_handler = handler;
        m_surTex = new SurfaceTexture(MAGIC_TEXTURE_ID);

    }

    public void surfaceCreated(SurfaceHolder holder){
        Log.d("chao", "begin open Camera");
        if(m_camera != null){
            m_camera.stopPreview();
            m_camera.release();
            m_camera = null;
        }

        m_camera = Camera.open();

        Log.d("chao", "open Camera Success!");
        try {
            m_camera.setPreviewDisplay(holder);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
    public void surfaceDestroyed(SurfaceHolder holder ){
        m_camera.stopPreview();
        m_camera.release();
        m_camera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d("chao", "surfaceChanged");
        if (m_camera != null) {
            m_camera.stopPreview();
        }

        Camera.Parameters parameters = m_camera.getParameters();
        parameters.setPreviewSize(640, 480);


        m_camera.setParameters(parameters);
        m_camera.startPreview();

        String xxx = parameters.getFlashMode();

        Log.d("chao", "startPreview");

        m_camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {

                Camera.Parameters parameters = camera.getParameters();
                int width = parameters.getPreviewSize().width;
                int height = parameters.getPreviewSize().height;
                int imageFormat = parameters.getPreviewFormat();

                processFrame(data, width, height, imageFormat, true, false);
                Message msg = m_handler.obtainMessage();
                msg.what = 1100;
                msg.obj = data;
                m_handler.sendMessage(msg);

            }
        });
    }

    public native void processFrame(byte[] inputFrame, int width, int height ,int type, boolean bFocus, boolean bPortrait);


}