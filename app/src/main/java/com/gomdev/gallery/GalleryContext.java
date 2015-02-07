package com.gomdev.gallery;

import android.content.Context;

class GalleryContext {
    static final Object sLockObject = new Object();
    private static GalleryContext sGalleryContext;

    private int mWidth;
    private int mHeight;

    private int mNumOfColumns = GalleryConfig.DEFAULT_NUM_OF_COLUMNS;
    private int mGridColumnWidth;

    private int mActionBarHeight = 0;

    private int mVersionCode = 100;

    static GalleryContext newInstance(Context context) {
        sGalleryContext = new GalleryContext();
        ImageManager.newInstance(context);
        ImageLoader.newInstance(context);
        return sGalleryContext;
    }

    static GalleryContext getInstance() {
        return sGalleryContext;
    }

    private GalleryContext() {

    }


    void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
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

    void setActionbarHeight(int height) {
        mActionBarHeight = height;
    }

    int getVersionCode() {
        return mVersionCode;
    }

    void setVersionCode(int version) {
        mVersionCode = version;
    }
}
