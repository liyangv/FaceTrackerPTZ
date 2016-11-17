package com.example.luffy.directundist;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;

/**
 * Created by Luffy on 11/4/2016.
 */
public class CombActivity extends Activity {
    CamSurface m_camSurface;
    GLSurfaceView m_glSurface;
    CamRenderer m_camglRenderer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_camglRenderer = new CamRenderer();

        if(hasGLEX20()) {
            Log.d("chao:", "has opengl es 2.0");
            m_glSurface = new GLSurfaceView(this);
            m_glSurface.setEGLContextClientVersion(2);
            m_glSurface.setPreserveEGLContextOnPause(true);
            m_glSurface.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            m_glSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT );
            m_glSurface.setRenderer(m_camglRenderer);
            setContentView(m_glSurface);// method 3
        }else{
            Log.d("chao:", "has  not  opengl es 2.0");
        }

        m_camSurface = new CamSurface(this, m_handler);
        addContentView(m_camSurface, new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) );// method 3

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }
    private static Handler m_handler = new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case 1100:
                    byte[] data_p = (byte[])msg.obj;
                    if (data_p!=null&&data_p.length>0){
                        //    Log.d("chao:", "process one frame!");
                    }
                    break;
                default:
                    break;
            }
        }
    };
    protected void onResume() {
        super.onResume();
        m_glSurface.onResume();
    }

    protected void onPause() {
        super.onPause();
        m_glSurface.onPause();
    }

    private boolean hasGLEX20(){
        ActivityManager am = (ActivityManager)
                getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return info.reqGlEsVersion >= 0x20000;
    }


}
