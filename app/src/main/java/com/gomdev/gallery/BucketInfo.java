package com.gomdev.gallery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BucketInfo implements Serializable {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mPosition;
    private final int mID;
    private String mName;
    private List<ImageInfo> mImageInfos = new ArrayList<>();
    private List<DateLabelInfo> mDateInfos = new ArrayList<>();

    public BucketInfo(int position, int id) {
        mPosition = position;
        mID = id;
        mImageInfos.clear();
        mDateInfos.clear();
    }

    public int getPosition() {
        return mPosition;
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

    public ImageInfo get(int position) {
        return mImageInfos.get(position);
    }

    public int getNumOfImages() {
        return mImageInfos.size();
    }

    public void add(DateLabelInfo dateLabelInfo) {
        mDateInfos.add(dateLabelInfo);
        dateLabelInfo.setBucketInfo(this);
    }

    public DateLabelInfo getDateInfo(int position) {
        return mDateInfos.get(position);
    }

    public int getNumOfDateInfos() {
        return mDateInfos.size();
    }
}
