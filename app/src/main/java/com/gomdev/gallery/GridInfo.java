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
    private final int mColumnWidth;
    private final int mNumOfColumns;
    private final int mNumOfRows;
    private final int mNumOfImages;
    private final int mScrollableHeight;

    private int mNumOfRowsInScreen;
    private int mNumOfImagesInScreen;


    public GridInfo(Context context, BucketInfo bucketInfo) {
        mBucketInfo = bucketInfo;

        mNumOfImages = bucketInfo.getNumOfImageInfos();

        GalleryContext galleryContext = GalleryContext.getInstance();
        mColumnWidth = galleryContext.getColumnWidth();
        mNumOfColumns = galleryContext.getNumOfColumns();
        mActionBarHeight = galleryContext.getActionBarHeight();

        mSpacing = context.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

        mNumOfRows = (int) Math.ceil((double) mNumOfImages / mNumOfColumns);
        mScrollableHeight = mActionBarHeight + (mColumnWidth + mSpacing) * mNumOfRows;
    }

    public void setScreenSize(int width, int height) {
        mNumOfRowsInScreen = (int) Math.ceil((double) height / (mColumnWidth + mSpacing));
        mNumOfImagesInScreen = mNumOfColumns * mNumOfRowsInScreen;

        if (mNumOfImages < mNumOfImagesInScreen) {
            mNumOfImagesInScreen = mNumOfImages;
        }

        // FIX-ME
        mNumOfImagesInScreen = mNumOfImages;
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

    public int getNumOfImagesInScreen() {
        return mNumOfImagesInScreen;
    }
}
