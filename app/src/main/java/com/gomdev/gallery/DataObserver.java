package com.gomdev.gallery;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gomdev on 15. 3. 29..
 */
class DataObserver extends ContentObserver {
    private static final String CLASS = "DataObserver";
    private static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryConfig.DEBUG;

    private static DataObserver sContentObserver = new DataObserver(new Handler());
    private static AtomicInteger sReferenceCount = new AtomicInteger(0);

    public DataObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(TAG, "onChange() selfChange=" + selfChange + " uri=" + uri);
    }

    static void registerContentObserver(Context context) {
        if (DEBUG) {
            Log.d(TAG, "registerContentObserver()");
        }


//        int count = sReferenceCount.getAndIncrement();
//        if (count == 0) {
//            if (DEBUG) {
//                Log.d(TAG, "registerContentObserver() >>> registerContentObserver");
//            }
//
//            context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, sContentObserver);
//        }
    }

    static void unregisterContentObserver(Context context) {
        if (DEBUG) {
            Log.d(TAG, "unregisterContentObserver()");
        }

//        int count = sReferenceCount.decrementAndGet();
//
//        if (count == 0) {
//            if (DEBUG) {
//                Log.d(TAG, "unregisterContentObserver() >>> unregisterContentObserver");
//            }
//
//            context.getContentResolver().unregisterContentObserver(sContentObserver);
//        }
    }

}

