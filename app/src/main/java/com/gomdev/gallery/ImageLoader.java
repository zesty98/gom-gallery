package com.gomdev.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;

import com.gomdev.gallery.GalleryConfig.SortBy;
import com.gomdev.gallery.GalleryTexture.TextureState;

import java.io.FileDescriptor;
import java.util.HashSet;
import java.util.Set;

public class ImageLoader {
    static final String CLASS = "ImageLoader";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private static final long MS_TO_DAY_CONVERT_UNIT = 86400000l; // 24 * 60 * 60 * 1000;

    private static final String BUCKET_GROUP_BY = "1) GROUP BY (1";

    private static final String[] PROJECTION_BUCKET = {
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

    private static final String[] PROJECTION_IMAGE = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
    };

    private static ImageLoader sImageLoader = null;

    public static ImageLoader newInstance(Context context) {
        sImageLoader = new ImageLoader(context);
        return sImageLoader;
    }

    public static ImageLoader getInstance() {
        return sImageLoader;
    }

    private final Context mContext;

    private ImageManager mImageManager = null;
    private ImageCache mImageCache = null;

    private Bitmap mLoadingBitmap = null;
    private String mOrderClause;

    private boolean mIsImageLoaded = false;

    private ImageLoader(Context context) {
        mContext = context;

        init(context);
    }

    private void init(Context context) {
        mImageManager = ImageManager.getInstance();

        ImageCache.ImageCacheParams params = new ImageCache.ImageCacheParams(mContext, DISK_CACHE_SUBDIR);
        params.mCompressFormat = Bitmap.CompressFormat.JPEG;
        params.mCompressQuality = 70;
//        params.setMemCacheSizePercent(0.25f);
        params.setMemCacheSize(15 * 1024);
        params.mDiskCacheEnabled = true;
        params.mMemoryCacheEnabled = true;
        mImageCache = ImageCache.getInstance(((Activity) context).getFragmentManager(), params);

        SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        int sortBy = pref.getInt(GalleryConfig.PREF_SORT_BY, SortBy.DESCENDING.getIndex());

        if (sortBy == SortBy.DESCENDING.getIndex()) {
            mOrderClause = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC, "
                    + MediaStore.Images.ImageColumns._ID + " DESC";
        } else if (sortBy == SortBy.ASCENDING.getIndex()) {
            mOrderClause = MediaStore.Images.ImageColumns.DATE_TAKEN + " ASC, "
                    + MediaStore.Images.ImageColumns._ID + " DESC";
        }

        mIsImageLoaded = false;
    }

    boolean isImageLoaded() {
        return mIsImageLoaded;
    }

    void checkAndLoadImages() {
        if (mIsImageLoaded == false) {
            loadBucketInfos();
            loadImageInfos();
        }
    }

    private void loadBucketInfos() {
        if (DEBUG) {
            Log.d(TAG, "loadBucketInfos()");
        }

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, PROJECTION_BUCKET,
                BUCKET_GROUP_BY, null, mOrderClause);

        int numOfImages = cursor.getCount();
        mImageManager.setNumOfImages(numOfImages);

        Set<Integer> buckets = new HashSet<>();

