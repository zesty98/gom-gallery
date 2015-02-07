package com.gomdev.gallery;

import android.content.Context;
import android.provider.MediaStore;
import android.widget.Toast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by gomdev on 15. 1. 22..
 */
class ImageManager {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static ImageManager sImageManager;

    static ImageManager newInstance(Context context) {
        sImageManager = new ImageManager(context);
        return sImageManager;
    }

    static ImageManager getInstance() {
        return sImageManager;
    }

    private final Context mContext;

    private int mNumOfImages = 0;
    private List<BucketInfo> mBucketInfos = null;
    private BucketInfo mCurrentBucketInfo = null;
    private ObjectManager mObjectManager = null;

    private int mBucketIndex = 0;

    private ImageManager(Context context) {
        mContext = context;

        mBucketInfos = new LinkedList<>();
        mNumOfImages = 0;
    }

    void setObjectManager(ObjectManager objectManager) {
        mObjectManager = objectManager;
    }

    void addBucketInfo(BucketInfo bucketInfo) {
        mBucketInfos.add(bucketInfo);
        bucketInfo.setIndex(mBucketIndex++);
    }

    BucketInfo getBucketInfo(int index) {
        return mBucketInfos.get(index);
    }

    void setCurrentBucketInfo(int index) {
        mCurrentBucketInfo = mBucketInfos.get(index);
    }

    BucketInfo getCurrentBucketInfo() {
        return mCurrentBucketInfo;
    }

    int getNumOfBucketInfos() {
        return mBucketInfos.size();
    }

    void setNumOfImages(int numOfImages) {
        mNumOfImages = numOfImages;
    }

    int getNumOfImages() {
        return mNumOfImages;
    }

    int getImageIndex(ImageIndexingInfo imageIndexingInfo) {
        int index = 0;

        BucketInfo bucketInfo = mBucketInfos.get(imageIndexingInfo.mBucketIndex);

        int dateLabelIndex = imageIndexingInfo.mDateLabelIndex;
        int imageInfoIndex = imageIndexingInfo.mImageIndex;

        for (int i = 0; i < dateLabelIndex; i++) {
            index += bucketInfo.get(i).getNumOfImages();
        }

        index += imageInfoIndex;

        return index;
    }

    ImageIndexingInfo getImageIndexingInfo(int index) {
        int bucketIndex = mCurrentBucketInfo.getIndex();
        int dateLabelIndex = 0;
        int imageIndex = 0;

        Iterator<DateLabelInfo> iter = mCurrentBucketInfo.getIterator();
        while (iter.hasNext()) {
            DateLabelInfo dateLabelInfo = iter.next();
            int maxIndexInDateLabel = dateLabelInfo.getNumOfImages() - 1;
            if (maxIndexInDateLabel < index) {
                index -= (maxIndexInDateLabel + 1);
                dateLabelIndex++;
            } else {
                break;
            }
        }

        imageIndex = index;

        return new ImageIndexingInfo(bucketIndex, dateLabelIndex, imageIndex);
    }

    ImageInfo getImageInfo(ImageIndexingInfo indexingInfo) {
        DateLabelInfo dateLabelInfo = mCurrentBucketInfo.get(indexingInfo.mDateLabelIndex);
        ImageInfo imageInfo = dateLabelInfo.get(indexingInfo.mImageIndex);

        return imageInfo;
    }

    boolean deleteImage(ImageIndexingInfo imageIndexingInfo) {
        boolean isBucketDeleted = false;

        DateLabelInfo dateLabelInfo = mCurrentBucketInfo.get(imageIndexingInfo.mDateLabelIndex);
        ImageInfo imageInfo = dateLabelInfo.get(imageIndexingInfo.mImageIndex);

        int num = mContext.getContentResolver().delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.Media._ID + " = ? ",
                new String[]{String.valueOf(imageInfo.getImageID())
                });
        if (num == 0) {
            Toast.makeText(mContext, "Delete Failed : Image",
                    Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        dateLabelInfo.deleteImageInfo(imageIndexingInfo.mImageIndex);

        mObjectManager.deleteImage(imageIndexingInfo);

        if (dateLabelInfo.getNumOfImages() == 0) {
            mCurrentBucketInfo.deleteDateLabel(imageIndexingInfo.mDateLabelIndex);
            mObjectManager.deleteDateLabel(imageIndexingInfo.mDateLabelIndex);

            if (mCurrentBucketInfo.getNumOfDateInfos() == 0) {
                delete(imageIndexingInfo.mBucketIndex);
                mCurrentBucketInfo = mBucketInfos.get(0);

                isBucketDeleted = true;
            }
        }

        mNumOfImages--;

        return isBucketDeleted;
    }

    private void delete(int index) {
        mBucketInfos.remove(index);

        int size = mBucketInfos.size();
        for (int i = index; i < size; i++) {
            mBucketInfos.get(i).setIndex(i);
        }
    }
}
