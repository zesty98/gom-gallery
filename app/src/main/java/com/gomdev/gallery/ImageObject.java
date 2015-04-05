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
    private float mAlpha = 1f;

    private long mAnimationStartTime = 0L;
    private boolean mIsOnAlphaAnimation = false;

    private boolean mPrevVisibility = false;

    ImageObject(String name) {
        super(name);
    }

    void setScale(float scale) {
        mScale = scale;
    }

    float getScale() {
        return mScale;
    }

    void setPrevScale(float scale) {
        mPrevScale = scale;
    }

    float getPrevScale() {
        return mPrevScale;
    }

    void setNextScale(float scale) {
        mNextScale = scale;
    }

    float getNextScale() {
        return mNextScale;
    }

    void setStartOffsetY(float startOffsetY) {
        mStartOffsetY = startOffsetY;
    }

    float getStartOffsetY() {
        return mStartOffsetY;
    }

    void setNextStartOffsetY(float nextStartOffsetY) {
        mNextStartOffsetY = nextStartOffsetY;
    }

    float getNextStartOffsetY() {
        return mNextStartOffsetY;
    }

    void setVisibility(boolean visibility) {
        mPrevVisibility = mIsVisible;
        mIsVisible = visibility;
    }

    boolean isVisibilityChanged() {
        return (mIsVisible != mPrevVisibility);
    }

    void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    float getAlpha() {
        return mAlpha;
    }

    void setAnimationStartTime(long startTime) {
        mAnimationStartTime = startTime;
    }

    void setIsOnAlphaAnimation(boolean isOnAlphaAnimation) {
        mIsOnAlphaAnimation = isOnAlphaAnimation;
    }

    boolean isOnAlphaAnimation() {
        return mIsOnAlphaAnimation;
    }

    long getAnimationStartTime() {
        return mAnimationStartTime;
    }
}