        if (cursor.moveToFirst() == true) {
            do {
                int columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
                int bucketID = cursor.getInt(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                String bucketName = cursor.getString(columnIndex);

                if (buckets.add(bucketID) == true) {
                    BucketInfo bucketInfo = new BucketInfo(bucketID);
                    bucketInfo.setName(bucketName);
                    mImageManager.addBucketInfo(bucketInfo);

                    if (DEBUG) {
                        Log.d(TAG, "\t bucketName=" + bucketName);
                    }
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    private void loadImageInfos() {
        int size = mImageManager.getNumOfBucketInfos();
        for (int i = 0; i < size; i++) {
            BucketInfo bucketInfo = mImageManager.getBucketInfo(i);
            loadImageInfosFromBucketInfo(bucketInfo);
        }
    }

    void loadImageInfosFromBucketInfo(BucketInfo bucketInfo) {
        if (DEBUG) {
            Log.d(TAG, "loadImageInfosFromBucketInfo() bucketName=" + bucketInfo.getName());
        }

        int bucketID = bucketInfo.getID();

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PROJECTION_IMAGE,
                MediaStore.Images.Media.BUCKET_ID + " = ? ",
                new String[]{String.valueOf(bucketID)
                },
                mOrderClause);

        long prevDateTaken = 0l;
        DateLabelInfo dateLabelInfo = null;
        if (cursor.moveToFirst() == true) {
            do {
                int columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long imageID = cursor.getLong(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String imagePath = cursor.getString(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);
                int orientation = cursor.getInt(columnIndex);

                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                long dateTakenInMs = cursor.getLong(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
                int width = cursor.getInt(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
                int height = cursor.getInt(columnIndex);

                ImageInfo imageInfo = new ImageInfo(imageID, orientation);
                imageInfo.setImagePath(imagePath);

                if (width <= 0 || height <= 0) {
                    setImageSize(imageInfo);
                } else {
                    imageInfo.setWidth(width);
                    imageInfo.setHeight(height);

                    adjustWidthAndHeight(imageInfo);
                }

                int flags = DateUtils.FORMAT_SHOW_YEAR;
                String date = DateUtils.formatDateTime(mContext, dateTakenInMs, flags);

                dateTakenInMs /= MS_TO_DAY_CONVERT_UNIT;

                if (prevDateTaken != dateTakenInMs) {
                    dateLabelInfo = new DateLabelInfo(date);
                    dateLabelInfo.add(imageInfo);
                    bucketInfo.add(dateLabelInfo);

                    if (DEBUG) {
                        Log.d(TAG, "\t dateLabel " + dateLabelInfo.getDate());
                    }
                } else {
                    dateLabelInfo.add(imageInfo);

                    if (DEBUG) {
                        Log.d(TAG, "\t\t imageInfo width=" + imageInfo.getWidth() + " height=" + imageInfo.getHeight() + " path=" + imageInfo.getImagePath());
                    }
                }

                prevDateTaken = dateTakenInMs;
            } while (cursor.moveToNext());
        }

        cursor.close();

        mIsImageLoaded = true;
    }

    private static void setImageSize(ImageInfo imageInfo) {
        String path = imageInfo.getImagePath();

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        imageInfo.setWidth(options.outWidth);
        imageInfo.setHeight(options.outHeight);

        adjustWidthAndHeight(imageInfo);
    }

    private static void adjustWidthAndHeight(ImageInfo imageInfo) {
        int orientation = imageInfo.getOrientation();
        if (orientation == 90 || orientation == 270) {
            int height = imageInfo.getHeight();
            imageInfo.setHeight(imageInfo.getWidth());
            imageInfo.setWidth(height);
        }
    }

    void setLoadingBitmap(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    <T extends BitmapContainer> void loadThumbnail(ImageInfo imageInfo, T container) {
        final String imageKey = String.valueOf(imageInfo.getImageID());
        final BitmapDrawable value = mImageCache.getBitmapFromMemCache(imageKey);

        if (value != null) {
            container.setBitmapDrawable(value);
            if (DEBUG) {
                Log.d(TAG, "Memory cache hit " + imageInfo.getImagePath());
            }
        } else {
            if (BitmapWorker.cancelPotentialWork(imageInfo, container) && container != null) {
                final BitmapLoaderTask<T> task = new BitmapLoaderTask<>(container);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(mContext.getResources(),
                                mLoadingBitmap, task);
                container.setBitmapDrawable(asyncDrawable);

                task.execute(imageInfo);

                if (container instanceof GalleryTexture) {
                    GalleryTexture texture = (GalleryTexture) container;
                    texture.setState(TextureState.DECODING);
                }
            }
        }
    }

    <T extends BitmapContainer> boolean loadThumbnailFromMemCache(ImageInfo imageInfo, T container) {
        final String imageKey = String.valueOf(imageInfo.getImageID());
        final BitmapDrawable value = mImageCache.getBitmapFromMemCache(imageKey);

        if (value != null) {
            container.setBitmapDrawable(value);
            return true;
        }

        return false;
    }

    <T extends BitmapContainer> void loadBitmap(ImageInfo imageInfo, T container,
                                                int requestWidth, int requestHeight) {
        if (BitmapWorker.cancelPotentialWork(imageInfo, container) && container != null) {
            final BitmapLoaderTask<T> task = new BitmapLoaderTask<>(container, requestWidth, requestHeight);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(),
                            mLoadingBitmap, task);
            container.setBitmapDrawable(asyncDrawable);
            task.execute(imageInfo);
        }
    }

    Bitmap getThumbnail(ImageInfo imageInfo,
                        boolean forcePortrait) {
        long imageID = imageInfo.getImageID();

        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                mContext.getContentResolver(), imageID,
                MediaStore.Images.Thumbnails.MINI_KIND, null);

        if (bitmap == null) {
            bitmap = decodeSampledBitmapFromFile(imageInfo, 512, 512, mImageCache);
        }

        if (forcePortrait == true) {
            bitmap = rotate(bitmap, imageInfo.getOrientation());
        }

        return bitmap;
    }

    Bitmap getBitmap(ImageInfo imageInfo, int requestWidth,
                     int requestHeight, boolean forcePortrait) {
        int orientation = imageInfo.getOrientation();
        if (orientation == 90 || orientation == 270) {
            int temp = requestWidth;
            requestWidth = requestHeight;
            requestHeight = temp;
        }

        Bitmap bitmap = decodeSampledBitmap(imageInfo, requestWidth,
                requestHeight);
//        Bitmap bitmap = decodeSampledBitmapFromFile(imageInfo,
//                requestWidth, requestHeight, mImageCache);

        if (forcePortrait == true && orientation != 0) {
            bitmap = rotate(bitmap, orientation);
        }

        return bitmap;
    }

    private static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }

    private static Bitmap decodeSampledBitmapFromFile(ImageInfo imageInfo,
                                                      int reqWidth, int reqHeight,
                                                      ImageCache cache) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        options.inSampleSize = calculateInSampleSize(width, height, reqWidth,
                reqHeight);

        options.outWidth = width;
        options.outHeight = height;

        String path = imageInfo.getImagePath();
        return BitmapFactory.decodeFile(path, options);
    }

    private static Bitmap decodeSampledBitmap(ImageInfo imageInfo, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        options.inSampleSize = calculateInSampleSize(width, height, reqWidth, reqHeight);

        String path = imageInfo.getImagePath();
        return BitmapFactory.decodeFile(path, options);
    }

    static Bitmap decodeSampledBitmapFromDescriptor(ImageInfo imageInfo,
                                                    FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        options.inSampleSize = calculateInSampleSize(width, height, reqWidth, reqHeight);

        return BitmapFactory
                .decodeFileDescriptor(fileDescriptor, null, options);
    }

    static Bitmap decodeSampledBitmapFromDescriptor(
            ImageInfo imageInfo,
            FileDescriptor fileDescriptor,
            int reqWidth, int reqHeight,
            ImageCache cache) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        options.inSampleSize = calculateInSampleSize(width, height, reqWidth,
                reqHeight);

        options.outWidth = width;
        options.outHeight = height;

        return BitmapFactory
                .decodeFileDescriptor(fileDescriptor, null, options);
    }

    private static int calculateInSampleSize(
            int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    class BitmapLoaderTask<T extends BitmapContainer> extends BitmapWorker.BitmapWorkerTask<T> {

        private boolean mNeedThumbnail = true;

        private int mRequestWidth = 0;
        private int mRequestHeight = 0;

        BitmapLoaderTask(T container) {
            super(container);
            mNeedThumbnail = true;
        }

        BitmapLoaderTask(T container, int requestWidth,
                         int requestHeight) {
            super(container);

            mNeedThumbnail = false;
            mRequestWidth = requestWidth;
            mRequestHeight = requestHeight;
        }

        // Decode image in background.
        @Override
        protected BitmapDrawable doInBackground(GalleryInfo... params) {
            mGalleryInfo = params[0];
            ImageInfo imageInfo = (ImageInfo) params[0];

            if (isCancelled() == true) {
                return null;
            }

            ImageLoader mImageLoader = ImageLoader.getInstance();
            Bitmap bitmap;
            BitmapDrawable value;
            if (mNeedThumbnail == true) {

                bitmap = mImageCache.getBitmapFromDiskCache(imageInfo);
                if (bitmap == null) {
                    bitmap = mImageLoader.getThumbnail(imageInfo, true);
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Disk cache hit " + imageInfo.getImagePath());
                    }
                }

                value = new RecyclingBitmapDrawable(mContext.getResources(), bitmap);

                if (bitmap != null) {
                    String imageKey = String.valueOf(imageInfo.getImageID());
                    mImageCache.addBitmapToCache(imageKey, value);
                }
            } else {
                bitmap = mImageLoader.getBitmap(imageInfo, mRequestWidth,
                        mRequestHeight, true);

                value = new BitmapDrawable(mContext.getResources(), bitmap);
            }

            return value;
        }
    }
}
