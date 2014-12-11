package com.gomdev.gallery;

public class ImageInfo {
    static final String CLASS = "ImageInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private int mPosition = 0;
    private int mOrientation = 0;
    private long mImageID = -1;
    private String mImagePath = null;
    private String mBucketName = null;

    public ImageInfo(int position, int orientation) {
        mPosition = position;
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

    public void setImageID(long imageID) {
        mImageID = imageID;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String path) {
        mImagePath = path;
    }

    public String getBucketName() {
        return mBucketName;
    }

    public void setBucketName(String name) {
        mBucketName = name;
    }

    @Override
    public String toString() {
        return "position=" + mPosition + "\n" + "id=" + mImageID + "\n" + "bucketName=" + mBucketName + "\n" + "imagePath=" + mImagePath + "\n";
    }
}
