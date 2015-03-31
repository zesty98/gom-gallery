package com.gomdev.gallery;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {
    static final String CLASS = "MainActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

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

        ImageView imageView = new ImageView(this);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.main);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        FrameLayout layout = (FrameLayout) findViewById(R.id.container);
        layout.addView(imageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        GalleryUtils.setSystemUiVisibility(this, GalleryConfig.VisibleMode.VISIBLE_MODE);

        init();
    }

    private void init() {
        GalleryContext.newInstance(this);

        mDBSyncTask.execute();
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

            imageLoader.loadBucketInfos();
            imageLoader.loadImageInfos();

            Log.d(TAG, "init() loading duration=" + ((System.nanoTime() - mStartTick) / 1000000));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (DEBUG) {
                Log.d(TAG, "onPostExecute()");
            }

            long currentTick = System.nanoTime();
            long durationInMS = (currentTick - mStartTick) / 1000000;

            if (durationInMS < 1000L) {
                try {
                    Thread.currentThread().sleep(1000L - durationInMS);
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
