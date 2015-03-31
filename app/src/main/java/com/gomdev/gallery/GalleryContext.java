package com.gomdev.gallery;

import android.content.Context;
import android.graphics.RectF;

class GalleryContext {

    static final Object sLockObject = new Object();
    private static GalleryContext sGalleryContext;

    static GalleryContext newInstance(Context context) {
        sGalleryContext = new GalleryContext();
        ImageManager.newInstance(context);
        ImageLoader.newInstance(context);
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

    private GalleryConfig.ImageViewMode mImageViewMode = GalleryConfig.ImageViewMode.ALBUME_VIEW_MODE;

    private GalleryContext() {

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

    void setImageViewMode(GalleryConfig.ImageViewMode mode) {
        mImageViewMode = mode;
    }

    GalleryConfig.ImageViewMode getImageViewMode() {
        return mImageViewMode;
    }
}
