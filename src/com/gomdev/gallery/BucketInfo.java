package com.gomdev.gallery;

import java.util.ArrayList;

public class BucketInfo {
    static final String CLASS = "BucketInfo";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int mID;
    private String mPath;
    private String mName;
    private ArrayList<ImageInfo> mImageInfos = new ArrayList<ImageInfo>();

    public BucketInfo(int id) {
        mID = id;
        mImageInfos.clear();
    }

    public int getID() {
        return mID;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
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

    public ArrayList<ImageInfo> getImageInfos() {
        return mImageInfos;
    }
}
