package com.gomdev.gallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.gomdev.gallery.GalleryConfig.AlbumViewMode;
import com.gomdev.gallery.GalleryConfig.DeletedInfo;
import com.gomdev.gallery.GalleryConfig.ImageViewMode;
import com.gomdev.gallery.GalleryConfig.VisibleMode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

public class ImageListActivity extends Activity {
    static final String CLASS = "ImageListActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    final static int SET_SYSTEM_UI_FLAG_LOW_PROFILE = 102;
    final static int SET_SYSTEM_UI_FLAG_VISIBLE = 103;
    final static int UPDATE_ACTION_BAR_TITLE = 104;
    final static int INVALIDATE_OPTION_MENU = 105;
    final static int DISMISS_PROGRESS_DIALOG = 106;
    final static int UPDATE_PROGRESS_DIALOG = 107;
    final static int FAIL_IMAGE_DELETION_FROM_DATABASE = 108;
    final static int DELETE_BUCKET = 109;

    private GallerySurfaceView mSurfaceView = null;
    private GalleryContext mGalleryContext = null;
    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;

    private ProgressDialog mProgressDialog = null;

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
        }

        GalleryUtils.setDefaultInfo(this);

        mImageManager = ImageManager.getInstance();
        if (mImageManager == null) {
            mImageManager = ImageManager.newInstance(this);
        }

        GalleryUtils.setSystemUiVisibility(this, VisibleMode.VISIBLE_TRANSPARENT_MODE);

        int bucketPosition = getIntent().getIntExtra(GalleryConfig.BUCKET_INDEX, 0);
        BucketInfo bucketInfo = mImageManager.getBucketInfo(bucketPosition);
        getActionBar().setTitle(bucketInfo.getName());

        mImageManager.setCurrentBucketInfo(bucketPosition);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GalleryUtils.setActionBarElevation(this);
        }

        SharedPreferences pref = getSharedPreferences(GalleryConfig.PREF_NAME, 0);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        int numOfColumnsInPortrait = 0;
        int numOfColumnsInLandScape = 0;

        if (width < height) {
            numOfColumnsInPortrait = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_PORTRAIT, 0);

            if (numOfColumnsInPortrait == 0) {
                numOfColumnsInPortrait = mGalleryContext.getDefaultNumOfColumns();
            }

            numOfColumnsInLandScape = GalleryUtils.calcNumOfColumnsInLandscape(this, numOfColumnsInPortrait);
        } else {
            numOfColumnsInLandScape = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_LANDSCAPE, 0);

            if (numOfColumnsInLandScape == 0) {
                numOfColumnsInLandScape = mGalleryContext.getDefaultNumOfColumns();
            }

            numOfColumnsInPortrait = GalleryUtils.calcNumOfColumnsInPortrait(this, numOfColumnsInLandScape);
        }

        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_PORTRAIT, numOfColumnsInPortrait);
        editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_LANDSCAPE, numOfColumnsInLandScape);
        editor.commit();

        setContentView(R.layout.activity_gles_main);

        mGridInfo = new GridInfo(this, bucketInfo);

        FrameLayout layout = (FrameLayout) findViewById(R.id.container);

        mSurfaceView = new GallerySurfaceView(this);
        layout.addView(mSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageListRenderer renderer = new ImageListRenderer(this, mGridInfo);

        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mSurfaceView.setRenderer(renderer);
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mSurfaceView.setPreserveEGLContextOnPause(true);

        renderer.setSurfaceView(mSurfaceView);
        renderer.setHandler(mHandler);

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

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Deleting");
        mProgressDialog.setMessage("...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }


    private void updateActionBarTitle() {
        ImageIndexingInfo indexingInfo = mGalleryContext.getImageIndexingInfo();
        BucketInfo bucketInfo = mImageManager.getBucketInfo(indexingInfo.mBucketIndex);
        DateLabelInfo dateLabelInfo = bucketInfo.get(indexingInfo.mDateLabelIndex);

        ImageViewMode mode = mGalleryContext.getImageViewMode();
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
        ImageViewMode mode = mGalleryContext.getImageViewMode();

        switch (mode) {
            case ALBUME_VIEW_MODE:
                AlbumViewMode albumViewMode = mGalleryContext.getAlbumViewMode();
                if (albumViewMode == AlbumViewMode.NORMAL_MODE) {
                    if (menu.findItem(R.id.action_sort) == null) {
                        getMenuInflater().inflate(R.menu.album_view_normal_menu, menu);
                    }
                } else {
                    if (menu.findItem(R.id.action_selection_delete) == null) {
                        getMenuInflater().inflate(R.menu.album_view_selection_menu, menu);
                    }
                }

                break;
            case DETAIL_VIEW_MODE:
                if (menu.findItem(R.id.action_delete) == null) {
                    getMenuInflater().inflate(R.menu.detail_view_menu, menu);
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
            showDeleteAlertDialog(true);
            return true;
        } else if (id == R.id.action_share) {
            share();
            return true;
        } else if (id == R.id.action_sort) {
            AlbumViewSortOptionDialog dialog = new AlbumViewSortOptionDialog();
            dialog.show(getFragmentManager(), "sort by");
        } else if (id == R.id.action_selection) {
            GalleryContext.getInstance().setAlbumViewMode(AlbumViewMode.MULTI_SELECTION_MODE);
            invalidateOptionsMenu();
        } else if (id == R.id.action_selection_delete) {
            showDeleteAlertDialog(false);
        }

        return super.onOptionsItemSelected(item);
    }

    private void delete() {
        Set<DeletedInfo> deletedInfo = new HashSet<>();

        GalleryContext galleryContext = GalleryContext.getInstance();
        ImageIndexingInfo imageIndexingInfo = galleryContext.getImageIndexingInfo();

        ImageManager.getInstance().deleteImage(imageIndexingInfo, deletedInfo);
        boolean isBucketDeleted = deletedInfo.contains(DeletedInfo.BUCKET);
        if (isBucketDeleted == true) {
            Intent intent = new Intent(this, BucketListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            galleryContext.setImageViewMode(ImageViewMode.ALBUME_VIEW_MODE);
            Intent intent = new Intent(this, ImageListActivity.class);
            intent.putExtra(GalleryConfig.BUCKET_INDEX, imageIndexingInfo.mBucketIndex);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void showDeleteAlertDialog(final boolean isDetailView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (isDetailView == true) {
            builder.setMessage("Selected file will be deleted. Delete?");
        } else {
            builder.setMessage("Selected files will be deleted. Delete?");
        }
        builder.setTitle(R.string.albumview_delete_option_dialog_title);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (isDetailView == true) {
                    delete();
                } else {
                    executeDeletion();
                }
            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (isDetailView == false) {
                    GalleryContext.getInstance().setAlbumViewMode(AlbumViewMode.NORMAL_MODE);
                    invalidateOptionsMenu();
                    mSurfaceView.requestRender();
                }
            }
        });
        builder.show();
    }

    private void executeDeletion() {
        long tick = System.nanoTime();

        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                deleteCheckedObjects();
            }
        }).start();

        if (DEBUG) {
            Log.d(TAG, "onOptionsItemSelected() deleteCheckedObjects() duration=" + (System.nanoTime() - tick) / 1000000L);
        }
    }

    private void deleteCheckedObjects() {
        Set<DeletedInfo> deletedInfo = new HashSet<>();

        GalleryContext galleryContext = GalleryContext.getInstance();
        SortedSet<ImageIndexingInfo> set = galleryContext.getCheckedImageIndexingInfos();
        Iterator<ImageIndexingInfo> iter = set.iterator();

        synchronized (GalleryContext.sLockObject) {
            int size = set.size();

            ImageIndexingInfo imageIndexingInfo = null;
            for (int i = 0; iter.hasNext(); i++) {
                imageIndexingInfo = iter.next();

                ImageManager.getInstance().deleteImage(imageIndexingInfo, deletedInfo);
                boolean isNotDeleted = deletedInfo.contains(DeletedInfo.NONE);
                if (isNotDeleted == true) {
                    mHandler.sendEmptyMessage(FAIL_IMAGE_DELETION_FROM_DATABASE);
                }

                boolean isBucketDeleted = deletedInfo.contains(DeletedInfo.BUCKET);
                if (isBucketDeleted == true) {
                    mHandler.sendEmptyMessage(DELETE_BUCKET);
                    return;
                }

                Message msg = mHandler.obtainMessage(UPDATE_PROGRESS_DIALOG);
                String str = (i + 1) + " / " + size;
                msg.obj = str;
                mHandler.sendMessage(msg);
            }

            boolean isImageDeleted = deletedInfo.contains(DeletedInfo.IMAGE);
            if (isImageDeleted == true) {
                mGridInfo.deleteImageInfo();
            }

            boolean isDateLabelDeleted = deletedInfo.contains(DeletedInfo.DATELABEL);
            if (isDateLabelDeleted == true) {
                mGridInfo.deleteDateLabelInfo();
            }
        }

        mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);

        galleryContext.setImageViewMode(ImageViewMode.ALBUME_VIEW_MODE);
        galleryContext.setAlbumViewMode(AlbumViewMode.NORMAL_MODE);

        invalidateOptionsMenu();

        mSurfaceView.requestRender();
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
                case UPDATE_PROGRESS_DIALOG:
                    mProgressDialog.setMessage((String) msg.obj);
                    break;
                case DISMISS_PROGRESS_DIALOG:
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    break;
                case FAIL_IMAGE_DELETION_FROM_DATABASE:
                    Toast.makeText(ImageListActivity.this, "Delete Failed : Image",
                            Toast.LENGTH_SHORT)
                            .show();
                    break;
                case DELETE_BUCKET:
                    Intent intent = new Intent(ImageListActivity.this, BucketListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    break;
                default:
            }
            mSurfaceView.requestRender();
        }
    };
}
