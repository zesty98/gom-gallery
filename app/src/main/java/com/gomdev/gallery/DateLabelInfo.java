package com.gomdev.gallery;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by gomdev on 15. 1. 9..
 */
public class DateLabelInfo implements Serializable, GalleryInfo {
    static final String CLASS = "DateInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final String mDate;

    private int mIndex = 0;

    private int mNumOfRows = 0;

    private int mImageIndex = 0;

    private LinkedList<ImageInfo> mImageInfos = new LinkedList<>();

    public DateLabelInfo(String date) {
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
        imageInfo.setIndex(mImageIndex++);
    }

    public ImageInfo get(int position) {
        return mImageInfos.get(position);
    }

    public ImageInfo getFirst() {
        return mImageInfos.getFirst();
    }

    public ImageInfo getLast() {
        return mImageInfos.getLast();
    }

    public int getNumOfImages() {
        return mImageInfos.size();
    }

    public void setNumOfColumns(int numOfColumns) {
        mNumOfRows = (int) Math.ceil((double) mImageInfos.size() / numOfColumns);
    }

    public int getNumOfRows() {
        return mNumOfRows;
    }

    public void deleteImageInfo(int index) {
        mImageInfos.remove(index);

        int size = mImageInfos.size();
        for (int i = index; i < size; i++) {
            mImageInfos.get(i).setIndex(i);
        }
    }
}
