package com.gomdev.gallery;

import android.content.Context;

/**
 * Created by gomdev on 15. 2. 24..
 */
public class DetailViewPager {
    static final String CLASS = "DetailViewPager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = true;//GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    DetailViewPager(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;
    }
}
