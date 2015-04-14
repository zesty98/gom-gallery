package com.gomdev.gallery;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.lang.ref.WeakReference;

class ImageInfo implements Serializable, GalleryInfo {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mOrientation;
    private final long mImageID;

    private int mIndex = 0;
    private String mImagePath = null;
    private int mWidth;
    private int mHeight;

    private WeakReference<Bitmap> mBitmapRef = null;

    ImageInfo(long id, int orientation) {
        mImageID = id;
        mOrientation = orientation;
    }

    int getOrientation() {
        return mOrientation;
    }

    long getImageID() {
        return mImageID;
    }

    void setIndex(int index) {
        mIndex = index;
    }

    int getIndex() {
        return mIndex;
    }

    String getImagePath() {
        return mImagePath;
    }

    void setImagePath(String path) {
        mImagePath = path;
    }

    int getWidth() {
        return mWidth;
    }

    void setWidth(int width) {
        mWidth = width;
    }

    int getHeight() {
        return mHeight;
    }

    void setHeight(int height) {
        mHeight = height;
    }

    void setBitmap(Bitmap bitmap) {
        mBitmapRef = new WeakReference<Bitmap>(bitmap);
    }

    Bitmap getBitmap() {
        if (mBitmapRef != null) {
            return mBitmapRef.get();
        }

        return null;
    }

    @Override
    public String toString() {
        return "ImageInfo id=" + mImageID + "\n" + "imagePath=" + mImagePath + "\n";
    }
}
