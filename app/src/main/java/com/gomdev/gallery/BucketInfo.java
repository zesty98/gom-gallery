package com.gomdev.gallery;

import java.util.ArrayList;

public class BucketInfo {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mPosition;
    private final int mID;
    private String mName;
    private ArrayList<ImageInfo> mImageInfos = new ArrayList<>();

    public BucketInfo(int position, int id) {
        mPosition = position;
        mID = id;
        mImageInfos.clear();
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

    public int getNumOfImageInfos() {
        return mImageInfos.size();
    }
}
