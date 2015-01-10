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
    private final int mDateLabelHeight;
    private final int mSpacing;
    private final int mNumOfImages;
    private final int mNumOfDateInfos;

    private int mColumnWidth;
    private int mNumOfRows;
    private int mScrollableHeight;

    private int mNumOfColumns;

    private int mWidth = 0;
    private int mHeight = 0;


    public GridInfo(Context context, BucketInfo bucketInfo) {
        mBucketInfo = bucketInfo;
        mNumOfImages = bucketInfo.getNumOfImages();
        mNumOfDateInfos = bucketInfo.getNumOfDateInfos();

        GalleryContext galleryContext = GalleryContext.getInstance();
        mColumnWidth = galleryContext.getColumnWidth();
        mNumOfColumns = galleryContext.getNumOfColumns();
        mActionBarHeight = galleryContext.getActionBarHeight();
        mDateLabelHeight = mActionBarHeight;

        mSpacing = context.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

        setNumOfColumnsToDateInfo(mNumOfColumns);

        mNumOfRows = calcNumOfRows();
        mScrollableHeight = calcScrollableHeight();
    }

    private void setNumOfColumnsToDateInfo(int numOfColumns) {
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mBucketInfo.getDateInfo(i).setNumOfColumns(numOfColumns);
        }
    }

    public void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void resize(int numOfColumns) {
        mNumOfColumns = numOfColumns;

        setNumOfColumnsToDateInfo(mNumOfColumns);

        mColumnWidth = calcColumnWidth();
        mNumOfRows = calcNumOfRows();
        mScrollableHeight = calcScrollableHeight();
    }

    private int calcColumnWidth() {
        int columnWidth = (int) ((mWidth - mSpacing * (mNumOfColumns + 1)) / mNumOfColumns);
        return columnWidth;
    }

    private int calcScrollableHeight() {
        int scrollableHeight = mActionBarHeight + (mColumnWidth + mSpacing) * mNumOfRows + (mDateLabelHeight + mSpacing) * mNumOfDateInfos + mSpacing;
        return scrollableHeight;
    }

    private int calcNumOfRows() {
        int totalNumOfRows = 0;
        int numOfRows = 0;

        for (int i = 0; i < mNumOfDateInfos; i++) {
            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            numOfRows = dateLabelInfo.getNumOfRows();
            totalNumOfRows += numOfRows;
        }

        return totalNumOfRows;
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

    public int getNumOfImages() {
        return mNumOfImages;
    }

    public int getNumOfDateInfos() {
        return mNumOfDateInfos;
    }

    public int getDateLabelHeight() {
        return mDateLabelHeight;
    }

    public int getScrollableHeight() {
        return mScrollableHeight;
    }
}
