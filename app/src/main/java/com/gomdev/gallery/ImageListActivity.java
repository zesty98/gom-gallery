package com.gomdev.gallery;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.gomdev.gallery.GalleryConfig.VisibleMode;

public class ImageListActivity extends Activity {
    static final String CLASS = "ImageListActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    final static int SET_SYSTEM_UI_FLAG_LOW_PROFILE = 102;
    final static int SET_SYSTEM_UI_FLAG_VISIBLE = 103;
    final static int UPDATE_ACTION_BAR_TITLE = 104;
    final static int INVALIDATE_OPTION_MENU = 105;

    private GallerySurfaceView mSurfaceView = null;
    private GalleryContext mGalleryContext = null;
    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreate()");
        }
        super.onCreate(savedInstanceState);

        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        mGalleryContext = GalleryContext.getInstance();
        if (mGalleryContext == null) {
            mGalleryContext = GalleryContext.newInstance(this);
            GalleryUtils.setDefaultInfo(this);
        }

        mImageManager = ImageManager.getInstance();

        GalleryUtils.setSystemUiVisibility(this, VisibleMode.VISIBLE_TRANSPARENT_MODE);

        int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_INDEX, 0);
        BucketInfo bucketInfo = mImageManager.getBucketInfo(bucketPosition);
        getActionBar().setTitle(bucketInfo.getName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GalleryUtils.setActionBarElevation(this);
        }

        SharedPreferences pref = getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        int numOfColumns = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS, 0);
        int columnWidth = pref.getInt(GalleryConfig.PREF_COLUMNS_WIDTH, 0);
        if (numOfColumns == 0 || columnWidth == 0) {
            numOfColumns = mGalleryContext.getNumOfColumns();
            columnWidth = mGalleryContext.getColumnWidth();

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(GalleryConfig.PREF_COLUMNS_WIDTH, columnWidth);
            editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS, numOfColumns);
            editor.putInt(GalleryConfig.PREF_MIN_NUM_OF_COLUMNS, numOfColumns);
            editor.putInt(GalleryConfig.PREF_MAX_NUM_OF_COLUMNS, numOfColumns * 3);
            editor.commit();
        }

        setContentView(R.layout.activity_gles_main);

        mGridInfo = new GridInfo(this, bucketInfo);

        FrameLayout layout = (FrameLayout) findViewById(R.id.container);

        mSurfaceView = new GallerySurfaceView(this, mGridInfo);
        layout.addView(mSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mSurfaceView.setHandler(mHandler);

        mSurfaceView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int vis) {
                        if (DEBUG) {
                            Log.d(TAG, "onSystemUiVisibilityChange() " + vis);
                        }

                        if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                            getActionBar().hide();
                        } else {
                            updateActionBarTitle();
                            invalidateOptionsMenu();
                            getActionBar().show();
                        }
                    }
                });

        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(GalleryConfig.PREF_BUCKET_INDEX, bucketInfo.getIndex());
        editor.putInt(GalleryConfig.PREF_DATE_LABEL_INDEX, 0);
        editor.putInt(GalleryConfig.PREF_IMAGE_INDEX, 0);
        editor.commit();
    }


    private void updateActionBarTitle() {
        ImageIndexingInfo indexingInfo = mGalleryContext.getImageIndexingInfo();
        BucketInfo bucketInfo = mImageManager.getBucketInfo(indexingInfo.mBucketIndex);
        DateLabelInfo dateLabelInfo = bucketInfo.get(indexingInfo.mDateLabelIndex);

        GalleryConfig.ImageViewMode mode = mGalleryContext.getImageViewMode();
        switch (mode) {
            case ALBUME_VIEW_MODE:
                getActionBar().setTitle(bucketInfo.getName());
                break;
            case DETAIL_VIEW_MODE:
                getActionBar().setTitle(dateLabelInfo.getDate());
                break;
            default:
                getActionBar().setTitle(bucketInfo.getName());
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
    protected void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        super.onResume();

        if (mSurfaceView != null) {
            mSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }

        if (mSurfaceView != null) {
            mSurfaceView.onPause();
        }

        super.onPause();
    }

    @Override
    public void finish() {
        if (DEBUG) {
            Log.d(TAG, "finish()");
        }

        if (mSurfaceView != null) {
            mSurfaceView.finish();
        } else {
            super.finish();
        }
    }

    void onFinished() {
        if (DEBUG) {
            Log.d(TAG, "onFinished()");
        }

        super.finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        GalleryConfig.ImageViewMode mode = mGalleryContext.getImageViewMode();

        switch (mode) {
            case ALBUME_VIEW_MODE:
                menu.removeItem(R.id.action_delete);
                break;
            case DETAIL_VIEW_MODE:
                if (menu.findItem(R.id.action_delete) == null) {
                    getMenuInflater().inflate(R.menu.main, menu);
                }
                break;
            default:
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            delete();
            return true;
        } else if (id == R.id.action_share) {
            share();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void delete() {
        GalleryContext galleryContext = GalleryContext.getInstance();
        ImageIndexingInfo imageIndexingInfo = galleryContext.getImageIndexingInfo();

        boolean isBucketDeleted = ImageManager.getInstance().deleteImage(imageIndexingInfo);

        if (isBucketDeleted == true) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            galleryContext.setImageViewMode(GalleryConfig.ImageViewMode.ALBUME_VIEW_MODE);
            Intent intent = new Intent(this, ImageListActivity.class);
            intent.putExtra(GalleryConfig.BUCKET_INDEX, imageIndexingInfo.mBucketIndex);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void share() {
        GalleryContext galleryContext = GalleryContext.getInstance();
        ImageIndexingInfo imageIndexingInfo = galleryContext.getImageIndexingInfo();

        ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(imageIndexingInfo);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageInfo.getImageID());
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share)));
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SET_SYSTEM_UI_FLAG_LOW_PROFILE:
                    GalleryUtils.setSystemUiVisibility(ImageListActivity.this, VisibleMode.FULLSCREEN_MODE);
                    break;
                case SET_SYSTEM_UI_FLAG_VISIBLE:
                    invalidateOptionsMenu();
                    GalleryUtils.setSystemUiVisibility(ImageListActivity.this, VisibleMode.VISIBLE_TRANSPARENT_MODE);
                    break;
                case UPDATE_ACTION_BAR_TITLE:
                    getActionBar().setTitle((String) msg.obj);
                    break;
                case INVALIDATE_OPTION_MENU:
                    invalidateOptionsMenu();
                    break;
                default:
            }
            mSurfaceView.requestRender();
        }
    };
}
