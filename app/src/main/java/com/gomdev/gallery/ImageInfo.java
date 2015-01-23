package com.gomdev.gallery;

import java.io.Serializable;

public class ImageInfo implements Serializable, GalleryInfo {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mOrientation;
    private final long mImageID;

    private int mIndex = 0;
    private int mIndexInDateLabelInfo = 0;
    private String mImagePath = null;
    private int mWidth;
    private int mHeight;

    private DateLabelInfo mDateLabelInfo = null;

    public ImageInfo(int index, long id, int orientation) {
        mIndex = index;

        mImageID = id;
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public long getImageID() {
        return mImageID;
    }

    public void setIndex(int index) {
        mIndex = index;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndexInDateLabelInfo(int index) {
        mIndexInDateLabelInfo = index;
    }

    public int getIndexInDateLabelInfo() {
        return mIndexInDateLabelInfo;
    }

    public void setDateLabelInfo(DateLabelInfo dateLabelInfo) {
        mDateLabelInfo = dateLabelInfo;
    }

    public DateLabelInfo getDateLabelInfo() {
        return mDateLabelInfo;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String path) {
        mImagePath = path;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    @Override
    public String toString() {
        return "id=" + mImageID + "\n" + "imagePath=" + mImagePath + "\n";
    }
}
