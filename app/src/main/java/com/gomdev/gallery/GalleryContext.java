package com.gomdev.gallery;

public class GalleryContext {
    private static GalleryContext sGalleryContext = new GalleryContext();

    private int mWidth;
    private int mHeight;

    private int mGridColumnWidth;

    private BucketInfo mCurrentBucketInfo = null;
    private ImageInfo mCurrentImageInfo = null;

    private int mVersionCode = 100;

    private GalleryContext() {

    }

    public static GalleryContext getInstance() {
        return sGalleryContext;
    }

    public void setScreenSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getGridColumnWidth() {
        return mGridColumnWidth;
    }

    public void setGridColumnWidth(int columnWidth) {
        mGridColumnWidth = columnWidth;
    }

    public void setCurrrentBucketInfo(BucketInfo bucketInfo) {
        mCurrentBucketInfo = bucketInfo;
    }

    public BucketInfo getCurrentBucketInfo() {
        return mCurrentBucketInfo;
    }

    public ImageInfo getCurrentImageInfo() {
        return mCurrentImageInfo;
    }

    public void setCurrentImageInfo(ImageInfo imageInfo) {
        mCurrentImageInfo = imageInfo;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public void setVersionCode(int version) {
        mVersionCode = version;
    }
}
