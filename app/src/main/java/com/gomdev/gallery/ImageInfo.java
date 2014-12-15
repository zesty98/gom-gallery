package com.gomdev.gallery;

import java.io.Serializable;

public class ImageInfo implements Serializable {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mPosition;
    private final int mOrientation;
    private final long mImageID;
    private String mImagePath = null;

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

    @Override
    public String toString() {
        return "position=" + mPosition + "\n" + "id=" + mImageID + "\n" + "imagePath=" + mImagePath + "\n";
    }
}
