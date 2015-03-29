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

    private int mNumOfColumns = GalleryConfig.DEFAULT_NUM_OF_COLUMNS;
    private int mGridColumnWidth;
    private int mActionBarHeight = 0;
    private int mSystemBarHeight = 0;
    private int mVersionCode = 100;

    private ImageIndexingInfo mImageIndexingInfo = new ImageIndexingInfo(0, 0, 0);
    private RectF mCurrentViewport = null;

    private GalleryConfig.ImageViewMode mImageViewMode = GalleryConfig.ImageViewMode.ALBUME_VIEW_MODE;

    private GalleryContext() {

    }

    int getColumnWidth() {
        return mGridColumnWidth;
    }

    void setColumnWidth(int columnWidth) {
        mGridColumnWidth = columnWidth;
    }

    int getNumOfColumns() {
        return mNumOfColumns;
    }

    void setNumOfColumns(int numOfColumns) {
        mNumOfColumns = numOfColumns;
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
