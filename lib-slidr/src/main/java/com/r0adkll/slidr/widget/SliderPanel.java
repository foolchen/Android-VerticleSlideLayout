/*
 * Copyright (c) 2014. 52inc
 * All Rights Reserved.
 */

package com.r0adkll.slidr.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.ViewDragHelper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.r0adkll.slidr.R;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;

/**
 * Project: PilotPass
 * Package: com.ftinc.mariner.pilotpass.widgets
 * Created by drew.heavner on 8/14/14.
 */
public class SliderPanel extends FrameLayout {

  /****************************************************
   * Constants
   */

  private static final int MIN_FLING_VELOCITY = 400; // dips per second

  private static final float MAX_DIM_ALPHA = 0.8f; // 80% black alpha shade

  /****************************************************
   * Variables
   */

  private int mScreenWidth;
  private int mScreenHeight;
  private View mDimView;
  private View mDecorView;
  private ViewDragHelper mDragHelper;
  private OnPanelSlideListener mListener;
  private boolean mIsLocked = false;

  private SlidrConfig mConfig;
  private boolean mCanDragFromEdge;

  /**
   * Constructor
   */
  public SliderPanel(Context context, View decorView) {
    super(context);
    mDecorView = decorView;
    mConfig = new SlidrConfig.Builder().build();
    init();
  }

  public SliderPanel(Context context, View decorView, SlidrConfig config) {
    super(context);
    mDecorView = decorView;
    mConfig = config;
    init();
  }

  /**
   * Set the panel slide listener that gets called based on slider changes
   */
  public void setOnPanelSlideListener(OnPanelSlideListener listener) {
    mListener = listener;
  }

  /**
   * Initialize the slider panel
   * <p>
   * TODO: Based on SlidrPosition configure the ViewDragHelper to the appropriate position.
   */
  private void init() {
    mScreenWidth = getResources().getDisplayMetrics().widthPixels;

    final float density = getResources().getDisplayMetrics().density;
    final float minVel = MIN_FLING_VELOCITY * density;

    ViewDragHelper.Callback callback;
    int position;
    switch (mConfig.getPosition()) {
      case LEFT:
        callback = mLeftCallback;
        position = ViewDragHelper.EDGE_LEFT;
        break;
      case RIGHT:
        callback = mRightCallback;
        position = ViewDragHelper.EDGE_RIGHT;
        break;
      case TOP:
        callback = mTopCallback;
        position = ViewDragHelper.EDGE_TOP;
        break;
      case BOTTOM:
        callback = mBottomCallback;
        position = ViewDragHelper.EDGE_BOTTOM;
        break;
      default:
        callback = mLeftCallback;
        position = ViewDragHelper.EDGE_LEFT;
    }

    mDragHelper = ViewDragHelper.create(this, mConfig.getSensitivity(), callback);
    mDragHelper.setMinVelocity(minVel);
    mDragHelper.setEdgeTrackingEnabled(position);

    ViewGroupCompat.setMotionEventSplittingEnabled(this, false);

    // Setup the dimmer view
    mDimView = new View(getContext());
    mDimView.setBackgroundColor(Color.BLACK);
    mDimView.setAlpha(MAX_DIM_ALPHA);

    // Add the dimmer view to the layout
    addView(mDimView);

        /*
         * This is so we can get the height of the view and
         * ignore the system navigation that would be included if we
         * retrieved this value from the DisplayMetrics
         */
    post(new Runnable() {
      @Override public void run() {
        mScreenHeight = getHeight();
      }
    });
  }

  /**********************************************************
   * Touch Methods
   */

  private float mInterceptX, mInterceptY;

  private boolean canDragFromEdge(MotionEvent ev) {
    float x = ev.getX();
    float y = ev.getY();
    if (mConfig.getPosition() == SlidrPosition.LEFT) {
      int width = getWidth();
      float edgeSize = mConfig.getEdgeSize(width);
      return x <= edgeSize;
    } else if (mConfig.getPosition() == SlidrPosition.RIGHT) {
      int width = getWidth();
      float edgeSize = mConfig.getEdgeSize(width);
      return x >= width - edgeSize;
    } else if (mConfig.getPosition() == SlidrPosition.TOP) {
      int height = getHeight();
      float edgeSize = mConfig.getEdgeSize(height);
      return y <= edgeSize;
    } else {
      int height = getHeight();
      float edgeSize = mConfig.getEdgeSize(height);
      return y >= height - edgeSize;
    }
  }

