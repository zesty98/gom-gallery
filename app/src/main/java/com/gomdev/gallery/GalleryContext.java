package com.gomdev.gallery;

public class GalleryContext {
    private static GalleryContext sGalleryContext = new GalleryContext();

    private int mWidth;
    private int mHeight;

    private int mNumOfColumns = GalleryConfig.NUM_OF_COLUMNS;
    private int mGridColumnWidth;

    private int mVersionCode = 100;

    private GalleryContext() {

    }

    public static GalleryContext getInstance() {
        return sGalleryContext;
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

    public int getGridColumnWidth() {
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

    public int getVersionCode() {
        return mVersionCode;
    }

    public void setVersionCode(int version) {
        mVersionCode = version;
    }
}
