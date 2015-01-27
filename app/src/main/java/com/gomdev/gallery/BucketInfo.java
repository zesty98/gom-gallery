package com.gomdev.gallery;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class BucketInfo implements Serializable {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mID;

    private int mIndex = 0;
    private String mName;
    private int mDateLableIndex = 0;

    private int mNumOfImages = 0;

    private LinkedList<DateLabelInfo> mDateLabelInfos = new LinkedList<>();

    public BucketInfo(int id) {
        mID = id;
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

    public void add(DateLabelInfo dateLabelInfo) {
        mDateLabelInfos.add(dateLabelInfo);
        dateLabelInfo.setIndex(mDateLableIndex++);
    }

    public DateLabelInfo get(int position) {
        return mDateLabelInfos.get(position);
    }

    public DateLabelInfo getFirst() {
        return mDateLabelInfos.getFirst();
    }

    public DateLabelInfo getLast() {
        return mDateLabelInfos.getLast();
    }

    public Iterator<DateLabelInfo> getIterator() {
        return mDateLabelInfos.iterator();
    }

    public int getNumOfDateInfos() {
        return mDateLabelInfos.size();
    }

    public int getNumOfImages() {
        int numOfImages = 0;

        Iterator<DateLabelInfo> iterator = mDateLabelInfos.iterator();
        DateLabelInfo dateLabelInfo = null;
        while (iterator.hasNext()) {
            dateLabelInfo = iterator.next();
            numOfImages += dateLabelInfo.getNumOfImages();
        }

        return numOfImages;
    }

    public void deleteDateLabel(int index) {
        mDateLabelInfos.remove(index);

        ListIterator<DateLabelInfo> iterator = mDateLabelInfos.listIterator(index);
        DateLabelInfo dateLabelInfo = null;
        while (iterator.hasNext()) {
            dateLabelInfo = iterator.next();
            dateLabelInfo.setIndex(index++);
        }
    }
}
