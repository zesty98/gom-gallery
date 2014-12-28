package com.gomdev.gallery;

import android.app.Activity;
import android.os.Bundle;

public class ImageListActivity extends Activity {
    static final String CLASS = "ImageListActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GallerySurfaceView mSurfaceView = null;

    private GridInfo mGridInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        if (GalleryConfig.sUseGLES == true) {
            setContentView(R.layout.activity_gles_main);

            int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_POSITION, 0);
            BucketInfo bucketInfo = ImageManager.getInstance().getBucketInfo(bucketPosition);

            mGridInfo = new GridInfo(this, bucketInfo);

            mSurfaceView = (GallerySurfaceView) findViewById(R.id.surfaceview);
            mSurfaceView.setGridInfo(mGridInfo);


        } else {
            setContentView(R.layout.activity_main);
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new ImageListFragment())
                        .commit();
            }
        }
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSurfaceView != null) {
            mSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mSurfaceView != null) {
            mSurfaceView.onPause();
        }

        super.onPause();
    }
}
