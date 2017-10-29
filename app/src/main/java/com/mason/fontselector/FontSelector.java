package com.mason.fontselector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jasonkent27 on 2017/10/24.
 */

public class FontSelector extends View {

    private final static boolean DEBUG = false;
    private static final int INVALID_POSITION = -1;
    private static final int DEFAULT_LINE_WIDTH = 2;
    private static final int DEFAULT_TEXT_SIZE = 13;
    private static final int DEFAULT_GAP = 18; // gap between line and text in dp

    private OnPositionChangedListener mOnPositionChangedListener;

    List<String> mDataList = new ArrayList<>();
    List<Float> mPoints = new ArrayList<>();

    private @ColorInt int mSelectedTextColor = Color.rgb(0xdd, 0x0, 0x0);
    private @ColorInt int mTextColor = Color.rgb(0x66, 0x66, 0x66);
    private @ColorInt int mLineColor = Color.rgb(0xdd, 0x0, 0x0);

    private Paint mLinePaint;
    private Paint mTextPaint;

    private Bitmap mCircleBitmap;
    private int mCircleHeight;
    private int mCircleWidth;
    private int mTextHeight;

    private int mDefaultHeight;
    private int mDefaultWidth;

    private float mCircleDrawX;
    private float mDefaultGap;
    private float mItemGap;

    private int mCurrentPosition = 0;
    private int mLastPosition = 0;


    public FontSelector(Context context) {
        this(context, null);
    }

    public FontSelector(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FontSelector(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStrokeWidth(dp2px(context, DEFAULT_LINE_WIDTH));
        mLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLinePaint.setColor(mLineColor);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(dp2px(context, DEFAULT_TEXT_SIZE));
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);

        mCircleBitmap = BitmapFactory.decodeResource(getResources(),
                R.mipmap.font_selector_icon);
        mCircleHeight = mCircleBitmap.getHeight();
        mCircleWidth = mCircleBitmap.getWidth();
        mCircleDrawX = getPaddingLeft();
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        mTextHeight = (int) Math.abs(fm.bottom - fm.top);
        mDefaultGap = dp2px(context, DEFAULT_GAP);

        final String[] array = getResources().getStringArray(R.array.text_list);
        mDataList.addAll(Arrays.asList(array));

        setDefaultPosition(mCurrentPosition);
    }

    public void setOnPositionChangedListener(OnPositionChangedListener listener) {
        this.mOnPositionChangedListener = listener;
    }

    public void setDefaultPosition(final int position) {
        post(new Runnable() {
            @Override
            public void run() {
                mCurrentPosition = position;
                adjustPosition(mCurrentPosition);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        mDefaultHeight = mCircleHeight + getPaddingTop() + getPaddingBottom()
                + mTextHeight + (int) mDefaultGap;
        mDefaultWidth = mCircleWidth + getPaddingLeft() + getPaddingRight();

        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mDefaultWidth, mDefaultHeight);

        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mDefaultWidth, heightSpecSize);

        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, mDefaultHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // step #1 draw text
        drawTopText(canvas);

        float lineY = getDrawTop() + mTextHeight + mDefaultGap + mCircleHeight / 2;
        // step #2 draw line
        canvas.drawLine(mPoints.get(0), lineY, mPoints.get(mPoints.size() - 1), lineY, mLinePaint);

        // step #3 finally, we draw the circle
        canvas.drawBitmap(mCircleBitmap, mCircleDrawX, lineY - mCircleHeight / 2, null);
    }

    protected float getDrawLeft() {
        return getPaddingLeft() + mCircleWidth / 2;
    }

    protected float getDrawTop() {
        return getPaddingTop();
    }

    /**
     * must call before {@link FontSelector#onDraw(Canvas)}.
     */
    private void adjustPosition(int position) {

        if (position >= mDataList.size() || position < 0) return;

        resetPointsLocation();
        mCircleDrawX = mPoints.get(position) - mCircleWidth / 2;
    }

    private void resetPointsLocation() {
        int len = mDataList.size();
        mPoints.clear();
        if (len > 1) {
            float textX = getDrawLeft();
            float availableWidth = getMeasuredWidth() - getPaddingLeft()
                    - getPaddingRight() - mCircleWidth;

            for (String text : mDataList) {
                availableWidth -= mTextPaint.measureText(text);
            }
            mItemGap = availableWidth / (len - 1);

            for (int i = 0; i < len; i++) {
                String text = mDataList.get(i);
                if (!TextUtils.isEmpty(text)) {
                    float textWidth = mTextPaint.measureText(text);
                    mPoints.add(textX + textWidth / 2);
                    textX += textWidth + mItemGap;
                }
            }
        } else {
            if (DEBUG) {
                throw new IllegalArgumentException("Data length must be greater than one!");
            }
            Log.e("FontSelector", "Data length must be greater than one!");
        }
    }

    private void drawTopText(Canvas canvas) {
        int len = mDataList.size();
        if (len > 1) {
            resetPointsLocation();
            float textY = getDrawTop() + mTextHeight;
            for (int i = 0; i < len; i++) {
                String text = mDataList.get(i);
                if (!TextUtils.isEmpty(text)) {

                    float textWidth = mTextPaint.measureText(text);
                    mTextPaint.setColor(i == mCurrentPosition ?
                            mSelectedTextColor : mTextColor);

                    canvas.drawText(text, mPoints.get(i) - textWidth / 2, textY, mTextPaint);
                }
            }
        } else {
            if (DEBUG) {
                throw new IllegalArgumentException("Data length must be greater than one!");
            }
            Log.e("FontSelector", "Data length must be greater than one!");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCircleDrawX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:

                int position = getNearestPoint(mCircleDrawX);
                if (position != INVALID_POSITION) {
                    mCurrentPosition = position;
                    if (mOnPositionChangedListener != null && mCurrentPosition != mLastPosition) {
                        mOnPositionChangedListener.onPositionChanged(mCurrentPosition);
                    }
                    mLastPosition = mCurrentPosition;
                }

                float right = mPoints.get(mPoints.size() - 1) - mCircleWidth / 2;
                float left = mPoints.get(0) - mCircleWidth / 2;
                float moveX = event.getX();

                // make sure the circle between [left , right]
                mCircleDrawX = Float.compare(moveX, left) < 0 ? left :
                        Float.compare(moveX, right) >= 0 ? right : moveX;

                invalidate();

                break;
            case MotionEvent.ACTION_UP:

                float upX = event.getX();
                int targetPosition = getNearestPoint(upX);
                if (targetPosition != INVALID_POSITION) {
                    mCircleDrawX = mPoints.get(targetPosition) - mCircleWidth / 2;
                    mCurrentPosition = targetPosition;
                    if (mOnPositionChangedListener != null && mCurrentPosition != mLastPosition) {
                        mOnPositionChangedListener.onPositionChanged(mCurrentPosition);
                    }
                    mLastPosition = mCurrentPosition;
                }
                invalidate();

                break;
        }
        return true;
    }

    private int getNearestPoint(float x) {
        for (int i = 0; i < mPoints.size(); i++) {
            Float point = mPoints.get(i);
            String text = mDataList.get(i);
            if (!TextUtils.isEmpty(text) &&
                    Math.abs(point - x) < (mTextPaint.measureText(text) + mItemGap) / 2) {
                return i;
            }
        }
        return INVALID_POSITION;
    }

    public interface OnPositionChangedListener {
        void onPositionChanged(int position);
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}