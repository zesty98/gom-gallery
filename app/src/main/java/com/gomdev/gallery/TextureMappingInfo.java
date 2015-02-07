package com.gomdev.gallery;

/**
 * Created by gomdev on 14. 12. 23..
 */
class TextureMappingInfo {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GalleryObject mObject;
    private GalleryTexture mTexture;
    private GalleryInfo mGalleryInfo;


    TextureMappingInfo(GalleryObject object, GalleryInfo galleryInfo) {
        mObject = object;
        mGalleryInfo = galleryInfo;
    }

    GalleryObject getObject() {
        return mObject;
    }

    GalleryTexture getTexture() {
        return mTexture;
    }

    GalleryInfo getGalleryInfo() {
        return mGalleryInfo;
    }

    void set(GalleryTexture texture) {
        mTexture = texture;
    }
}
