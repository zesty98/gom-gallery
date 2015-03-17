package com.gomdev.gallery;

import android.view.MotionEvent;

/**
 * Created by gomdev on 15. 2. 21..
 */
public interface ViewManager {
    public void update(long currentTime);

    public void updateAnimation(long currentTime);

    public boolean onTouchEvent(MotionEvent event);
}
