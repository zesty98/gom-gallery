package com.gomdev.gallery;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.gomdev.gallery.GalleryConfig.VisibleMode;

public class BucketListActivity extends Activity {
    static final String CLASS = "BucketListActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    public BucketListActivity() {
        if (DEBUG) {
            Log.d(TAG, "BucketListActivity()");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreate()");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new BucketListFragment())
                    .commit();
        }

        GalleryUtils.setSystemUiVisibility(this, VisibleMode.VISIBLE_MODE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GalleryUtils.setActionBarElevation(this);
        }

        GalleryUtils.setDefaultInfo(this);
    }

    @Override
    protected void onStart() {
        if (DEBUG) {
            Log.d(TAG, "onStart()");
        }

        super.onStart();

        DataObserver.registerContentObserver(this);
    }

    @Override
    protected void onStop() {
        if (DEBUG) {
            Log.d(TAG, "onStop()");
        }

        DataObserver.unregisterContentObserver(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "onDestroy()");
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }

        super.onPause();
    }
}
