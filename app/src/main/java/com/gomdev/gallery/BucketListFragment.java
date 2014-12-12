package com.gomdev.gallery;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class BucketListFragment extends Fragment {
    static final String CLASS = "BucketListFragment";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v,
                                int position, long id) {
            ImageManager imageManager = ImageManager.getInstance();
            BucketInfo bucketInfo = imageManager.getBucketInfo(position);

            GalleryContext galleryContext = GalleryContext.getInstance();
            galleryContext.setCurrrentBucketInfo(bucketInfo);

            Intent intent = new Intent(getActivity(),
                    com.gomdev.gallery.ImageListActivity.class);
            startActivity(intent);
        }
    };

    public BucketListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        View rootView = inflater.inflate(R.layout.fragment_main, container,
                false);

        Activity activity = getActivity();

        BucketGridAdapter adapter = new BucketGridAdapter(activity);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(adapter);

        GalleryContext context = GalleryContext.getInstance();
        int columnWidth = context.getGridColumnWidth();

        gridview.setColumnWidth(columnWidth);

        gridview.setOnItemClickListener(mOnItemClickListener);

        return rootView;
    }

    public class BucketGridAdapter extends BaseAdapter {
        static final String CLASS = "BucketGridAdapter";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final LayoutInflater mInflater;
        private final ImageManager mImageManager;
        private final int mNumOfBuckets;

        public BucketGridAdapter(Context context) {
            mInflater = LayoutInflater.from(context);

            mImageManager = ImageManager.getInstance();
            mNumOfBuckets = mImageManager.getNumOfBucksets();
        }

        public int getCount() {
            return mNumOfBuckets;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout layout;

            if (convertView == null) {
                layout = (FrameLayout) mInflater.inflate(
                        R.layout.grid_item_folder,
                        parent, false);
            } else {
                layout = (FrameLayout) convertView;

            }
            ImageView imageView = (RecyclingImageView) layout
                    .findViewById(R.id.image);
            BucketInfo bucketInfo = mImageManager.getBucketInfo(position);
            ImageInfo imageInfo = bucketInfo.get(0);

            mImageManager.loadThumbnail(imageInfo, imageView);

            TextView textView = (TextView) layout
                    .findViewById(R.id.bucket_info);
            if (textView != null) {
                textView.setText(bucketInfo.getName() + "\n"
                        + bucketInfo.getNumOfImageInfos());
            }

            return layout;
        }
    }
}
