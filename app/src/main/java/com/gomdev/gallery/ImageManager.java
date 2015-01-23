package com.gomdev.gallery;

import android.content.Context;
import android.provider.MediaStore;
import android.widget.Toast;

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

    public static ImageManager newInstance(Context context) {
        sImageManager = new ImageManager(context);
        return sImageManager;
    }

    public static ImageManager getInstance() {
        return sImageManager;
    }

    private final Context mContext;

    private int mNumOfImages = 0;
    private List<BucketInfo> mBucketInfos = null;
    private BucketInfo mCurrentBucketInfo = null;
    private ObjectManager mObjectManager = null;

    private ImageManager(Context context) {
        mContext = context;

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

    public boolean deleteImage(int index) {
        boolean isBucketDeleted = false;
        ImageInfo imageInfo = mCurrentBucketInfo.getImageInfo(index);

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

        DateLabelInfo dateLabelInfo = imageInfo.getDateLabelInfo();

        int indexInDateLabelInfo = imageInfo.getIndexInDateLabelInfo();
        dateLabelInfo.deleteImageInfo(indexInDateLabelInfo);
        mCurrentBucketInfo.deleteImageInfo(index);

        mObjectManager.deleteImage(index);

        if (dateLabelInfo.getNumOfImages() == 0) {
            int dateLabelIndex = dateLabelInfo.getIndex();
            mCurrentBucketInfo.deleteDateLabel(dateLabelIndex);
            mObjectManager.deleteDateLabel(dateLabelIndex);

            if (mCurrentBucketInfo.getNumOfImages() == 0) {
//                num = mContext.getContentResolver().delete(
//                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                        MediaStore.Images.Media.BUCKET_ID + " = ? ",
//                        new String[]{String.valueOf(mCurrentBucketInfo.getID())
//                        });
//                if (num == 0) {
//                    Toast.makeText(mContext, "Delete Failed : Bucket",
//                            Toast.LENGTH_SHORT)
//                            .show();
//                    return false;
//                }

                int bucketIndex = mCurrentBucketInfo.getIndex();
                delete(bucketIndex);
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
