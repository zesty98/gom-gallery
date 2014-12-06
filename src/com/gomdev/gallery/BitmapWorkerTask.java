package com.gomdev.gallery;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class BitmapWorkerTask extends AsyncTask<ImageInfo, Void, Bitmap> {

    private final WeakReference<ImageView> mImageViewReference;
    private ImageInfo mImageInfo = null;
    private boolean mNeedThumbnail = true;

    private int mRequestWidth = 0;
    private int mRequestHeight = 0;

    public BitmapWorkerTask(ImageView imageView) {
        mNeedThumbnail = true;
        mImageViewReference = new WeakReference<ImageView>(imageView);
    }

    public BitmapWorkerTask(ImageView imageView, int requestWidth,
            int requestHeight) {
        mNeedThumbnail = false;
        mRequestWidth = requestWidth;
        mRequestHeight = requestHeight;
        mImageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(ImageInfo... params) {
        mImageInfo = params[0];

        ImageManager mImageManager = ImageManager.getInstance();
        Bitmap bitmap = null;
        if (mNeedThumbnail == true) {
            bitmap = mImageManager.getThumbnail(mImageInfo, true);

            mImageManager.addBitmapToMemoryCache(
                    String.valueOf(mImageInfo.getImageID()), bitmap);
        } else {
            bitmap = mImageManager.getBitmap(mImageInfo, mRequestWidth,
                    mRequestHeight, true);
        }

        return bitmap;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (mImageViewReference != null && bitmap != null) {
            final ImageView imageView = mImageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask =
                    ImageManager.getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public ImageInfo getData() {
        return mImageInfo;
    }
}
