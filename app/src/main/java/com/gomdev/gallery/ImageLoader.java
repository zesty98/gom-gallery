package com.gomdev.gallery;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.FileDescriptor;
import java.util.HashSet;
import java.util.Set;

public class ImageLoader {
    static final String CLASS = "ImageLoader";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private static final long MS_TO_DAY_CONVERT_UNIT = 86400000l; // 24 * 60 * 60 * 1000;

    private static ImageLoader sImageLoader = null;

    private final Context mContext;

    private ImageManager mImageManager = null;
    private ImageCache mImageCache = null;

    private Bitmap mLoadingBitmap = null;
    private String mOrderClause;

    private ImageLoader(Context context) {
        mContext = context;

        init(context);
    }

    private void init(Context context) {
        mImageManager = ImageManager.getInstance();

        ImageCache.ImageCacheParams params = new ImageCache.ImageCacheParams(mContext, DISK_CACHE_SUBDIR);
        params.mCompressFormat = Bitmap.CompressFormat.JPEG;
        params.mCompressQuality = 70;
        params.setMemCacheSizePercent(0.25f);
        params.mDiskCacheEnabled = true;
        params.mMemoryCacheEnabled = true;
        mImageCache = ImageCache.getInstance(((Activity) context).getFragmentManager(), params);

        mOrderClause = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC, "
                + MediaStore.Images.ImageColumns._ID + " DESC";

        loadFolderInfo();

        int size = mImageManager.getNumOfBucketInfos();
        for (int i = 0; i < size; i++) {
            BucketInfo bucketInfo = mImageManager.getBucketInfo(i);
            loadImageInfoFromBucketInfo(bucketInfo);
        }
    }

    public static ImageLoader newInstance(Context context) {
        sImageLoader = new ImageLoader(context);
        return sImageLoader;
    }

    public static ImageLoader getInstance() {
        return sImageLoader;
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
                null, mOrderClause);

        if (DEBUG) {
            Log.d(TAG, "loadFolderInfo() duration=" + (System.nanoTime() - tick));
        }

        int numOfImages = cursor.getCount();
        mImageManager.setNumOfImages(numOfImages);

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
                    mImageManager.addBucketInfo(bucketInfo);
                    index++;
                }
            } while (cursor.moveToNext());
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
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.DATE_TAKEN
        };

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media.BUCKET_ID + " = ? ",
                new String[]{String.valueOf(bucketID)
                },
                mOrderClause);

        long prevDateTaken = 0l;
        DateLabelInfo dateLabelInfo = null;
        int index = 0;
        int dateIndex = 0;
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

                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                long dateTakenInMs = cursor.getLong(columnIndex);

                ImageInfo imageInfo = new ImageInfo(index, imageID, orientation);
                imageInfo.setImagePath(imagePath);
                imageInfo.setWidth(width);
                imageInfo.setHeight(height);

                if (DEBUG) {
                    Log.d(TAG, index + " : imageID=" + imageID + " imagePath="
                            + imagePath + " orientation=" + orientation);
                }

                int flags = DateUtils.FORMAT_SHOW_YEAR;
                String date = DateUtils.formatDateTime(mContext, dateTakenInMs, flags);

                dateTakenInMs /= MS_TO_DAY_CONVERT_UNIT;

                if (prevDateTaken != dateTakenInMs) {
                    dateLabelInfo = new DateLabelInfo(dateIndex, date);
                    dateLabelInfo.add(imageInfo);
                    dateLabelInfo.setFirstImagePosition(imageInfo.getPosition());
                    bucketInfo.add(dateLabelInfo);

                    dateIndex++;
                } else {
                    dateLabelInfo.add(imageInfo);
                }

                bucketInfo.add(imageInfo);

                dateLabelInfo.setLastImagePosition(imageInfo.getPosition());

                prevDateTaken = dateTakenInMs;

                index++;
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    public void setLoadingBitmap(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    public <T extends BitmapContainer> void loadThumbnail(ImageInfo imageInfo, T container) {
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
            }
        }
    }

    public <T extends BitmapContainer> boolean loadThumbnailFromMemCache(ImageInfo imageInfo, T container) {
        final String imageKey = String.valueOf(imageInfo.getImageID());
        final BitmapDrawable value = mImageCache.getBitmapFromMemCache(imageKey);

        if (value != null) {
            container.setBitmapDrawable(value);
            return true;
        }

        return false;
    }

    public void loadBitmap(ImageInfo imageInfo, RecyclingImageView imageView,
                           int requestWidth, int requestHeight) {
        if (BitmapWorker.cancelPotentialWork(imageInfo, imageView)) {
            final BitmapLoaderTask<RecyclingImageView> task = new BitmapLoaderTask(imageView,
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
            Bitmap inBitmap = ReusableBitmaps.getInstance().getBitmapFromReusableSet(options);

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

    class BitmapLoaderTask<T extends BitmapContainer> extends BitmapWorker.BitmapWorkerTask<T> {

        private boolean mNeedThumbnail = true;

        private int mRequestWidth = 0;
        private int mRequestHeight = 0;

        public BitmapLoaderTask(T container) {
            super(container);
            mNeedThumbnail = true;
        }

        public BitmapLoaderTask(T container, int requestWidth,
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

            ImageLoader mImageLoader = ImageLoader.getInstance();
            Bitmap bitmap;
            BitmapDrawable value;
            if (mNeedThumbnail == true) {
                String imageKey = String.valueOf(imageInfo.getImageID());
                bitmap = mImageCache.getBitmapFromDiskCache(imageKey);
                if (bitmap == null) {
                    bitmap = mImageLoader.getThumbnail(imageInfo, true);
                } else {
                    if (DEBUG) {
                        Log.d(TAG,
                                "Disk cache hit " + imageInfo.getImagePath());
                    }
                }

                value = new RecyclingBitmapDrawable(mContext.getResources(), bitmap, mImageCache);

                mImageCache.addBitmapToCache(imageKey, value);
            } else {
                bitmap = mImageLoader.getBitmap(imageInfo, mRequestWidth,
                        mRequestHeight, true);

                value = new BitmapDrawable(mContext.getResources(), bitmap);
            }

            return value;
        }
    }
}
