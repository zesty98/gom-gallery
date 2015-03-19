package com.gomdev.gallery;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.gomdev.gles.GLESUtils;

public class MainActivity extends Activity {
    static final String CLASS = "MainActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        GalleryContext galleryContext = GalleryContext.getInstance();
        if (galleryContext == null) {
            GalleryContext.newInstance(this);
        }

        GalleryUtils.setDefaultInfo(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        GalleryUtils.showSystemUiVisibility(this);

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


}
