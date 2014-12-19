package com.gomdev.gallery;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class ImageListActivity extends Activity {
    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (GalleryConfig.sUseGLES == true) {
            setContentView(R.layout.activity_gles_main);

            mSurfaceView = (GallerySurfaceView) findViewById(R.id.surfaceview);
            mRenderer = new ImageListRenderer(this);
            mRenderer.setSurfaceView(mSurfaceView);

            mSurfaceView.setEGLContextClientVersion(2);
            mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            mSurfaceView.setRenderer(mRenderer);
            mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//            mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_POSITION, 0);
            BucketInfo bucketInfo = ImageManager.getInstance().getBucketInfo(bucketPosition);
            mRenderer.setBucketInfo(bucketInfo);

            GalleryContext galleryContext = GalleryContext.getInstance();
            int actionBarHeight = galleryContext.getActionBarHeight();
            int numOfColumns = galleryContext.getNumOfColumns();
            int columnWidth = galleryContext.getColumnWidth();
            int spacing = this.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

            int totalNumOfRows = (int) Math.ceil((double) bucketInfo.getNumOfImageInfos() / numOfColumns);
            int scrollableHeight = actionBarHeight + (columnWidth + spacing) * totalNumOfRows;
            mSurfaceView.setScrollableHeight(scrollableHeight);

        } else {
            setContentView(R.layout.activity_main);
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new ImageListFragment())
                        .commit();
            }
        }

        init();
    }

    private void init() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        int widthInDP = width * 160 / getResources().getDisplayMetrics().densityDpi;

        int numOfColumns = 3;

        if (widthInDP < 500f) {
            numOfColumns = 3;
        } else if (widthInDP < 600f) {
            numOfColumns = 4;
        } else if (widthInDP < 820f) {
            numOfColumns = 5;
        } else {
            numOfColumns = 6;
        }
        GalleryContext.getInstance().setNumOfColumns(numOfColumns);

        int spacing = getResources().getDimensionPixelSize(
                R.dimen.gridview_spacing);
        int columnWidth = (int) ((width - spacing * (numOfColumns + 1)) / numOfColumns);


        GalleryContext context = GalleryContext.getInstance();
        context.setScreenSize(width, height);
        context.setColumnWidth(columnWidth);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (GalleryConfig.sUseGLES == true && mSurfaceView != null) {
            mSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (GalleryConfig.sUseGLES == true && mSurfaceView != null) {
            mSurfaceView.onPause();
        }

        super.onPause();
    }
}
