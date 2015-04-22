package com.gomdev.gallery;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.util.Log;

import com.gomdev.gallery.GalleryConfig.AlbumViewMode;
import com.gomdev.gallery.GalleryConfig.ImageViewMode;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

class GalleryContext {
    private static final String CLASS = "GalleryContext";
    private static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryConfig.DEBUG;

    static final Object sLockObject = new Object();
    private static GalleryContext sGalleryContext;

    static GalleryContext newInstance(Context context) {
        sGalleryContext = new GalleryContext();
        ImageManager.newInstance(context);
        ImageLoader imageLoader = ImageLoader.newInstance(context);

        if (!(context instanceof MainActivity)) {
            imageLoader.checkAndLoadImages();
        }

        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);

            if (packageInfo != null) {
                GalleryContext.getInstance().setVersionCode(packageInfo.versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return sGalleryContext;
    }

    static GalleryContext getInstance() {
        return sGalleryContext;
    }

    private int mDefaultNumOfColumns = GalleryConfig.DEFAULT_NUM_OF_COLUMNS;
    private int mMinNumOfColumns = GalleryConfig.DEFAULT_NUM_OF_COLUMNS;
    private int mMaxNumOfColumns = GalleryConfig.DEFAULT_NUM_OF_COLUMNS * 3;
    private int mDefaultColumnWidth;
    private int mActionBarHeight = 0;
    private int mSystemBarHeight = 0;
    private int mVersionCode = 100;

    private ImageIndexingInfo mImageIndexingInfo = new ImageIndexingInfo(0, 0, 0);
    private RectF mCurrentViewport = null;

    private ImageViewMode mImageViewMode = ImageViewMode.ALBUME_VIEW_MODE;
    private AlbumViewMode mAlbumViewMode = AlbumViewMode.NORMAL_MODE;

    private SortedSet<ImageIndexingInfo> mCheckedImageIndexingInfos = null;

    private GalleryContext() {
        mCheckedImageIndexingInfos = new TreeSet<>(mImageIndexingInfoComparator);
    }

    int getDefaultColumnWidth() {
        return mDefaultColumnWidth;
    }

    void setDefaultColumnWidth(int columnWidth) {
        mDefaultColumnWidth = columnWidth;
    }

    int getDefaultNumOfColumns() {
        return mDefaultNumOfColumns;
    }

    void setDefaultNumOfColumns(int defaultNumOfColumns) {
        mDefaultNumOfColumns = defaultNumOfColumns;
    }

    int getMinNumOfColumns() {
        return mMinNumOfColumns;
    }

    void setMinNumOfColumns(int minNumOfColumns) {
        mMinNumOfColumns = minNumOfColumns;
    }

    int getMaxNumOfColumns() {
        return mMaxNumOfColumns;
    }

    void setMaxNumOfColumns(int maxNumOfColumns) {
        mMaxNumOfColumns = maxNumOfColumns;
    }

    int getActionBarHeight() {
        return mActionBarHeight;
    }

    void setActionBarHeight(int height) {
        mActionBarHeight = height;
    }

    int getSystemBarHeight() {
        return mSystemBarHeight;
    }

    void setSystemBarHeight(int height) {
        mSystemBarHeight = height;
    }

    int getVersionCode() {
        return mVersionCode;
    }

    void setVersionCode(int version) {
        mVersionCode = version;
    }

    void setImageIndexingInfo(ImageIndexingInfo indexingInfo) {
        mImageIndexingInfo = indexingInfo;
    }

    ImageIndexingInfo getImageIndexingInfo() {
        return mImageIndexingInfo;
    }

    void setCurrentViewport(RectF viewport) {
        mCurrentViewport = viewport;
    }

    RectF getCurrentViewport() {
        return mCurrentViewport;
    }

    void setImageViewMode(ImageViewMode mode) {
        mImageViewMode = mode;
    }

    ImageViewMode getImageViewMode() {
        return mImageViewMode;
    }

    void setAlbumViewMode(AlbumViewMode mode) {
        mAlbumViewMode = mode;

        if (mode == AlbumViewMode.NORMAL_MODE) {
            mCheckedImageIndexingInfos.clear();
        }
    }

    AlbumViewMode getAlbumViewMode() {
        return mAlbumViewMode;
    }

    void checkImageIndexingInfo(ImageIndexingInfo info) {
        mCheckedImageIndexingInfos.add(info);

        dumpCheckedImageIndexingInfos();
    }

    void uncheckImageIndexingInfo(ImageIndexingInfo info) {
        mCheckedImageIndexingInfos.remove(info);

        dumpCheckedImageIndexingInfos();
    }

    SortedSet<ImageIndexingInfo> getCheckedImageIndexingInfos() {
        return mCheckedImageIndexingInfos;
    }

    void dumpCheckedImageIndexingInfos() {
        if (DEBUG) {
            Log.d(TAG, "dumpCheckedImageIndexingInfos()");
            for (ImageIndexingInfo info : mCheckedImageIndexingInfos) {
                Log.d(TAG, "\t " + info);
            }
        }
    }

    private Comparator<ImageIndexingInfo> mImageIndexingInfoComparator = new Comparator<ImageIndexingInfo>() {
        @Override
        public int compare(ImageIndexingInfo info1, ImageIndexingInfo info2) {
            if (info1.mBucketIndex < info2.mBucketIndex) {
                return 1;
            } else if (info1.mBucketIndex > info2.mBucketIndex) {
                return -1;
            }

            if (info1.mDateLabelIndex < info2.mDateLabelIndex) {
                return 1;
            } else if (info1.mDateLabelIndex > info2.mDateLabelIndex) {
                return -1;
            }

            if (info1.mImageIndex < info2.mImageIndex) {
                return 1;
            } else if (info1.mImageIndex > info2.mImageIndex) {
                return -1;
            }

            return 0;
        }
    };
}
