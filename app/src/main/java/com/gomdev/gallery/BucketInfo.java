package com.gomdev.gallery;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

class BucketInfo implements Serializable {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mID;

    private int mIndex = 0;
    private String mName;
    private int mDateLableIndex = 0;

    private int mNumOfImages = 0;

    private LinkedList<DateLabelInfo> mDateLabelInfos = new LinkedList<>();

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
        return mDateLabelInfos.getFirst();
    }

    DateLabelInfo getLast() {
        return mDateLabelInfos.getLast();
    }

    Iterator<DateLabelInfo> getIterator() {
        return mDateLabelInfos.iterator();
    }

    int getNumOfDateInfos() {
        return mDateLabelInfos.size();
    }

    int getNumOfImages() {
        int numOfImages = 0;

        Iterator<DateLabelInfo> iterator = mDateLabelInfos.iterator();
        DateLabelInfo dateLabelInfo = null;
        while (iterator.hasNext()) {
            dateLabelInfo = iterator.next();
            numOfImages += dateLabelInfo.getNumOfImages();
        }

        return numOfImages;
    }

    void deleteDateLabel(int index) {
        mDateLabelInfos.remove(index);

        ListIterator<DateLabelInfo> iterator = mDateLabelInfos.listIterator(index);
        DateLabelInfo dateLabelInfo = null;
        while (iterator.hasNext()) {
            dateLabelInfo = iterator.next();
            dateLabelInfo.setIndex(index++);
        }
    }
}
