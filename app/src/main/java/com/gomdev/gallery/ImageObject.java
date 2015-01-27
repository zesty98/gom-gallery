package com.gomdev.gallery;

/**
 * Created by gomdev on 15. 1. 26..
 */
class ImageObject extends GalleryObject {
    static final String CLASS = "ImageObject";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private float mScale = 1f;

    ImageObject(String name) {
        super(name);
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public float getScale() {
        return mScale;
    }
}
