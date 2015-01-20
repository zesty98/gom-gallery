package com.gomdev.gallery;

import android.app.Activity;
import android.content.SharedPreferences;
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

        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        ImageManager imageManager = ImageManager.getInstance();
        if (imageManager == null) {
            imageManager.newInstance(this);

            GalleryUtils.setDefaultInfo(this);
        }

        int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_POSITION, 0);
        BucketInfo bucketInfo = imageManager.getBucketInfo(bucketPosition);
        getActionBar().setTitle(bucketInfo.getName());

        GalleryContext galleryContext = GalleryContext.getInstance();

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
            editor.putInt(GalleryConfig.PREF_MAX_NUM_OF_COLUMNS, numOfColumns * 3);
            editor.commit();
        }

        if (GalleryConfig.sUseGLES == true) {
            setContentView(R.layout.activity_gles_main);

            mGridInfo = new GridInfo(this, bucketInfo);

            mSurfaceView = (GallerySurfaceView) findViewById(R.id.surfaceview);
            mSurfaceView.setGridInfo(mGridInfo);

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(GalleryConfig.PREF_IMAGE_INDEX, 0);
            editor.putInt(GalleryConfig.PREF_BUCKET_INDEX, bucketInfo.getPosition());
            editor.commit();

        } else {
            setContentView(R.layout.activity_main);
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new ImageListFragment())
                        .commit();
            }
        }
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
