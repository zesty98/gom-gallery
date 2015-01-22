package com.gomdev.gallery;

import android.content.Context;

public class GalleryContext {
    static final Object sLockObject = new Object();
    private static GalleryContext sGalleryContext;

    private int mWidth;
    private int mHeight;

    private int mNumOfColumns = GalleryConfig.DEFAULT_NUM_OF_COLUMNS;
    private int mGridColumnWidth;

    private int mActionBarHeight = 0;

    private int mVersionCode = 100;

    public static GalleryContext newInstance(Context context) {
        sGalleryContext = new GalleryContext();
        ImageManager.newInstance();
        ImageLoader.newInstance(context);
        return sGalleryContext;
    }

    public static GalleryContext getInstance() {
        return sGalleryContext;
    }

    private GalleryContext() {

    }


    public void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getColumnWidth() {
        return mGridColumnWidth;
    }

    public void setColumnWidth(int columnWidth) {
        mGridColumnWidth = columnWidth;
    }

    public int getNumOfColumns() {
        return mNumOfColumns;
    }

    public void setNumOfColumns(int numOfColumns) {
        mNumOfColumns = numOfColumns;
    }

    public int getActionBarHeight() {
        return mActionBarHeight;
    }

    public void setActionbarHeight(int height) {
        mActionBarHeight = height;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public void setVersionCode(int version) {
        mVersionCode = version;
    }
}
