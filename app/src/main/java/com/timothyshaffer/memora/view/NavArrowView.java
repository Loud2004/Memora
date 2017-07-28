package com.timothyshaffer.memora.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.timothyshaffer.memora.R;


/**
 * Draw a simple arrow inside a circle
 */
public class NavArrowView extends View {
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    private int mArrowColor = Color.RED; // TODO: use a default from R.color...
    private int mArrowDirection = 3; // TODO: use a default from R.dimen...
    private int mCircleColor = Color.GRAY; // TODO: use default blah blah blah
    private boolean bFillCircle = true;

    private Paint mArrowPaint;
    private Paint mCirclePaint;
    private RectF mContentRect;
    private float radius;
    private Path arrowPath;

    public NavArrowView(Context context) {
        super(context);
        init(null, 0);
    }

    public NavArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public NavArrowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.NavArrowView, defStyle, 0);

        mArrowDirection = a.getInt(R.styleable.NavArrowView_arrowDirection, mArrowDirection);
        mArrowColor = a.getColor(R.styleable.NavArrowView_arrowColor, mArrowColor);
        mCircleColor = a.getColor(R.styleable.NavArrowView_circleColor, mCircleColor);
        bFillCircle = a.getBoolean(R.styleable.NavArrowView_fillCircle, bFillCircle);

        a.recycle();

        // Set up a default TextPaint object
        mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrowPaint.setColor(mArrowColor);
        mArrowPaint.setStyle(Paint.Style.STROKE);
        mArrowPaint.setStrokeCap(Paint.Cap.ROUND);
        mArrowPaint.setStrokeJoin(Paint.Join.ROUND);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(mCircleColor);

        // Allocate here to suppress: "Avoid object allocations during draw/layout operations"
        mContentRect = new RectF();
        arrowPath = new Path();
    }

    // Suppress warning about width/height being passed as height/width
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Set the width of this custom View to be 95% of the width of the
        // parent View (should be screen size), and set the View height to
        // 65% of the View's width (not the parent's)
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int mViewHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Set the size to what was originally passed in by the parent
        this.setMeasuredDimension(mViewWidth, mViewHeight);

        float contentWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
        float contentHeight = mViewHeight - getPaddingTop() - getPaddingBottom();
        // Use whichever side is smallest to determine how large the content area will be
        if( contentWidth > contentHeight ) {
            float offset = (contentWidth - contentHeight) / 2f;
            /*mContentRect.set(
                    offset + getPaddingLeft(),
                    getPaddingTop(),
                    mViewWidth - offset - getPaddingRight(),
                    mViewHeight - getPaddingBottom() );*/
            mContentRect.set(0, 0, contentHeight, contentHeight);
            mContentRect.offsetTo(offset+getPaddingLeft(), getPaddingTop());
        } else {
            float offset = (contentHeight - contentWidth) / 2f;
            /*mContentRect.set(
                    getPaddingLeft(),
                    offset + getPaddingTop(),
                    mViewWidth - getPaddingRight(),
                    mViewHeight - offset - getPaddingBottom() );*/
            mContentRect.set(0, 0, contentWidth, contentWidth);
            mContentRect.offsetTo(getPaddingLeft(), offset+getPaddingTop());
        }

        // Determine the circle's radius
        radius = (mContentRect.width() / 2f) - mArrowPaint.getStrokeWidth();

        // Offset from the circle on the side the arrow is pointing
        float forwardOffset = mContentRect.centerX() + (radius * 0.40f);
        // Offset from the circle in the rear of the arrow.
        float rearOffset = mContentRect.centerX() - (radius * 0.25f);
        // Offset from the circle on the sides (top/bottom) of the arrow
        float sideOffset = (radius * 0.5f);
        float topOffset = mContentRect.centerY() - sideOffset;
        float botOffset = mContentRect.centerY() + sideOffset;

        // Generate a path to draw a right-facing arrow
        arrowPath.moveTo(rearOffset, topOffset);
        arrowPath.lineTo(forwardOffset, mContentRect.centerY());
        arrowPath.lineTo(rearOffset, botOffset);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the circle around the arrow
        if( bFillCircle ) {
            mCirclePaint.setStyle(Paint.Style.FILL);
        } else {
            mCirclePaint.setStyle(Paint.Style.STROKE);
            // Set the stroke size to be proportional to the view size
            mCirclePaint.setStrokeWidth(mContentRect.width()*0.03f);
        }

        canvas.drawCircle(
                mContentRect.centerX(),
                mContentRect.centerY(),
                radius,
                mCirclePaint);

        // TODO: Allow for alternative arrow directions
        // Instead of calculating four distinct sets of points to draw the arrow,
        // we will just rotate the canvas and draw a single set of arrow points.
        canvas.save();
        switch(mArrowDirection) {
            case 0:
                // rotate -90
                canvas.rotate(270, mContentRect.centerX(), mContentRect.centerY());
                break;
            case 1:
                // rotate 90
                canvas.rotate(90, mContentRect.centerX(), mContentRect.centerY());
                break;
            case 2:
                // rotate 180
                canvas.rotate(180, mContentRect.centerX(), mContentRect.centerY());
                break;
            case 3: // Case 3 will also be the default
            default:
                // do nothing
                break;
        }

        // Draw the arrow
        mArrowPaint.setStrokeWidth(mContentRect.width() * 0.1f);
        canvas.drawPath(arrowPath, mArrowPaint);

        canvas.restore();

    }

    public void setArrowDirection( int direction ) {
        mArrowDirection = direction;
        invalidate();
    }

}
