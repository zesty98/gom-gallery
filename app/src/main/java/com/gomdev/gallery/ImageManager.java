package com.gomdev.gallery;

import java.util.LinkedList;
import java.util.List;

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
    private List<BucketInfo> mBucketInfos = null;
    private BucketInfo mCurrentBucketInfo = null;
    private ObjectManager mObjectManager = null;

    private ImageManager() {
        mBucketInfos = new LinkedList<>();
        mNumOfImages = 0;
    }

    public void setObjectManager(ObjectManager objectManager) {
        mObjectManager = objectManager;
    }

    public void addBucketInfo(BucketInfo bucketInfo) {
        mBucketInfos.add(bucketInfo);
    }

    public BucketInfo getBucketInfo(int index) {
        return mBucketInfos.get(index);
    }

    public void setCurrentBucketInfo(int index) {
        mCurrentBucketInfo = mBucketInfos.get(index);
    }

    public BucketInfo getCurrentBucketInfo() {
        return mCurrentBucketInfo;
    }

    public int getNumOfBucketInfos() {
        return mBucketInfos.size();
    }

    public void setNumOfImages(int numOfImages) {
        mNumOfImages = numOfImages;
    }

    public int getNumOfImages() {
        return mNumOfImages;
    }

    public int getIndex(BucketInfo bucketInfo) {
        return mBucketInfos.indexOf(bucketInfo);
    }

    public void deleteImage(int index) {
        ImageInfo imageInfo = mCurrentBucketInfo.getImageInfo(index);
        DateLabelInfo dateLabelInfo = imageInfo.getDateLabelInfo();

        dateLabelInfo.deleteImageInfo(imageInfo);
        mCurrentBucketInfo.deleteImageInfo(index);

        mObjectManager.deleteImage(index);

        if (dateLabelInfo.getNumOfImages() == 0) {
            int dateLabelIndex = mCurrentBucketInfo.getIndex(dateLabelInfo);
            mCurrentBucketInfo.deleteDateLabel(dateLabelInfo);
            mObjectManager.deleteDateLabel(dateLabelIndex);

            if (mCurrentBucketInfo.getNumOfImages() == 0) {
                mBucketInfos.remove(mCurrentBucketInfo);
                mCurrentBucketInfo = mBucketInfos.get(0);
            }
        }

        mNumOfImages--;
    }
}
