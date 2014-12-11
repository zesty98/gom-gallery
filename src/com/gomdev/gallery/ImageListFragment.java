package com.gomdev.gallery;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;

public class ImageListFragment extends Fragment {

    private ImageManager mImageManager;
    private BucketInfo mBucketInfo;

    public ImageListFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container,
                false);

        Activity activity = getActivity();

        mImageManager = ImageManager.getInstance();
        mBucketInfo = GalleryContext.getInstance().getCurrentBucketInfo();

        ImageGridAdapter adapter = new ImageGridAdapter(activity);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(adapter);

        GalleryContext context = GalleryContext.getInstance();
        int columnWidth = context.getGridColumnWidth();

        gridview.setColumnWidth(columnWidth);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                    int position, long id) {
                ImageInfo imageInfo = mBucketInfo.get(position);

                GalleryContext galleryContext = GalleryContext.getInstance();
                galleryContext.setCurrentImageInfo(imageInfo);
                
                Intent intent = new Intent(getActivity(), com.gomdev.gallery.ImageViewActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }

    public class ImageGridAdapter extends BaseAdapter {
        static final String CLASS = "ImageAdapter";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final LayoutInflater mInflater;
        private int mNumOfImages = 0;

        public ImageGridAdapter(Context context) {
            mInflater = LayoutInflater.from(context);

            mNumOfImages = mBucketInfo.getNumOfImageInfos();
        }

        public int getCount() {
            return mNumOfImages;
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
                        R.layout.grid_item_image,
                        parent, false);
            } else {
                layout = (FrameLayout) convertView;

            }
            ImageView imageView = (SquareImageView) layout
                    .findViewById(R.id.image);
            ImageInfo imageInfo = mBucketInfo.get(position);
            mImageManager.loadThumbnail(imageInfo, imageView);

            return layout;
        }
    }
}
