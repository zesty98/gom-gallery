package com.gomdev.gallery;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomdev on 14. 12. 20..
 */
public class GridInfo {
    static final String CLASS = "GridInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final BucketInfo mBucketInfo;

    private final int mActionBarHeight;
    private final int mDateLabelHeight;
    private final int mSpacing;

    private int mNumOfDateInfos;
    private int mNumOfImages;
    private int mColumnWidth;
    private int mDefaultColumnWidth;
    private int mNumOfRows;
    private int mScrollableHeight;

    private int mNumOfColumns;
    private int mMinNumOfColumns;
    private int mMaxNumOfColumns;

    private int mWidth = 0;
    private int mHeight = 0;

    private float mScale = 1f;

    private List<GridInfoChangeListener> mListeners = new ArrayList<>();

    public GridInfo(Context context, BucketInfo bucketInfo) {
        mContext = context;
        mBucketInfo = bucketInfo;

        mListeners.clear();

        mNumOfImages = bucketInfo.getNumOfImages();
        mNumOfDateInfos = bucketInfo.getNumOfDateInfos();
        mSpacing = context.getResources().getDimensionPixelSize(R.dimen.gridview_spacing);

        GalleryContext galleryContext = GalleryContext.getInstance();
        mActionBarHeight = galleryContext.getActionBarHeight();
        mDateLabelHeight = mActionBarHeight;
        mDefaultColumnWidth = galleryContext.getColumnWidth();

        SharedPreferences pref = context.getSharedPreferences(GalleryConfig.PREF_NAME, 0);

        mColumnWidth = pref.getInt(GalleryConfig.PREF_COLUMNS_WIDTH, 0);
        mNumOfColumns = pref.getInt(GalleryConfig.PREF_NUM_OF_COLUMNS, 0);
        mMinNumOfColumns = pref.getInt(GalleryConfig.PREF_MIN_NUM_OF_COLUMNS, 0);
        mMaxNumOfColumns = pref.getInt(GalleryConfig.PREF_MAX_NUM_OF_COLUMNS, 0);

        setNumOfColumnsToDateInfo(mNumOfColumns);

        mNumOfRows = calcNumOfRows();
        mScrollableHeight = calcScrollableHeight();
    }

    private void setNumOfColumnsToDateInfo(int numOfColumns) {
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mBucketInfo.getDateLabelInfo(i).setNumOfColumns(numOfColumns);
        }
    }

    public void addListener(GridInfoChangeListener listener) {
        mListeners.add(listener);
    }

    public void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void resize(int numOfColumns) {
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
        editor.putInt(GalleryConfig.PREF_COLUMNS_WIDTH, mColumnWidth);
        editor.putInt(GalleryConfig.PREF_NUM_OF_COLUMNS, mNumOfColumns);
        editor.commit();
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
            DateLabelInfo dateLabelInfo = mBucketInfo.getDateLabelInfo(i);
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

    public int getDefaultColumnWidth() {
        return mDefaultColumnWidth;
    }

    public int getNumOfColumns() {
        return mNumOfColumns;
    }

    public int getMinNumOfColumns() {
        return mMinNumOfColumns;
    }

    public int getMaxNumOfColumns() {
        return mMaxNumOfColumns;
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

    public void setScale(float scale) {
        mScale = scale;
    }

    public float getScale() {
        return mScale;
    }

    public void deleteImageInfo() {
        synchronized (GalleryContext.sLockObject) {
            mNumOfImages = mBucketInfo.getNumOfImages();

            setNumOfColumnsToDateInfo(mNumOfColumns);

            mNumOfRows = calcNumOfRows();
            mScrollableHeight = calcScrollableHeight();

            int size = mListeners.size();
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onNumOfImageInfosChanged();
            }
        }
    }

    public void deleteDateLabelInfo() {
        synchronized (GalleryContext.sLockObject) {
            mNumOfDateInfos = mBucketInfo.getNumOfDateInfos();

            setNumOfColumnsToDateInfo(mNumOfColumns);

            mNumOfRows = calcNumOfRows();
            mScrollableHeight = calcScrollableHeight();

            int size = mListeners.size();
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onNumOfDateLabelInfosChanged();
            }
        }
    }

    private float mTranslateY = 0f;
    private float mTranslateZ = 0f;
    private float mRotateX = 0f;

    public void setTranslateY(float y) {
        mTranslateY = y;
    }

    public float getTranslateY() {
        return mTranslateY;
    }

    public void setTranslateZ(float z) {
        mTranslateZ = z;
    }

    public float getTranslateZ() {
        return mTranslateZ;
    }

    public void setRotateX(float angle) {
        mRotateX = angle;
    }

    public float getRotateX() {
        return mRotateX;
    }
}
