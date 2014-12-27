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
    private final int mMinNumOfColumns;
    private final int mMaxNumOfColumns;

    private int mColumnWidth;
    private int mNumOfRows;
    private int mScrollableHeight;

    private int mNumOfColumns;
    private int mNumOfRowsInScreen;
    private int mNumOfImagesInScreen;


    private int mWidth = 0;
    private int mHeight = 0;


    public GridInfo(Context context, BucketInfo bucketInfo) {
        mBucketInfo = bucketInfo;
        mNumOfImages = bucketInfo.getNumOfImageInfos();

        GalleryContext galleryContext = GalleryContext.getInstance();
        mColumnWidth = galleryContext.getColumnWidth();
        mNumOfColumns = galleryContext.getNumOfColumns();
        mMinNumOfColumns = mNumOfColumns;
        mMaxNumOfColumns = 3 * mNumOfColumns;
        mActionBarHeight = galleryContext.getActionBarHeight();

        mSpacing = context.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

        mNumOfRows = (int) Math.ceil((double) mNumOfImages / mNumOfColumns);
        mScrollableHeight = mActionBarHeight + (mColumnWidth + mSpacing) * mNumOfRows;
    }

    public void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;

        mNumOfRowsInScreen = (int) Math.ceil((double) height / (mColumnWidth + mSpacing));
        mNumOfImagesInScreen = mNumOfColumns * mNumOfRowsInScreen;

        if (mNumOfImages < mNumOfImagesInScreen) {
            mNumOfImagesInScreen = mNumOfImages;
        }
    }

    public void resize(int numOfColumns) {
        numOfColumns = Math.max(numOfColumns, mMinNumOfColumns);
        numOfColumns = Math.min(numOfColumns, mMaxNumOfColumns);

        if (numOfColumns != mNumOfColumns) {
            mNumOfColumns = numOfColumns;
            mColumnWidth = (int) ((mWidth - mSpacing * (mNumOfColumns + 1)) / mNumOfColumns);

            mNumOfRows = (int) Math.ceil((double) mNumOfImages / mNumOfColumns);
            mScrollableHeight = mActionBarHeight + (mColumnWidth + mSpacing) * mNumOfRows;

            mNumOfRowsInScreen = (int) Math.ceil((double) mHeight / (mColumnWidth + mSpacing));
            mNumOfImagesInScreen = mNumOfColumns * mNumOfRowsInScreen;
        }
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