  @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mConfig.isEdge()) {
      mCanDragFromEdge = canDragFromEdge(ev);
      if (!mCanDragFromEdge) {
        // 在滑动事件产生的区域在有效范围外时,则交给下层处理
        return false;
      }
    }

    switch (ev.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        mInterceptX = ev.getX();
        mInterceptY = ev.getY();
        break;
      case MotionEvent.ACTION_MOVE:
        float x = ev.getX();
        float y = ev.getY();
        float dx = Math.abs(x - mInterceptX);
        float dy = Math.abs(y - mInterceptY);
        // 此处在横向滑动<竖向滑动/横向滑动为负值(右划返回)时,则放过触摸事件交给下层处理
        if ((dx < dy) || dx < 0) {
          return false;
        }
        break;
    }

    boolean interceptForDrag = false;
    try {
      // 在页面中快速点击/滑动时,可能会出现数组角标越界,此处暂时使用try-catch处理
      interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);
    } catch (Exception ignore) {
    }
    return interceptForDrag && !mIsLocked;
  }

  @Override public boolean onTouchEvent(@NonNull MotionEvent event) {
    // 在特定情况下(目前未确定是什么情况),onInterceptTouchEvent返回false时,将Event传递到了onTouchEvent中
    if (mIsLocked) {
      return false;
    }

    if (mConfig.isEdge() && !mCanDragFromEdge) {
      return super.onTouchEvent(event);
    }
    try {
      mDragHelper.processTouchEvent(event);
    } catch (Exception e) { //此处会报Caused by: java.lang.ArrayIndexOutOfBoundsException: length=1; index=-1崩溃异常,cache父类Exception
      return false;
    }

    return true;
  }

  @Override public void computeScroll() {
    super.computeScroll();
    if (mDragHelper.continueSettling(true)) {
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  /**
   * Lock this sliding panel to ignore touch inputs.
   */
  public void lock() {
    mDragHelper.cancel();
    mIsLocked = true;
  }

  /**
   * Unlock this sliding panel to listen to touch inputs.
   */
  public void unlock() {
    mDragHelper.cancel();
    mIsLocked = false;
  }

  /**
   * The drag helper callback interface for the Left position
   */
  private ViewDragHelper.Callback mLeftCallback = new ViewDragHelper.Callback() {

    @Override public boolean tryCaptureView(View child, int pointerId) {
      return child.getId() == mDecorView.getId();
    }

    @Override public int clampViewPositionHorizontal(View child, int left, int dx) {
      return clamp(left, 0, mScreenWidth);
    }

    @Override public int getViewHorizontalDragRange(View child) {
      return mScreenWidth;
    }

    @Override public void onViewReleased(View releasedChild, float xvel, float yvel) {
      super.onViewReleased(releasedChild, xvel, yvel);

      final int width = getWidth();
      float offset = width - releasedChild.getLeft();
      int left = xvel < 0 || xvel == 0 && offset > 0.5f ? 0 : mScreenWidth;

      mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());
      invalidate();
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      super.onViewPositionChanged(changedView, left, top, dx, dy);
      float percent = 1f - ((float) left / (float) mScreenWidth);

      if (mListener != null) mListener.onSlideChange(percent);

      // Update the dimmer alpha
      float alpha = percent * MAX_DIM_ALPHA;
      mDimView.setAlpha(alpha);
    }

    @Override public void onViewDragStateChanged(int state) {
      super.onViewDragStateChanged(state);
      if (mListener != null) mListener.onStateChanged(state);
      switch (state) {
        case ViewDragHelper.STATE_IDLE:
          if (mDecorView.getLeft() == 0) {
            // State Open
            if (mListener != null) mListener.onOpened();
          } else {
            // State Closed
            if (mListener != null) mListener.onClosed();
          }
          break;
        case ViewDragHelper.STATE_DRAGGING:

          break;
        case ViewDragHelper.STATE_SETTLING:

          break;
      }
    }
  };

  /**
   * The drag helper callbacks for dragging the slidr attachment from the right of the screen
   */
  private ViewDragHelper.Callback mRightCallback = new ViewDragHelper.Callback() {
    @Override public boolean tryCaptureView(View child, int pointerId) {
      return child.getId() == mDecorView.getId();
    }

    @Override public int clampViewPositionHorizontal(View child, int left, int dx) {
      return clamp(left, -mScreenWidth, 0);
    }

    @Override public int getViewHorizontalDragRange(View child) {
      return mScreenWidth;
    }

    @Override public void onViewReleased(View releasedChild, float xvel, float yvel) {
      super.onViewReleased(releasedChild, xvel, yvel);

      final int width = getWidth();
      float offset = width + releasedChild.getLeft();
      int left = xvel > 0 || xvel == 0 && offset < (mScreenWidth - 0.5f) ? 0 : -mScreenWidth;

      mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());
      invalidate();
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      super.onViewPositionChanged(changedView, left, top, dx, dy);
      float percent = 1f - ((float) Math.abs(left) / (float) mScreenWidth);

      if (mListener != null) mListener.onSlideChange(percent);

      // Update the dimmer alpha
      float alpha = percent * MAX_DIM_ALPHA;
      mDimView.setAlpha(alpha);
    }

    @Override public void onViewDragStateChanged(int state) {
      super.onViewDragStateChanged(state);
      if (mListener != null) mListener.onStateChanged(state);
      switch (state) {
        case ViewDragHelper.STATE_IDLE:
          if (mDecorView.getLeft() == 0) {
            // State Open
            if (mListener != null) mListener.onOpened();
          } else {
            // State Closed
            if (mListener != null) mListener.onClosed();
          }
          break;
        case ViewDragHelper.STATE_DRAGGING:

          break;
        case ViewDragHelper.STATE_SETTLING:

          break;
      }
    }
  };

  /**
   * The drag helper callbacks for dragging the slidr attachment from the top of the screen
   */
  private ViewDragHelper.Callback mTopCallback = new ViewDragHelper.Callback() {
    @Override public boolean tryCaptureView(View child, int pointerId) {
      return child.getId() == mDecorView.getId();
    }

    @Override public int clampViewPositionVertical(View child, int top, int dy) {
      return clamp(top, 0, mScreenHeight);
    }

    @Override public int getViewVerticalDragRange(View child) {
      return mScreenHeight;
    }

    @Override public void onViewReleased(View releasedChild, float xvel, float yvel) {
      super.onViewReleased(releasedChild, xvel, yvel);

      final int height = getHeight();
      float offset = height - releasedChild.getTop();
      int top = yvel < 0 || yvel == 0 && offset > 0.5f ? 0 : mScreenHeight;

      mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
      invalidate();
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      super.onViewPositionChanged(changedView, left, top, dx, dy);
      float percent = 1f - ((float) Math.abs(top) / (float) mScreenHeight);

      if (mListener != null) mListener.onSlideChange(percent);

      // Update the dimmer alpha
      float alpha = percent * MAX_DIM_ALPHA;
      mDimView.setAlpha(alpha);
    }

    @Override public void onViewDragStateChanged(int state) {
      super.onViewDragStateChanged(state);
      if (mListener != null) mListener.onStateChanged(state);
      switch (state) {
        case ViewDragHelper.STATE_IDLE:
          if (mDecorView.getTop() == 0) {
            // State Open
            if (mListener != null) mListener.onOpened();
          } else {
            // State Closed
            if (mListener != null) mListener.onClosed();
          }
          break;
        case ViewDragHelper.STATE_DRAGGING:

          break;
        case ViewDragHelper.STATE_SETTLING:

          break;
      }
    }
  };

  /**
   * The drag helper callbacks for dragging the slidr attachment from the bottom of hte screen
   */
  private ViewDragHelper.Callback mBottomCallback = new ViewDragHelper.Callback() {
    @Override public boolean tryCaptureView(View child, int pointerId) {
      return child.getId() == mDecorView.getId();
    }

    @Override public int clampViewPositionVertical(View child, int top, int dy) {
      return clamp(top, -mScreenHeight, 0);
    }

    @Override public int getViewVerticalDragRange(View child) {
      return mScreenHeight;
    }

    @Override public void onViewReleased(View releasedChild, float xvel, float yvel) {
      super.onViewReleased(releasedChild, xvel, yvel);

      final int height = getHeight();
      float offset = height - releasedChild.getTop();
      int top = yvel > 0 || yvel == 0 && offset < (mScreenHeight - 0.5f) ? 0 : -mScreenHeight;

      mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
      invalidate();
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      super.onViewPositionChanged(changedView, left, top, dx, dy);
      float percent = 1f - ((float) Math.abs(top) / (float) mScreenHeight);

      if (mListener != null) mListener.onSlideChange(percent);

      // Update the dimmer alpha
      float alpha = percent * MAX_DIM_ALPHA;
      mDimView.setAlpha(alpha);
    }

    @Override public void onViewDragStateChanged(int state) {
      super.onViewDragStateChanged(state);
      if (mListener != null) mListener.onStateChanged(state);
      switch (state) {
        case ViewDragHelper.STATE_IDLE:
          if (mDecorView.getTop() == 0) {
            // State Open
            if (mListener != null) mListener.onOpened();
          } else {
            // State Closed
            if (mListener != null) mListener.onClosed();
          }
          break;
        case ViewDragHelper.STATE_DRAGGING:

          break;
        case ViewDragHelper.STATE_SETTLING:

          break;
      }
    }
  };

  /** 手动触发右划返回 */
  public void triggerSlide() {
    View view = findViewById(R.id.slidable_content);
    mDragHelper.smoothSlideViewTo(view, mScreenWidth, 0);
    invalidate();
  }

  /**
   * Clamp Integer values to a given range
   *
   * @param value the value to clamp
   * @param min the minimum value
   * @param max the maximum value
   * @return the clamped value
   */
  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  /**
   * The panel sliding interface that gets called
   * whenever the panel is closed or opened
   */
  public static interface OnPanelSlideListener {
    public void onStateChanged(int state);

    public void onClosed();

    public void onOpened();

    public void onSlideChange(float percent);
  }
}
