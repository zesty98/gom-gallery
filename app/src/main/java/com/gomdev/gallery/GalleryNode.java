package com.gomdev.gallery;

import com.gomdev.gles.GLESNode;

/**
 * Created by gomdev on 15. 2. 1..
 */
class GalleryNode extends GLESNode {
    static final String CLASS = "GalleryNode";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private boolean mPrevVisibility = false;

    GalleryNode() {
        super();
    }

    GalleryNode(String name) {
        super(name);
    }

    void setVisibility(boolean visibility) {
        mPrevVisibility = mIsVisible;
        mIsVisible = visibility;
    }

    boolean isVisibilityChanged() {
        return (mIsVisible != mPrevVisibility);
    }
}
