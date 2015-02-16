package com.gomdev.gallery;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class DetailViewActivity extends Activity {
    static final String CLASS = "DetailViewActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GallerySurfaceView mSurfaceView = null;
    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;
    private BucketInfo mBucketInfo = null;
    private DateLabelInfo mDateLabelInfo = null;

    private ImageIndexingInfo mCurrentImageIndexingInfo = null;

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

        int bucketIndex = getIntent().getIntExtra(GalleryConfig.BUCKET_INDEX, 0);
        int dateLabelIndex = getIntent().getIntExtra(GalleryConfig.DATE_LABEL_INDEX, 0);
        int imageIndex = getIntent().getIntExtra(GalleryConfig.IMAGE_INDEX, 0);

        mCurrentImageIndexingInfo = new ImageIndexingInfo(bucketIndex, dateLabelIndex, imageIndex);

        mImageManager = ImageManager.getInstance();
        mBucketInfo = mImageManager.getBucketInfo(bucketIndex);

        mDateLabelInfo = mBucketInfo.get(dateLabelIndex);

        setContentView(R.layout.activity_gles_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        FrameLayout layout = (FrameLayout) findViewById(R.id.container);
        mSurfaceView = new GallerySurfaceView(this, mGridInfo);
        layout.addView(mSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ActionBar actionBar = getActionBar();

            // Hide title text and set home as up
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(mDateLabelInfo.getDate());

            // Hide and show the ActionBar as the visibility changes
            layout.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int vis) {
                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                actionBar.hide();
                            } else {
                                actionBar.show();
                            }
                        }
                    });

            layout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            actionBar.hide();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            boolean isBucketDeleted = ImageManager.getInstance().deleteImage(mCurrentImageIndexingInfo);
            if (isBucketDeleted == false) {
                finish();
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
