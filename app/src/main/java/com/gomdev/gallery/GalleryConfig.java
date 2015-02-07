package com.gomdev.gallery;

public class GalleryConfig {
    static final boolean DEBUG = false;
    static final String TAG = "gomdev";

    private GalleryConfig() {

    }

    static final String BUCKET_INDEX = "bucket_index";
    static final String DATE_LABEL_INDEX = "date_label_index";
    static final String IMAGE_INDEX = "image_index";
    static final String IMAGE_VIEW_DATA = "image_view_data";

    static final String PREF_NAME = "gom_gallery_pref_name";
    static final String PREF_NUM_OF_COLUMNS = "num_of_columns";
    static final String PREF_MIN_NUM_OF_COLUMNS = "min_num_of_columns";
    static final String PREF_MAX_NUM_OF_COLUMNS = "max_num_of_columns";
    static final String PREF_COLUMNS_WIDTH = "columns_width";

    static final String PREF_BUCKET_INDEX = "bucket_index";
    static final String PREF_DATE_LABEL_INDEX = "date_label_index";
    static final String PREF_IMAGE_INDEX = "image_index";

    static final int DEFAULT_NUM_OF_COLUMNS = 3;

    static final float SCROLLBAR_MIN_HEIGHT_IN_DP = 50f;

    static final long IMAGE_ANIMATION_START_OFFSET = 0L;
    static final long IMAGE_ANIMATION_END_OFFSET = 300L;
    static final long DATE_LABEL_ANIMATION_START_OFFSET = IMAGE_ANIMATION_END_OFFSET;
    static final long DATE_LABEL_ANIMATION_END_OFFSET = IMAGE_ANIMATION_END_OFFSET + 200L;

    static final long SCROLLBAR_ANIMATION_DURATION = 300L;
}
