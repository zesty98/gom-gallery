package com.gomdev.gallery;

/**
 * Created by gomdev on 15. 1. 26..
 */
class DateLabelObject extends GalleryObject {
    static final String CLASS = "DateLabelObject";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GalleryNode mParentNode = null;
    private ImageObjects mImageObjects = null;

    DateLabelObject(String name) {
        super(name);
    }

    void setParentNode(GalleryNode parent) {
        mParentNode = parent;
    }

    GalleryNode getParentNode() {
        return mParentNode;
    }

    void setImageObjects(ImageObjects imageObjects) {
        mImageObjects = imageObjects;
    }

    ImageObjects getImageObjects() {
        return mImageObjects;
    }
}
