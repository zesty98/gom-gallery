package com.gomdev.gallery;

import java.io.Serializable;

public class ImageInfo implements Serializable, GalleryInfo {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mOrientation;
    private final long mImageID;
    private String mImagePath = null;
    private int mWidth;
    private int mHeight;
    private int mThumbnailWidth;
    private int mThumbnailHeight;

    private DateLabelInfo mDateLabelInfo = null;

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

    public int getThumbnailWidth() {
        return mThumbnailWidth;
    }

    public void setThumbnailWidth(int width) {
        mThumbnailWidth = width;
    }

    public int getThumbnailHeight() {
        return mThumbnailHeight;
    }

    public void setThumbnailHeight(int height) {
        mThumbnailHeight = height;
    }

    @Override
    public String toString() {
        return "id=" + mImageID + "\n" + "imagePath=" + mImagePath + "\n";
    }
}
