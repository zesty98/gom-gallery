package com.gomdev.gallery;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class ImageListActivity extends Activity {
    static final String CLASS = "ImageListActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    final static int SET_SYSTEM_UI_FLAG_LOW_PROFILE = 102;
    final static int SET_SYSTEM_UI_FLAG_VISIBLE = 103;
    final static int UPDATE_ACTION_BAR_TITLE = 104;

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_INDEX, 0);
        BucketInfo bucketInfo = mImageManager.getBucketInfo(bucketPosition);
        getActionBar().setTitle(bucketInfo.getName());

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

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SET_SYSTEM_UI_FLAG_LOW_PROFILE:
                    mSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                    break;
                case SET_SYSTEM_UI_FLAG_VISIBLE:
                    mSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                    break;
                case UPDATE_ACTION_BAR_TITLE:
                    getActionBar().setTitle((String) msg.obj);
                    break;
                default:
            }
            mSurfaceView.requestRender();
        }
    };
}
