/*
 * Copyright (C) 2016 hejunlin <hejunlin2013@gmail.com>
 * 
 * Github:https://github.com/hejunlin2013/DragVideo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hejunlin.dragvideo;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;


public class DragVideoView extends ViewGroup {
    /**
     * 当前拖动的方向
     */
    public static final int NONE = 1 << 0;
    public static final int HORIZONTAL = 1 << 1;
    public static final int VERTICAL = 1 << 2;

    /**
     * 最终组件滑向的方向
     */
    public static final int SLIDE_RESTORE_ORIGINAL = 1 << 0;
    public static final int SLIDE_TO_LEFT = 1 << 1;
    public static final int SLIDE_TO_RIGHT = 1 << 2;

    /**
     * 播放器最低透明度
     */
    private static final float MIN_ALPHA = 0.2f;

    /**
     * 播放器最终缩小的比例
     */
    private static final float PLAYER_RATIO = 0.5f;

    /**
     * 视频的长宽比
     */
    private static final float VIDEO_RATIO = 16f / 9f;

    /**
     * 当播放器最小化后，其在水平方向的偏移量常量
     */
    private static final float ORIGINAL_MIN_OFFSET = 1f / (1f + PLAYER_RATIO);
    private static final float LEFT_DRAG_DISAPPEAR_OFFSET = (4f - PLAYER_RATIO) / (4f + 4f * PLAYER_RATIO);
    private static final float RIGHT_DRAG_DISAPPEAR_OFFSET = (4f + PLAYER_RATIO) / (4f + 4f * PLAYER_RATIO);

    private static final float MAX_OFFSET_RATIO = (1f - PLAYER_RATIO) / (1f + PLAYER_RATIO);
    private static final String TAG = DragVideoView.class.getSimpleName();

    /**
     * 自定义ViewDragHelper类
     */
    private CustomViewDragHelper mDragHelper;

    /**
     * 本ViewGroup只能包含2个直接子组件
     */
    private View mPlayer;
    private View mDesc;

    /**
     * 第一次调用onMeasure时调用
     */
    private boolean mIsFinishInit = false;

    /**
     * 是否最小化
     */
    private boolean mIsMinimum = true;

    /**
     * 垂直方向的拖动范围
     */
    private int mVerticalRange;

    /**
     * 水平方向的拖动范围
     */
    private int mHorizontalRange;

    /**
     * 即this.getPaddingTop()
     */
    private int mMinTop;

    /**
     * 播放器的top
     */
    private int mTop;

    /**
     * 播放器的left
     */
    private int mLeft;

    /**
     * 播放器的最大宽度，避免重复计算
     */
    private int mPlayerMaxWidth;

    /**
     * 播放器最小宽度，避免重复计算
     */
    private int mPlayerMinWidth;

    /**
     * 当前拖动的方向
     */
    private int mDragDirect = NONE;

    /**
     * 垂直拖动时的偏移量
     * (mTop - mMinTop) / mVerticalRange
     */
    private float mVerticalOffset = 1f;

    /**
     * 水平拖动的偏移量
     * (mLeft + mPlayerMinWidth) / mHorizontalRange)
     */
    private float mHorizontalOffset = ORIGINAL_MIN_OFFSET;

    /**
     * 弱引用，绑定回调
     */
    private WeakReference<Callback> mCallback;

    /**
     * 触发ACTION_DOWN时的坐标
     */
    private int mDownX;
    private int mDownY;

    /**
     * 最终播放器消失的方向
     */
    private int mDisappearDirect = SLIDE_RESTORE_ORIGINAL;

    public DragVideoView(Context context) {
        this(context, null);
    }

    public DragVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mDragHelper = CustomViewDragHelper.create(this, 1f, new MyHelperCallback());
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void restorePosition() {//恢复原始状态
        mPlayer.setAlpha(1f);
        this.setAlpha(0f);//当前DragVideoView变成透明
        mLeft = mHorizontalRange - mPlayerMinWidth;
        mTop = mVerticalRange;
        mIsMinimum = true;
        mVerticalOffset = 1f;
    }

    public void show() {
        this.setAlpha(1f);//当前DragVideoView变成全部显示
        mDragDirect = VERTICAL;
        maximize();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isHit = mDragHelper.isViewUnder(mPlayer, (int) event.getX(), (int) event.getY());

        if (isHit) {
            switch (MotionEventCompat.getActionMasked(event)) {
                case MotionEvent.ACTION_DOWN: {
                    mDownX = (int) event.getX();
                    mDownY = (int) event.getY();
                }
                break;

                case MotionEvent.ACTION_MOVE:
                    if (mDragDirect == NONE) {
                        int dx = Math.abs(mDownX - (int) event.getX());//上一次getX()时和在MOVE过程中getX()的差值
                        int dy = Math.abs(mDownY - (int) event.getY());//上一次getY()时和在MOVE过程中getY()的差值
                        int slop = mDragHelper.getTouchSlop();//用户拖动的最小距离

                        if (Math.sqrt(dx * dx + dy * dy) >= slop) {//判断是水平方向拖拽，还是垂直方向上拖拽
                            if (dy >= dx)
                                mDragDirect = VERTICAL;
                            else
                                mDragDirect = HORIZONTAL;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP: {
                    if (mDragDirect == NONE) {
                        int dx = Math.abs(mDownX - (int) event.getX());
                        int dy = Math.abs(mDownY - (int) event.getY());
                        int slop = mDragHelper.getTouchSlop();

                        if (Math.sqrt(dx * dx + dy * dy) < slop) {
                            mDragDirect = VERTICAL;

                            if (mIsMinimum)
                                maximize();
                            else
                                minimize();
                        }
                    }
                }
                break;

                default:
                    break;
            }
        }

        mDragHelper.processTouchEvent(event);
        return isHit;
    }

    private void maximize() {
        mIsMinimum = false;
        slideVerticalTo(0f);
    }

    private void minimize() {
        mIsMinimum = true;
        slideVerticalTo(1f);
    }

    private boolean slideVerticalTo(float slideOffset) {//滑动到垂直方向上某位置
        int topBound = mMinTop;
        int y = (int) (topBound + slideOffset * mVerticalRange);

        if (mDragHelper.smoothSlideViewTo(mPlayer, mIsMinimum ?
                (int) (mPlayerMaxWidth * (1 - PLAYER_RATIO)) : getPaddingLeft(), y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    private void slideToLeft() {//左滑
        slideHorizontalTo(0f);
        mDisappearDirect = SLIDE_TO_LEFT;
    }

    private void slideToRight() {//右滑
        slideHorizontalTo(1f);
        mDisappearDirect = SLIDE_TO_RIGHT;
    }

    private void slideToOriginalPosition() {//原地
        slideHorizontalTo(ORIGINAL_MIN_OFFSET);
        mDisappearDirect = SLIDE_RESTORE_ORIGINAL;
    }

    private boolean slideHorizontalTo(float slideOffset) {//滑动到水平方向上某位置
        int leftBound = -mPlayer.getWidth();
        int x = (int) (leftBound + slideOffset * mHorizontalRange);
        if (mDragHelper.smoothSlideViewTo(mPlayer, x, mTop)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    private class MyHelperCallback extends CustomViewDragHelper.Callback { //继承CustomViewDragHelper的Callback
        @Override
        public boolean tryCaptureView(View child, int pointerId) {//当前view是否允许拖动
            return child == mPlayer; //如果是显示视频区域的view
        }

        @Override
        public void onViewDragStateChanged(int state) { //当ViewDragHelper状态发生变化时回调（IDLE,DRAGGING,SETTING[自动滚动时]）
            if (state == CustomViewDragHelper.STATE_IDLE) {
                if (mIsMinimum && mDragDirect == HORIZONTAL && mDisappearDirect != SLIDE_RESTORE_ORIGINAL) {
                    if (mCallback != null && mCallback.get() != null)
                        mCallback.get().onDisappear(mDisappearDirect);//水平方向上拖拽消失回调

                    mDisappearDirect = SLIDE_RESTORE_ORIGINAL;
                    restorePosition();
                    requestLayoutLightly();
                }
                mDragDirect = NONE;
            }
        }

        @Override
        public int getViewVerticalDragRange(View child) { //垂直方向拖动的最大距离
            int range = 0;
            if (child == mPlayer && mDragDirect == VERTICAL) {
                range = mVerticalRange;
            }
            Log.d(TAG, ">> getViewVerticalDragRange-range:" + range);
            return range;
        }

        @Override
        public int getViewHorizontalDragRange(View child) { //横向拖动的最大距离
            int range = 0;

            if (child == mPlayer && mIsMinimum && mDragDirect == HORIZONTAL) {
                range = mHorizontalRange;
            }
            Log.d(TAG, ">> getViewHorizontalDragRange-range:"+range);
            return range;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {//该方法中对child移动的边界进行控制，left , top 分别为即将移动到的位置
            int newTop = mTop;
            Log.d(TAG, ">> clampViewPositionVertical:" + top + "," + dy);
            if (child == mPlayer && mDragDirect == VERTICAL) {
                int topBound = mMinTop;
                int bottomBound = topBound + mVerticalRange;
                newTop = Math.min(Math.max(top, topBound), bottomBound);
            }
            Log.d(TAG, ">> clampViewPositionVertical:newTop-"+newTop);
            return newTop;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) { //返回横向坐标左右边界值  
            int newLeft = mLeft;
            Log.d(TAG, ">> clampViewPositionHorizontal:" + left + "," + dx);
            if (child == mPlayer && mIsMinimum && mDragDirect == HORIZONTAL) {
                int leftBound = -mPlayer.getWidth();
                int rightBound = leftBound + mHorizontalRange;
                newLeft = Math.min(Math.max(left, leftBound), rightBound);
            }
            Log.d(TAG, ">> clampViewPositionHorizontal:newLeft-"+newLeft+",mLeft-"+mLeft);
            return newLeft;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) { //view在拖动过程坐标发生变化时会调用此方法，包括两个时间段：手动拖动和自动滚动 
            Log.d(TAG, ">> onViewPositionChanged:" + "mDragDirect-" + mDragDirect + ",left-" + left + ",top-" + top + ",mLeft-" + mLeft);
            Log.d(TAG, ">> onViewPositionChanged-mPlayer:left-"+mPlayer.getLeft()+",top-"+mPlayer.getTop());
            if (mDragDirect == VERTICAL) { //垂直方向
                mTop = top;
                mVerticalOffset = (float) (mTop - mMinTop) / mVerticalRange;
            } else if (mIsMinimum && mDragDirect == HORIZONTAL) { // 水平方向
                mLeft = left;
                mHorizontalOffset = Math.abs((float) (mLeft + mPlayerMinWidth) / mHorizontalRange);
            }
            requestLayoutLightly();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {//
            if (mDragDirect == VERTICAL) { //如果拖拽的方向是在垂直方向上
                if (yvel > 0 || (yvel == 0 && mVerticalOffset >= 0.5f))
                    minimize();
                else if (yvel < 0 || (yvel == 0 && mVerticalOffset < 0.5f))
                    maximize();
            } else if (mIsMinimum && mDragDirect == HORIZONTAL) { //如果已经最小化窗口，并且是在水平方向上
                if ((mHorizontalOffset < LEFT_DRAG_DISAPPEAR_OFFSET && xvel < 0))
                    slideToLeft(); //向左滑动
                else if ((mHorizontalOffset > RIGHT_DRAG_DISAPPEAR_OFFSET && xvel > 0))
                    slideToRight();// 向右滑动
                else
                    slideToOriginalPosition();//原地不动
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2)
            throw new RuntimeException("this ViewGroup must only contains 2 views");

        mPlayer = getChildAt(0);
        mDesc = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        customMeasure(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        if (!mIsFinishInit) {
            mMinTop = getPaddingTop();
            mPlayerMinWidth = mPlayer.getMeasuredWidth();
//            mPlayerMaxWidth = (int)(mPlayerMinWidth / PLAYER_RATIO);
            mHorizontalRange = mPlayerMaxWidth + mPlayerMinWidth;
            mVerticalRange = getMeasuredHeight() - getPaddingTop() - getPaddingBottom()
                    - mPlayer.getMeasuredHeight();

            restorePosition();
            mIsFinishInit = true;
        }
    }

    private void customMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measurePlayer(widthMeasureSpec, heightMeasureSpec);
        measureDesc(widthMeasureSpec, heightMeasureSpec);
    }

    private void measurePlayer(int widthMeasureSpec, int heightMeasureSpec) {
        final LayoutParams lp = mPlayer.getLayoutParams();
        if (!mIsFinishInit) {
            int measureWidth = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight(), lp.width);

            mPlayerMaxWidth = MeasureSpec.getSize(measureWidth);
        }

        justMeasurePlayer();
    }

    private void measureDesc(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(mDesc, widthMeasureSpec, heightMeasureSpec);
    }

    private void justMeasurePlayer() {
        int widthCurSize = (int) (mPlayerMaxWidth * (1f - mVerticalOffset * (1f - PLAYER_RATIO)));
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthCurSize, MeasureSpec.EXACTLY);

        int heightSize = (int) (MeasureSpec.getSize(childWidthMeasureSpec) / VIDEO_RATIO);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);

        mPlayer.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        onLayoutLightly();
    }

    private void onLayoutLightly() {
        if (mDragDirect != HORIZONTAL) {
            mLeft = this.getWidth() - this.getPaddingRight() - this.getPaddingLeft()
                    - mPlayer.getMeasuredWidth();

            mDesc.layout(mLeft, mTop + mPlayer.getMeasuredHeight(),
                    mLeft + mDesc.getMeasuredWidth(), mTop + mDesc.getMeasuredHeight());
        }

        mPlayer.layout(mLeft, mTop, mLeft + mPlayer.getMeasuredWidth(), mTop + mPlayer.getMeasuredHeight());
    }

    private void requestLayoutLightly() {
        justMeasurePlayer();
        onLayoutLightly();
        ViewCompat.postInvalidateOnAnimation(this);//进行重绘
    }

    public void setCallback(Callback callback) {
        mCallback = new WeakReference<>(callback);
    }

    public interface Callback {
        void onDisappear(int direct);
    }
}
