package com.gomdev.gallery;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

public class ImageListActivity extends Activity {
    static final String CLASS = "ImageListActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GallerySurfaceView mSurfaceView = null;
    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        GalleryContext galleryContext = GalleryContext.getInstance();
        if (galleryContext == null) {
            galleryContext = GalleryContext.newInstance(this);
            GalleryUtils.setDefaultInfo(this);
        }

        mImageManager = ImageManager.getInstance();

        int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_INDEX, 0);
        BucketInfo bucketInfo = mImageManager.getBucketInfo(bucketPosition);
        getActionBar().setTitle(bucketInfo.getName());

        SharedPreferences pref = getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        int numOfColumns = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS, 0);
        int columnWidth = pref.getInt(GalleryConfig.PREF_COLUMNS_WIDTH, 0);
        if (numOfColumns == 0 || columnWidth == 0) {
            numOfColumns = galleryContext.getNumOfColumns();
            columnWidth = galleryContext.getColumnWidth();

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(GalleryConfig.PREF_COLUMNS_WIDTH, columnWidth);
            editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS, numOfColumns);
            editor.putInt(GalleryConfig.PREF_MIN_NUM_OF_COLUMNS, numOfColumns);
            editor.putInt(GalleryConfig.PREF_MAX_NUM_OF_COLUMNS, numOfColumns * 2); // FIX_ME
            editor.commit();
        }

        setContentView(R.layout.activity_gles_main);

        mGridInfo = new GridInfo(this, bucketInfo);

        mSurfaceView = (GallerySurfaceView) findViewById(R.id.surfaceview);
        mSurfaceView.setGridInfo(mGridInfo);

        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(GalleryConfig.PREF_BUCKET_INDEX, bucketInfo.getIndex());
        editor.putInt(GalleryConfig.PREF_DATE_LABEL_INDEX, 0);
        editor.putInt(GalleryConfig.PREF_IMAGE_INDEX, 0);
        editor.commit();
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
