package com.gomdev.gallery;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.gomdev.gles.GLESUtils;

public class ImageListFragment extends Fragment {
    static final String CLASS = "BucketListFragment";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static Bitmap sLoadingBitmap = null;

    static {
        sLoadingBitmap = GLESUtils.makeBitmap(512, 512, Bitmap.Config.ARGB_8888, Color.BLACK);
    }

    private ImageLoader mImageLoader = null;
    private ImageManager mImageManager = null;
    private BucketInfo mBucketInfo;
    private int mNumOfColumns = 0;

    public ImageListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreateView()");
        }

        View rootView = inflater.inflate(R.layout.fragment_main, container,
                false);

        mImageManager = ImageManager.getInstance();
        mImageLoader = ImageLoader.getInstance();

        Activity activity = getActivity();

        int bucketPosition = getActivity().getIntent().getIntExtra(GalleryConfig.BUCKET_POSITION, 0);
        mBucketInfo = mImageManager.getBucketInfo(bucketPosition);

        ImageGridAdapter adapter = new ImageGridAdapter(activity);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(adapter);

        GalleryContext context = GalleryContext.getInstance();
        int columnWidth = context.getColumnWidth();
        mNumOfColumns = context.getNumOfColumns();

        gridview.setColumnWidth(columnWidth);
        adapter.setItemHeight(columnWidth);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Intent intent = new Intent(getActivity(), com.gomdev.gallery.ImageViewActivity.class);

                intent.putExtra(GalleryConfig.BUCKET_POSITION, mBucketInfo.getIndex());
                intent.putExtra(GalleryConfig.IMAGE_POSITION, position - mNumOfColumns);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
                    // show plus the thumbnail image in GridView is cropped. so using
                    // makeScaleUpAnimation() instead.
                    ActivityOptions options =
                            ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
                    getActivity().startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        mImageLoader.setLoadingBitmap(sLoadingBitmap);

        super.onResume();


    }

    public class ImageGridAdapter extends BaseAdapter {
        static final String CLASS = "ImageAdapter";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final LayoutInflater mInflater;
        private int mNumOfImages = 0;
        private int mActionBarHeight = 0;

        private int mItemHeight = 0;
        private FrameLayout.LayoutParams mImageViewLayoutParams;

        public ImageGridAdapter(Context context) {
            mInflater = LayoutInflater.from(context);

            mNumOfImages = mBucketInfo.getNumOfImages();

            mImageViewLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            // Calculate ActionBar height
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.actionBarSize, tv, true)) {
                mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                        tv.data, context.getResources().getDisplayMetrics());
            }
        }

        @Override
        public int getCount() {
            return mNumOfImages + mNumOfColumns;
        }

        @Override
        public Object getItem(int position) {
            return position < mNumOfColumns ?
                    null : mBucketInfo.getImageInfo(position - mNumOfColumns);
        }

        @Override
        public long getItemId(int position) {
            return position < mNumOfColumns ? 0 : position - mNumOfColumns;
        }


        @Override
        public int getViewTypeCount() {
            // Two types of views, the normal ImageView and the top row of empty views
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumOfColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout layout;

            if (position < mNumOfColumns) {
                if (convertView == null) {
                    convertView = new ImageView(getActivity());
                }
                // Set empty view with height of ActionBar
                convertView.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, mActionBarHeight));
                return convertView;
            }

            if (convertView == null) {
                layout = (FrameLayout) mInflater.inflate(
                        R.layout.grid_item_image,
                        parent, false);
            } else {
                layout = (FrameLayout) convertView;

            }

            // Check the height matches our calculated column width
            if (layout.getLayoutParams().height != mItemHeight) {
                layout.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mItemHeight));
            }

            RecyclingImageView imageView = (RecyclingImageView) layout
                    .findViewById(R.id.image);
            imageView.setLayoutParams(mImageViewLayoutParams);
            ImageInfo imageInfo = mBucketInfo.getImageInfo(position - mNumOfColumns);
            mImageLoader.loadThumbnail(imageInfo, imageView);

            return layout;
        }

        public void setItemHeight(int height) {
            if (height == mItemHeight) {
                return;
            }

            mItemHeight = height;
            mImageViewLayoutParams =
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mItemHeight);
            notifyDataSetChanged();

        }
    }
}
