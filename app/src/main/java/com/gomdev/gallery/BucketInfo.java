package com.gomdev.gallery;

import java.io.Serializable;
import java.util.ArrayList;

class BucketInfo implements Serializable {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mID;

    private int mIndex = 0;
    private String mName;
    private int mDateLableIndex = 0;

    private ArrayList<DateLabelInfo> mDateLabelInfos = new ArrayList<>();

    BucketInfo(int id) {
        mID = id;
        mDateLabelInfos.clear();
    }

    void setIndex(int index) {
        mIndex = index;
    }

    int getIndex() {
        return mIndex;
    }

    int getID() {
        return mID;
    }

    String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }

    void add(DateLabelInfo dateLabelInfo) {
        mDateLabelInfos.add(dateLabelInfo);
        dateLabelInfo.setIndex(mDateLableIndex++);
    }

    DateLabelInfo get(int position) {
        return mDateLabelInfos.get(position);
    }

    DateLabelInfo getFirst() {
        return mDateLabelInfos.get(0);
    }

    DateLabelInfo getLast() {
        int size = mDateLabelInfos.size();
        return mDateLabelInfos.get(size - 1);
    }

    int getNumOfDateInfos() {
        return mDateLabelInfos.size();
    }

    int getNumOfImages() {
        int numOfImages = 0;

        int size = mDateLabelInfos.size();
        for (int i = 0; i < size; i++) {
            DateLabelInfo dateLabelInfo = mDateLabelInfos.get(i);
            numOfImages += dateLabelInfo.getNumOfImages();
        }

        return numOfImages;
    }

    void deleteDateLabel(int index) {
        mDateLabelInfos.remove(index);

        int size = mDateLabelInfos.size();
        for (int i = index; i < size; i++) {
            DateLabelInfo dateLabelInfo = mDateLabelInfos.get(i);
            dateLabelInfo.setIndex(index++);
        }

    }
}
