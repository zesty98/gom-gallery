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
import android.widget.TextView;

import com.gomdev.gles.GLESUtils;

public class BucketListFragment extends Fragment {
    static final String CLASS = "BucketListFragment";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static Bitmap sLoadingBitmap = null;

    static {
        sLoadingBitmap = GLESUtils.makeBitmap(512, 512, Bitmap.Config.ARGB_8888, Color.BLACK);
    }

    private ImageManager mImageManager;
    private int mNumOfColumns = 0;
    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v,
                                int position, long id) {
            Intent intent = new Intent(getActivity(),
                    com.gomdev.gallery.ImageListActivity.class);
            intent.putExtra(GalleryConfig.BUCKET_POSITION, position - mNumOfColumns);
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
    };

    public BucketListFragment() {

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

        Activity activity = getActivity();

        BucketGridAdapter adapter = new BucketGridAdapter(activity);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(adapter);

        GalleryContext context = GalleryContext.getInstance();
        int columnWidth = context.getGridColumnWidth();
        mNumOfColumns = context.getNumOfColumns();

        gridview.setColumnWidth(columnWidth);
        adapter.setItemHeight(columnWidth);

        gridview.setOnItemClickListener(mOnItemClickListener);

        return rootView;
    }

    @Override
    public void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        super.onResume();

        mImageManager.setLoadingBitmap(sLoadingBitmap);
    }

    public class BucketGridAdapter extends BaseAdapter {
        static final String CLASS = "BucketGridAdapter";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final LayoutInflater mInflater;

        private final int mNumOfBuckets;
        private int mItemHeight = 0;
        private FrameLayout.LayoutParams mImageViewLayoutParams;
        private int mActionBarHeight = 0;

        public BucketGridAdapter(Context context) {
            mInflater = LayoutInflater.from(context);

            mNumOfBuckets = mImageManager.getNumOfBuckets();

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
            return mNumOfBuckets + mNumOfColumns;
        }

        @Override
        public Object getItem(int position) {
            return (position < mNumOfColumns) ? null : mImageManager.getBucketInfo(position - mNumOfColumns);
        }

        @Override
        public long getItemId(int position) {
            return (position < mNumOfColumns) ? 0 : (position - mNumOfColumns);
        }

        @Override
        public int getViewTypeCount() {
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

        // create a new ImageView for each item referenced by the Adapter
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
                        R.layout.grid_item_folder,
                        parent, false);
            } else {
                layout = (FrameLayout) convertView;

            }

            ImageView imageView = (RecyclingImageView) layout
                    .findViewById(R.id.image);
            BucketInfo bucketInfo = mImageManager.getBucketInfo(position - mNumOfColumns);
            ImageInfo imageInfo = bucketInfo.get(0);

            imageView.setLayoutParams(mImageViewLayoutParams);

            mImageManager.loadThumbnail(imageInfo, imageView);

            TextView textView = (TextView) layout
                    .findViewById(R.id.bucket_info);
            if (textView != null) {
                textView.setText(bucketInfo.getName() + "\n"
                        + bucketInfo.getNumOfImageInfos());
            }

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
