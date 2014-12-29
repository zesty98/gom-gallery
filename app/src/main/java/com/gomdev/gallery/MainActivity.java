package com.gomdev.gallery;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
    static final String CLASS = "MainActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        GalleryContext galleryContext = GalleryContext.getInstance();

        int actionBarHeight = GalleryUtils.getActionBarHeight(this);
        galleryContext.setActionbarHeight(actionBarHeight);

        int spacing = getResources().getDimensionPixelSize(
                R.dimen.gridview_spacing);
        int columnWidth = getResources().getDimensionPixelSize(R.dimen.gridview_column_width);
        int numOfColumns = width / (columnWidth + spacing);
        galleryContext.setNumOfColumns(numOfColumns);

        columnWidth = (int) ((width - spacing * (numOfColumns + 1)) / numOfColumns);

        galleryContext.setScreenSize(width, height);
        galleryContext.setColumnWidth(columnWidth);

        ImageManager.newInstance(this);

        PackageInfo packageInfo;
        try {
            packageInfo = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 0);

            if (packageInfo != null) {
                galleryContext.setVersionCode(packageInfo.versionCode);
            }
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
