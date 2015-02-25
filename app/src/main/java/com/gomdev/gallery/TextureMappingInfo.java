package com.gomdev.gallery;

/**
 * Created by gomdev on 14. 12. 23..
 */
class TextureMappingInfo {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final GalleryObject mObject;
    private GalleryTexture mTexture = null;
    private GalleryInfo mGalleryInfo = null;

    TextureMappingInfo(GalleryObject object) {
        mObject = object;
    }

    TextureMappingInfo(GalleryObject object, GalleryInfo galleryInfo) {
        mObject = object;
        mGalleryInfo = galleryInfo;
    }

    GalleryObject getObject() {
        return mObject;
    }

    void setGalleryInfo(GalleryInfo info) {
        mGalleryInfo = info;
    }

    GalleryInfo getGalleryInfo() {
        return mGalleryInfo;
    }

    void setTexture(GalleryTexture texture) {
        mTexture = texture;
    }

    GalleryTexture getTexture() {
        return mTexture;
    }
}
