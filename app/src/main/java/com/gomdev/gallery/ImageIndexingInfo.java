package com.gomdev.gallery;

/**
 * Created by gomdev on 15. 1. 26..
 */
class ImageIndexingInfo {
    static final String CLASS = "ImageIndex";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    int mBucketIndex = 0;
    int mDateLabelIndex = 0;
    int mImageIndex = 0;

    ImageIndexingInfo() {

    }

    ImageIndexingInfo(int bucketIndex, int dateLabelIndex, int imageIndex) {
        mBucketIndex = bucketIndex;
        mDateLabelIndex = dateLabelIndex;
        mImageIndex = imageIndex;
    }

    @Override
    public String toString() {
        return "BucketIndex=" + mBucketIndex + " DateLabelIndex=" + mDateLabelIndex + " ImageIndex=" + mImageIndex;
    }
}
