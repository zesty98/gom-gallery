package com.gomdev.gallery;

import android.content.Context;

/**
 * Created by gomdev on 14. 12. 20..
 */
public class GridInfo {
    static final String CLASS = "GridInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final BucketInfo mBucketInfo;

    private final int mActionBarHeight;
    private final int mSpacing;
    private final int mNumOfImages;

    private int mColumnWidth;
    private int mNumOfRows;
    private int mScrollableHeight;

    private int mNumOfColumns;
    private int mNumOfRowsInScreen;

    private int mWidth = 0;
    private int mHeight = 0;


    public GridInfo(Context context, BucketInfo bucketInfo) {
        mBucketInfo = bucketInfo;
        mNumOfImages = bucketInfo.getNumOfImageInfos();

        GalleryContext galleryContext = GalleryContext.getInstance();
        mColumnWidth = galleryContext.getColumnWidth();
        mNumOfColumns = galleryContext.getNumOfColumns();
        mActionBarHeight = galleryContext.getActionBarHeight();

        mSpacing = context.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

        mNumOfRows = calcNumOfRows();
        mScrollableHeight = calcScrollableHeight();
    }

    public void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;

        mNumOfRowsInScreen = calcNumOfRowsInScreen();
    }

    public void resize(int numOfColumns) {
        mNumOfColumns = numOfColumns;

        mColumnWidth = calcColumnWidth();
        mNumOfRows = calcNumOfRows();
        mScrollableHeight = calcScrollableHeight();
        mNumOfRowsInScreen = calcNumOfRowsInScreen();
    }

    private int calcColumnWidth() {
        int columnWidth = (int) ((mWidth - mSpacing * (mNumOfColumns + 1)) / mNumOfColumns);
        return columnWidth;
    }

    private int calcNumOfRowsInScreen() {
        int numOfRowsInScreen = (int) Math.ceil((double) mHeight / (mColumnWidth + mSpacing)) + 2;
        return numOfRowsInScreen;
    }

    private int calcScrollableHeight() {
        int scrollableHeight = mActionBarHeight + (mColumnWidth + mSpacing) * mNumOfRows;
        return scrollableHeight;
    }

    private int calcNumOfRows() {
        int numOfRows = (int) Math.ceil((double) mNumOfImages / mNumOfColumns);
        return numOfRows;
    }

    public BucketInfo getBucketInfo() {
        return mBucketInfo;
    }

    public int getActionBarHeight() {
        return mActionBarHeight;
    }

    public int getSpacing() {
        return mSpacing;
    }

    public int getColumnWidth() {
        return mColumnWidth;
    }

    public int getNumOfColumns() {
        return mNumOfColumns;
    }

    public int getNumOfRows() {
        return mNumOfRows;
    }

    public int getNumOfImages() {
        return mNumOfImages;
    }

    public int getScrollableHeight() {
        return mScrollableHeight;
    }

    public int getNumOfRowsInScreen() {
        return mNumOfRowsInScreen;
    }
}
