package com.gomdev.gallery;

/**
 * Created by gomdev on 15. 1. 26..
 */
class ImageObject extends GalleryObject {
    static final String CLASS = "ImageObject";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private float mScale = 1f;
    private float mPrevScale = 1f;
    private float mNextScale = 1f;
    private float mStartOffsetY = 0f;
    private float mNextStartOffsetY = 0f;

    ImageObject(String name) {
        super(name);
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public float getScale() {
        return mScale;
    }

    public void setPrevScale(float scale) {
        mPrevScale = scale;
    }

    public float getPrevScale() {
        return mPrevScale;
    }

    public void setNextScale(float scale) {
        mNextScale = scale;
    }

    public float getNextScale() {
        return mNextScale;
    }

    public void setStartOffsetY(float startOffsetY) {
        mStartOffsetY = startOffsetY;
    }

    public float getStartOffsetY() {
        return mStartOffsetY;
    }

    public void setNextStartOffsetY(float nextStartOffsetY) {
        mNextStartOffsetY = nextStartOffsetY;
    }

    public float getNextStartOffsetY() {
        return mNextStartOffsetY;
    }
}
