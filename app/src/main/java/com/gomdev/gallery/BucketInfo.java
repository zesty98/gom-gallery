package com.gomdev.gallery;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class BucketInfo implements Serializable {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mID;

    private int mIndex = 0;
    private String mName;

    private List<ImageInfo> mImageInfos = new LinkedList<>();
    private List<DateLabelInfo> mDateLabelInfos = new LinkedList<>();

    public BucketInfo(int index, int id) {
        mIndex = index;
        mID = id;
        mImageInfos.clear();
        mDateLabelInfos.clear();
    }

    public void setIndex(int index) {
        mIndex = index;
    }

    public int getIndex() {
        return mIndex;
    }

    public int getID() {
        return mID;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public void add(ImageInfo imageInfo) {
        mImageInfos.add(imageInfo);
    }

    public ImageInfo getImageInfo(int position) {
        return mImageInfos.get(position);
    }

    public int getNumOfImages() {
        return mImageInfos.size();
    }

    public void add(DateLabelInfo dateLabelInfo) {
        mDateLabelInfos.add(dateLabelInfo);
    }

    public DateLabelInfo getDateLabelInfo(int position) {
        return mDateLabelInfos.get(position);
    }

    public int getNumOfDateInfos() {
        return mDateLabelInfos.size();
    }

    public void deleteImageInfo(int index) {
        DateLabelInfo parentDateLabelInfo = mImageInfos.get(index).getDateLabelInfo();

        mImageInfos.remove(index);

        int size = mImageInfos.size();
        for (int i = index; i < size; i++) {
            mImageInfos.get(i).setIndex(i);
        }

        int firstImageInfoIndex = parentDateLabelInfo.getFirstImageInfoIndex();
        int lastImageInfoIndex = parentDateLabelInfo.getLastImageInfoIndex();

        if (index == firstImageInfoIndex) {
            parentDateLabelInfo.setFirstImageInfoIndex(firstImageInfoIndex + 1);
        }

        if (index == lastImageInfoIndex) {
            parentDateLabelInfo.setLastImageInfoIndex(lastImageInfoIndex - 1);
        }

        int parentDateLabelInfoIndex = parentDateLabelInfo.getIndex();
        int dateLabelInfoSize = mDateLabelInfos.size();
        for (int i = parentDateLabelInfoIndex + 1; i < dateLabelInfoSize; i++) {
            DateLabelInfo dateLabelInfo = mDateLabelInfos.get(i);
            dateLabelInfo.setFirstImageInfoIndex(dateLabelInfo.getFirstImageInfoIndex() - 1);
            dateLabelInfo.setLastImageInfoIndex(dateLabelInfo.getLastImageInfoIndex() - 1);
        }
    }

    public void deleteDateLabel(int index) {
        mDateLabelInfos.remove(index);

        int size = mDateLabelInfos.size();
        for (int i = index; i < size; i++) {
            mDateLabelInfos.get(i).setIndex(i);
        }
    }
}
