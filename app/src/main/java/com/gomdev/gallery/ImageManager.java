package com.gomdev.gallery;

import java.util.ArrayList;

/**
 * Created by gomdev on 15. 1. 22..
 */
public class ImageManager {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static ImageManager sImageManager;

    public static ImageManager newInstance() {
        sImageManager = new ImageManager();
        return sImageManager;
    }

    public static ImageManager getInstance() {
        return sImageManager;
    }

    private int mNumOfImages = 0;
    private int mNumOfBucketInfos = 0;
    private ArrayList<BucketInfo> mBucketInfos = null;

    private ImageManager() {
        mBucketInfos = new ArrayList<>();
        mNumOfImages = 0;
        mNumOfBucketInfos = 0;
    }

    public void addBucketInfo(BucketInfo bucketInfo) {
        mBucketInfos.add(bucketInfo);
    }

    public BucketInfo getBucketInfo(int index) {
        return mBucketInfos.get(index);
    }

    public int getNumOfBucketInfos() {
        mNumOfBucketInfos = mBucketInfos.size();
        return mNumOfBucketInfos;
    }

    public void setNumOfImages(int numOfImages) {
        mNumOfImages = numOfImages;
    }

    public int getNumOfImages() {
        return mNumOfImages;
    }
}
