package com.gomdev.gallery;

import java.io.Serializable;

public class ImageInfo implements Serializable, GalleryInfo {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mPosition;
    private final int mOrientation;
    private final long mImageID;
    private String mImagePath = null;
    private int mWidth;
    private int mHeight;
    private int mThumbnailWidth;
    private int mThumbnailHeight;

    public ImageInfo(int position, long id, int orientation) {
        mPosition = position;
        mImageID = id;
        mOrientation = orientation;
    }

    public int getPosition() {
        return mPosition;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public long getImageID() {
        return mImageID;
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
        return "position=" + mPosition + "\n" + "id=" + mImageID + "\n" + "imagePath=" + mImagePath + "\n";
    }
}
