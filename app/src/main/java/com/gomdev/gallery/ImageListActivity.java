package com.gomdev.gallery;

import android.app.Activity;
import android.os.Bundle;

public class ImageListActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ImageListFragment())
                    .commit();
        }

        init();
    }

    private void init() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        int widthInDP = width * 160 / getResources().getDisplayMetrics().densityDpi;

        int numOfColumns = 3;

        if (widthInDP < 500f) {
            numOfColumns = 3;
        } else if (widthInDP < 600f) {
            numOfColumns = 4;
        } else if (widthInDP < 820f) {
            numOfColumns = 5;
        } else {
            numOfColumns = 6;
        }
        GalleryContext.getInstance().setNumOfColumns(numOfColumns);

        int spacing = getResources().getDimensionPixelSize(
                R.dimen.gridview_spacing);
        int columnWidth = (int) ((width - spacing * (numOfColumns + 1)) / numOfColumns);


        GalleryContext context = GalleryContext.getInstance();
        context.setScreenSize(width, height);
        context.setColumnWidth(columnWidth);
    }
}
