package com.gomdev.gallery;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by gomdev on 15. 1. 9..
 */
public class DateLabelInfo implements Serializable, GalleryInfo {
    static final String CLASS = "DateInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final String mDate;

    private int mIndex = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mNumOfRows = 0;
    private int mNumOfColumns = 0;

    private int mFirstImageInfoIndex = 0;
    private int mLastImageInfoIndex = 0;

    private int mImageInfoIndex = 0;

    private List<ImageInfo> mImageInfos = new LinkedList<>();

    public DateLabelInfo(int index, String date) {
        mIndex = index;
        mDate = date;

        mImageInfos.clear();
    }

    public String getDate() {
        return mDate;
    }

    public void setIndex(int index) {
        mIndex = index;
    }

    public int getIndex() {
        return mIndex;
    }

    public void add(ImageInfo imageInfo) {
        mImageInfos.add(imageInfo);
        imageInfo.setDateLabelInfo(this);
        imageInfo.setIndexInDateLabelInfo(mImageInfoIndex);
        mImageInfoIndex++;
    }

    public ImageInfo get(int position) {
        return mImageInfos.get(position);
    }

    public int getNumOfImages() {
        return mImageInfos.size();
    }

    public void setNumOfColumns(int numOfColumns) {
        mNumOfRows = (int) Math.ceil((double) mImageInfos.size() / numOfColumns);
        mNumOfColumns = numOfColumns;
    }

    public int getNumOfRows() {
        return mNumOfRows;
    }

    public void setFirstImageInfoIndex(int position) {
        mFirstImageInfoIndex = position;
    }

    public int getFirstImageInfoIndex() {
        return mFirstImageInfoIndex;
    }

    public void setLastImageInfoIndex(int position) {
        mLastImageInfoIndex = position;
    }

    public int getLastImageInfoIndex() {
        return mLastImageInfoIndex;
    }

    public void deleteImageInfo(int index) {
        mImageInfos.remove(index);

        int size = mImageInfos.size();
        for (int i = index; i < size; i++) {
            mImageInfos.get(i).setIndexInDateLabelInfo(i);
        }
    }
}
