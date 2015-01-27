package com.gomdev.gallery;

import java.io.Serializable;

public class ImageInfo implements Serializable, GalleryInfo {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mOrientation;
    private final long mImageID;

    private int mIndex = 0;
    private String mImagePath = null;
    private int mWidth;
    private int mHeight;

    public ImageInfo(long id, int orientation) {
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
