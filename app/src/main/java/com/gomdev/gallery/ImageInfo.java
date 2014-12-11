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
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("position=").append(mPosition).append("\n");
        sbuf.append("id=").append(mImageID).append("\n");
        sbuf.append("bucketName=").append(mBucketName).append("\n");
        sbuf.append("imagePath=").append(mImagePath).append("\n");
        return sbuf.toString();
    }
}
