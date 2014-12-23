package com.gomdev.gallery;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Created by gomdev on 14. 12. 23..
 */
public class TextureMappingInfo {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private ReferenceQueue<GalleryTexture> mReferenceQueue;
    private GalleryObject mObject;
    private WeakReference<GalleryTexture> mTextureReference;
    private ImageInfo mImageInfo;


    public TextureMappingInfo(GalleryObject object, ImageInfo imageInfo) {
        mObject = object;
        mImageInfo = imageInfo;
    }

    public GalleryObject getObject() {
        return mObject;
    }

    public GalleryTexture getTexture() {
        if (mTextureReference == null) {
            return null;
        }

        return mTextureReference.get();
    }

    public ImageInfo getImageInfo() {
        return mImageInfo;
    }

    public void set(GalleryTexture texture) {
        if (texture == null) {
            if (mTextureReference != null) {
                GalleryTexture tempTexture = mTextureReference.get();
                if (tempTexture != null) {
                    tempTexture.destroy();
                }
            }
            mTextureReference = null;
        } else {
            if (mReferenceQueue != null) {
                mTextureReference = new WeakReference<GalleryTexture>(texture, mReferenceQueue);
            } else {
                mTextureReference = new WeakReference<GalleryTexture>(texture);
            }
        }
    }

    public void setReferenceQueue(ReferenceQueue<GalleryTexture> referenceQueue) {
        mReferenceQueue = referenceQueue;
    }
}
