package com.gomdev.gallery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BucketInfo implements Serializable {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mID;
    private String mName;
    private List<ImageInfo> mImageInfos = new LinkedList<>();
    private List<DateLabelInfo> mDateInfos = new ArrayList<>();

    public BucketInfo(int id) {
        mID = id;
        mImageInfos.clear();
        mDateInfos.clear();
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
        mDateInfos.add(dateLabelInfo);
    }

    public DateLabelInfo getDateInfo(int position) {
        return mDateInfos.get(position);
    }

    public int getNumOfDateInfos() {
        return mDateInfos.size();
    }

    public int getIndex(DateLabelInfo dateLabelInfo) {
        return mDateInfos.indexOf(dateLabelInfo);
    }

    public int getIndex(ImageInfo imageInfo) {
        return mImageInfos.indexOf(imageInfo);
    }

    public void deleteImageInfo(int index) {
        mImageInfos.remove(index);
    }

    public void deleteDateLabel(DateLabelInfo dateLabelInfo) {
        mDateInfos.remove(dateLabelInfo);
    }
}
