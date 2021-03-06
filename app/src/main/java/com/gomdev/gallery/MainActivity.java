package com.gomdev.gallery;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private static final String CLASS = "MainActivity";
    private static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryConfig.DEBUG;

    private GLSurfaceView mSurfaceView = null;

    public MainActivity() {
        if (DEBUG) {
            Log.d(TAG, "MainActivity()");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreate()");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GalleryUtils.setSystemUiVisibility(this, GalleryConfig.VisibleMode.VISIBLE_MODE);

        GalleryContext.newInstance(this);

        mDBSyncTask.execute();

        FrameLayout layout = (FrameLayout) findViewById(R.id.container);
        mSurfaceView = new GLSurfaceView(this);
        layout.addView(mSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        IntroRenderer renderer = new IntroRenderer(this);

        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mSurfaceView.setRenderer(renderer);
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mSurfaceView.setPreserveEGLContextOnPause(true);

        renderer.setSurfaceView(mSurfaceView);
    }

    @Override
    protected void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        super.onResume();

        if (mSurfaceView != null) {
            mSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }

        if (mSurfaceView != null) {
            mSurfaceView.onPause();
        }

        super.onPause();
    }

    private AsyncTask<Void, Void, Void> mDBSyncTask = new AsyncTask<Void, Void, Void>() {
        private long mStartTick = 0L;

        @Override
        protected Void doInBackground(Void... params) {
            if (DEBUG) {
                Log.d(TAG, "doInBackground()");
            }

            ImageLoader imageLoader = ImageLoader.getInstance();

            mStartTick = System.nanoTime();

            imageLoader.checkAndLoadImages();

            Log.d(TAG, "init() loading duration=" + ((System.nanoTime() - mStartTick) / 1000000));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (DEBUG) {
                Log.d(TAG, "onPostExecute()");
            }

            long currentTick = System.nanoTime();
            long durationInMS = (currentTick - mStartTick) / 1000000L;

            if (durationInMS < GalleryConfig.MAINACTIVITY_DURATION) {
                try {
                    Thread.currentThread().sleep(GalleryConfig.MAINACTIVITY_DURATION - durationInMS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Intent intent = new Intent(MainActivity.this, com.gomdev.gallery.BucketListActivity.class);
            MainActivity.this.startActivity(intent);
            finish();
        }
    };
}
