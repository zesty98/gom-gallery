package com.gomdev.gallery;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by gomdev on 15. 1. 9..
 */
class DateLabelInfo implements Serializable, GalleryInfo {
    static final String CLASS = "DateInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final String mDate;

    private int mIndex = 0;

    private int mNumOfRows = 0;

    private int mImageIndex = 0;

    private LinkedList<ImageInfo> mImageInfos = new LinkedList<>();

    DateLabelInfo(String date) {
        mDate = date;

        mImageInfos.clear();
    }

    String getDate() {
        return mDate;
    }

    void setIndex(int index) {
        mIndex = index;
    }

    int getIndex() {
        return mIndex;
    }

    void add(ImageInfo imageInfo) {
        mImageInfos.add(imageInfo);
        imageInfo.setIndex(mImageIndex++);
    }

    ImageInfo get(int position) {
        return mImageInfos.get(position);
    }

    ImageInfo getFirst() {
        return mImageInfos.getFirst();
    }

    ImageInfo getLast() {
        return mImageInfos.getLast();
    }

    int getNumOfImages() {
        return mImageInfos.size();
    }

    void setNumOfColumns(int numOfColumns) {
        mNumOfRows = (int) Math.ceil((double) mImageInfos.size() / numOfColumns);
    }

    int getNumOfRows() {
        return mNumOfRows;
    }

    void deleteImageInfo(int index) {
        mImageInfos.remove(index);

        int size = mImageInfos.size();
        for (int i = index; i < size; i++) {
            mImageInfos.get(i).setIndex(i);
        }
    }
}
