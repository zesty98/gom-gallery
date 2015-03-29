package com.gomdev.gallery;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import com.gomdev.gallery.GalleryConfig.VisibleMode;

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
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new BucketListFragment())
                    .commit();
        }

        init();
    }

    private void init() {
        GalleryContext.newInstance(this);

        GalleryUtils.setDefaultInfo(this);

        GalleryUtils.setSystemUiVisibility(this, VisibleMode.VISIBLE_MODE);
        GalleryUtils.setActionBarElevation(this);

        PackageInfo packageInfo;
        try {
            packageInfo = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0);

            if (packageInfo != null) {
                GalleryContext.getInstance().setVersionCode(packageInfo.versionCode);
            }
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
