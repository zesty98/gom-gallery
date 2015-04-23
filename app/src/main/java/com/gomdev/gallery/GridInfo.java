package com.gomdev.gallery;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomdev on 14. 12. 20..
 */
class GridInfo {
    static final String CLASS = "GridInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final BucketInfo mBucketInfo;

    private final int mActionBarHeight;
    private final int mSystemBarHeight; // actionBar + status bar
    private final int mDateLabelHeight;
    private final int mSpacing;

    private int mNumOfDateInfos = 0;
    private int mColumnWidth = 0;
    private int mDefaultColumnWidth = 0;
    private int mNumOfRows = 0;
    private int mScrollableHeight = 0;
    private int mDateLabelWidth = 0;

    private int mNumOfColumns = 0;
    private int mMinNumOfColumns = 0;
    private int mMaxNumOfColumns = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    private List<GridInfoChangeListener> mListeners = new ArrayList<>();

    GridInfo(Context context, BucketInfo bucketInfo) {
        mContext = context;
        mBucketInfo = bucketInfo;

        mListeners.clear();

        mNumOfDateInfos = bucketInfo.getNumOfDateInfos();
        mSpacing = context.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

        GalleryContext galleryContext = GalleryContext.getInstance();
        mActionBarHeight = galleryContext.getActionBarHeight();
        mSystemBarHeight = galleryContext.getSystemBarHeight();
        mDateLabelHeight = mActionBarHeight;
        mDefaultColumnWidth = galleryContext.getDefaultColumnWidth();
        mMinNumOfColumns = galleryContext.getMinNumOfColumns();
        mMaxNumOfColumns = galleryContext.getMaxNumOfColumns();

        SharedPreferences pref = context.getSharedPreferences(GalleryConfig.PREF_NAME, 0);

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        if (width < height) {
            mNumOfColumns = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_PORTRAIT, 0);
        } else {
            mNumOfColumns = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_LANDSCAPE, 0);
        }

        mColumnWidth = GalleryUtils.calcColumnWidth(mNumOfColumns, width, mSpacing);

        setNumOfColumnsToDateInfo(mNumOfColumns);

        mNumOfRows = calcNumOfRows();
        mScrollableHeight = calcScrollableHeight();
    }

    private void setNumOfColumnsToDateInfo(int numOfColumns) {
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mBucketInfo.get(i).setNumOfColumns(numOfColumns);
        }
    }

    void addListener(GridInfoChangeListener listener) {
        mListeners.add(listener);
    }

    void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;

        mDateLabelWidth = width - mSpacing * 2;
    }

    void resize(int numOfColumns) {
        synchronized (GalleryContext.sLockObject) {
            mNumOfColumns = numOfColumns;

            setNumOfColumnsToDateInfo(mNumOfColumns);

            mColumnWidth = calcColumnWidth();
            mNumOfRows = calcNumOfRows();
            mScrollableHeight = calcScrollableHeight();

            int size = mListeners.size();
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onColumnWidthChanged();
            }
        }

        SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        if (mWidth < mHeight) {
            editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_PORTRAIT, mNumOfColumns);
            int numOfColumnsInLandscape = GalleryUtils.calcNumOfColumnsInLandscape(mContext, mNumOfColumns);
            editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_LANDSCAPE, numOfColumnsInLandscape);
        } else {
            editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_LANDSCAPE, mNumOfColumns);
            int numOfColumnsInPortrait = GalleryUtils.calcNumOfColumnsInPortrait(mContext, mNumOfColumns);
            editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS_IN_PORTRAIT, numOfColumnsInPortrait);
        }
        editor.commit();
    }

    private int calcColumnWidth() {
        int columnWidth = (int) ((mWidth - mSpacing * (mNumOfColumns + 1)) / mNumOfColumns);
        return columnWidth;
    }

    private int calcScrollableHeight() {
        int scrollableHeight = mSystemBarHeight + (mColumnWidth + mSpacing) * mNumOfRows + (mDateLabelHeight + mSpacing) * mNumOfDateInfos + mSpacing;
        return scrollableHeight;
    }

    private int calcNumOfRows() {
        int totalNumOfRows = 0;
        int numOfRows = 0;

        for (int i = 0; i < mNumOfDateInfos; i++) {
            DateLabelInfo dateLabelInfo = mBucketInfo.get(i);
            numOfRows = dateLabelInfo.getNumOfRows();
            totalNumOfRows += numOfRows;
        }

        return totalNumOfRows;
    }

    BucketInfo getBucketInfo() {
        return mBucketInfo;
    }

    int getActionBarHeight() {
        return mActionBarHeight;
    }

    int getSystemBarHeight() {
        return mSystemBarHeight;
    }

    int getSpacing() {
        return mSpacing;
    }

    int getColumnWidth() {
        return mColumnWidth;
    }

    int getDefaultColumnWidth() {
        return mDefaultColumnWidth;
    }

    int getNumOfColumns() {
        return mNumOfColumns;
    }

    int getMinNumOfColumns() {
        return mMinNumOfColumns;
    }

    int getMaxNumOfColumns() {
        return mMaxNumOfColumns;
    }

    int getNumOfDateInfos() {
        return mNumOfDateInfos;
    }

    int getDateLabelWidth() {
        return mDateLabelWidth;
    }

    int getDateLabelHeight() {
        return mDateLabelHeight;
    }

    int getScrollableHeight() {
        return mScrollableHeight;
    }

    void deleteImageInfo() {
        synchronized (GalleryContext.sLockObject) {
            setNumOfColumnsToDateInfo(mNumOfColumns);

            mNumOfRows = calcNumOfRows();
            mScrollableHeight = calcScrollableHeight();

            int size = mListeners.size();
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onImageDeleted();
            }
        }
    }

    void deleteDateLabelInfo() {
        synchronized (GalleryContext.sLockObject) {
            mNumOfDateInfos = mBucketInfo.getNumOfDateInfos();

            setNumOfColumnsToDateInfo(mNumOfColumns);

            mNumOfRows = calcNumOfRows();
            mScrollableHeight = calcScrollableHeight();

            int size = mListeners.size();
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onDateLabelDeleted();
            }
        }
    }

    private float mTranslateY = 0f;
    private float mTranslateZ = 0f;
    private float mRotateX = 0f;

    void setTranslateY(float y) {
        mTranslateY = y;
    }

    float getTranslateY() {
        return mTranslateY;
    }

    void setTranslateZ(float z) {
        mTranslateZ = z;
    }

    float getTranslateZ() {
        return mTranslateZ;
    }

    void setRotateX(float angle) {
        mRotateX = angle;
    }

    float getRotateX() {
        return mRotateX;
    }
}
