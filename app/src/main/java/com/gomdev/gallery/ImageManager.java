package com.gomdev.gallery;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileDescriptor;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ImageManager {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final String DISK_CACHE_SUBDIR = "thumbnails";

    private static ImageManager sImageManager = null;
    private final Context mContext;
    private ImageCache mImageCache = null;
    private int mNumOfImages = 0;
    private int mNumOfBuckets = 0;
    private ArrayList<BucketInfo> mBuckets = new ArrayList<>();
    private Bitmap mLoadingBitmap = null;

    private ImageManager(Context context) {
        mContext = context;

        ImageCache.ImageCacheParams params = new ImageCache.ImageCacheParams(mContext, DISK_CACHE_SUBDIR);
        params.mCompressFormat = Bitmap.CompressFormat.JPEG;
        params.mCompressQuality = 70;
        params.setMemCacheSizePercent(0.25f);
        params.mDiskCacheEnabled = true;
        params.mMemoryCacheEnabled = true;
        mImageCache = ImageCache.getInstance(((Activity) context).getFragmentManager(), params);

        loadFolderInfo();

        for (BucketInfo bucketInfo : mBuckets) {
            loadImageInfoFromBucketInfo(bucketInfo);
        }

        if (DEBUG) {
            Log.d(TAG, "ImageManager() BucketInfos");
            for (BucketInfo bucketInfo : mBuckets) {
                Log.d(TAG,
                        "\t name=" + bucketInfo.getName() + " num="
                                + bucketInfo.getNumOfImageInfos());
            }
        }
    }

    public static void newInstance(Context context) {
        sImageManager = new ImageManager(context);
    }

    public static ImageManager getInstance() {
        return sImageManager;
    }


    private void loadFolderInfo() {
        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        };

        long tick = System.nanoTime();
        // CursorLoader cursorLoader = new CursorLoader(mContext,
        // MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
        // null, null, null);
        // Cursor cursor = cursorLoader.loadInBackground();

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                null, null);

        if (DEBUG) {
            Log.d(TAG, "loadFolderInfo() duration=" + (System.nanoTime() - tick));
        }

        mNumOfImages = cursor.getCount();

        Set<Integer> buckets = new HashSet<>();

        int index = 0;

        if (cursor.moveToFirst() == true) {
            do {
                int columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
                int bucketID = cursor.getInt(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                String bucketName = cursor.getString(columnIndex);

                if (buckets.add(bucketID) == true) {
                    BucketInfo bucketInfo = new BucketInfo(index, bucketID);
                    bucketInfo.setName(bucketName);
                    mBuckets.add(bucketInfo);
                    index++;
                }
            } while (cursor.moveToNext());

            mNumOfBuckets = mBuckets.size();
        }

        cursor.close();
    }

    private void loadImageInfoFromBucketInfo(BucketInfo bucketInfo) {
        int bucketID = bucketInfo.getID();
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
        };

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media.BUCKET_ID + " = ? ",
                new String[]{String.valueOf(bucketID)
                },
                null);

        int index = 0;
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

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
                int width = cursor.getInt(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
                int height = cursor.getInt(columnIndex);

                ImageInfo imageInfo = new ImageInfo(index, imageID, orientation);
                imageInfo.setImagePath(imagePath);
                imageInfo.setWidth(width);
                imageInfo.setHeight(height);

                bucketInfo.add(imageInfo);

                if (DEBUG) {
                    Log.d(TAG, index + " : imageID=" + imageID + " imagePath="
                            + imagePath + " orientation=" + orientation);
                }

                index++;
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    public void setLoadingBitmap(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    public int getNumOfImages() {
        return mNumOfImages;
    }

    public int getNumOfBuckets() {
        return mNumOfBuckets;
    }

    public BucketInfo getBucketInfo(int index) {
        return mBuckets.get(index);
    }

    public <T extends CacheContainer> void loadThumbnail(ImageInfo imageInfo, T container) {
        final String imageKey = String.valueOf(imageInfo.getImageID());
        final BitmapDrawable value = mImageCache.getBitmapFromMemCache(imageKey);

        if (value != null) {
            container.setBitmapDrawable(value);
            if (DEBUG) {
                Log.d(TAG, "Memory cache hit " + imageInfo.getImagePath());
            }
        } else {

            if (cancelPotentialWork(imageInfo, container)) {
                final BitmapWorkerTask<T> task = new BitmapWorkerTask<>(container);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(mContext.getResources(),
                                mLoadingBitmap, task);
                container.setBitmapDrawable(asyncDrawable);
                task.execute(imageInfo);
            }
        }
    }

    public void loadBitmap(ImageInfo imageInfo, RecyclingImageView imageView,
                           int requestWidth, int requestHeight) {
        if (cancelPotentialWork(imageInfo, imageView)) {
            final BitmapWorkerTask<RecyclingImageView> task = new BitmapWorkerTask(imageView,
                    requestWidth, requestHeight);

            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(),
                            mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
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

    private static <T extends CacheContainer> boolean cancelPotentialWork(ImageInfo imageInfo,
                                                                          T container) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(container);

        if (bitmapWorkerTask != null) {
            final ImageInfo bitmapData = bitmapWorkerTask.getData();
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || bitmapData != imageInfo) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was
        // cancelled
        return true;
    }

    public static <T extends CacheContainer> void cancelWork(T container) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(container);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (DEBUG) {
                final Object bitmapData = bitmapWorkerTask.getData();
                Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
            }
        }
    }

    private static <T extends CacheContainer> BitmapWorkerTask getBitmapWorkerTask(T container) {
        if (container != null) {
            final Drawable drawable = container.getBitmapDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static Bitmap decodeSampledBitmapFromFile(ImageInfo imageInfo,
                                                      int reqWidth, int reqHeight,
                                                      ImageCache cache) {
        String path = imageInfo.getImagePath();

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        addInBitmapOptions(options, cache);

        return BitmapFactory.decodeFile(path, options);
    }

    private static void addInBitmapOptions(BitmapFactory.Options options,
                                           ImageCache cache) {
        // inBitmap only works with mutable bitmaps, so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;

        if (cache != null) {
            // Try to find a bitmap to use for inBitmap.
            Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

            if (inBitmap != null) {
                // If a suitable bitmap has been found, set it as the value of
                // inBitmap.
                options.inBitmap = inBitmap;
            }
        }
    }

    private static Bitmap decodeSampledBitmap(ImageInfo imageInfo,
                                              int reqWidth, int reqHeight) {
        String path = imageInfo.getImagePath();

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(
            FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory
                .decodeFileDescriptor(fileDescriptor, null, options);
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(
            FileDescriptor fileDescriptor,
            int reqWidth, int reqHeight,
            ImageCache cache) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        addInBitmapOptions(options, cache);

        return BitmapFactory
                .decodeFileDescriptor(fileDescriptor, null, options);
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
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

    class BitmapWorkerTask<T extends CacheContainer> extends AsyncTask<ImageInfo, Void, BitmapDrawable> {

        private final WeakReference<T> mReference;
        private ImageInfo mImageInfo = null;
        private boolean mNeedThumbnail = true;

        private int mRequestWidth = 0;
        private int mRequestHeight = 0;

        public BitmapWorkerTask(T container) {
            mNeedThumbnail = true;
            mReference = new WeakReference<>(container);
        }

        public BitmapWorkerTask(T container, int requestWidth,
                                int requestHeight) {
            mNeedThumbnail = false;
            mRequestWidth = requestWidth;
            mRequestHeight = requestHeight;
            mReference = new WeakReference<>(container);
        }

        // Decode image in background.
        @Override
        protected BitmapDrawable doInBackground(ImageInfo... params) {
            mImageInfo = params[0];

            ImageManager mImageManager = ImageManager.getInstance();
            Bitmap bitmap;
            BitmapDrawable value;
            if (mNeedThumbnail == true) {
                String imageKey = String.valueOf(mImageInfo.getImageID());
                bitmap = mImageCache.getBitmapFromDiskCache(imageKey);
                if (bitmap == null) {
                    bitmap = mImageManager.getThumbnail(mImageInfo, true);
                } else {
                    if (DEBUG) {
                        Log.d(TAG,
                                "Disk cache hit " + mImageInfo.getImagePath());
                    }
                }

                value = new RecyclingBitmapDrawable(mContext.getResources(), bitmap, mImageCache);

                mImageCache.addBitmapToCache(imageKey, value);
            } else {
                bitmap = mImageManager.getBitmap(mImageInfo, mRequestWidth,
                        mRequestHeight, true);

                value = new BitmapDrawable(mContext.getResources(), bitmap);
            }


            return value;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(BitmapDrawable value) {
            if (isCancelled()) {
                value = null;
            }

            if (mReference != null && value != null) {
                final T container = mReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        ImageManager.getBitmapWorkerTask(container);
                if (this == bitmapWorkerTask && container != null) {
                    container.setBitmapDrawable(value);
                }
            }
        }

        public ImageInfo getData() {
            return mImageInfo;
        }
    }
}
