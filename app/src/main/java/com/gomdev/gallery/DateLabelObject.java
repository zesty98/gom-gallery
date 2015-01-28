package com.gomdev.gallery;

import com.gomdev.gles.GLESNode;

/**
 * Created by gomdev on 15. 1. 26..
 */
class DateLabelObject extends GalleryObject {
    static final String CLASS = "DateLabelObject";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GLESNode mParentNode = null;

    DateLabelObject(String name) {
        super(name);
    }

    void setParentNode(GLESNode parent) {
        mParentNode = parent;
    }

    GLESNode getParentNode() {
        return mParentNode;
    }
}
