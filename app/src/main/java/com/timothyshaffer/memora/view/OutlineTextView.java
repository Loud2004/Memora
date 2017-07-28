package com.timothyshaffer.memora.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.timothyshaffer.memora.R;


/**
 * Display text with an outline/border around the letters
 */
public class OutlineTextView extends View {

    private String mOutlineText = ""; // TODO: use a default from R.string...
    private int mTextColor = Color.BLACK; // TODO: use a default from R.color...
    private int mOutlineColor = Color.WHITE;
    private float mTextSize = 10; // TODO: use a default from R.dimen...
    private int mOutlineStroke = 8;

    private Paint mTextPaint;
    private Paint mOutlinePaint;
    private float mTextWidth;
    private float mTextHeight;

    public OutlineTextView(Context context) {
        super(context);
        init(null, 0);
    }

    public OutlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public OutlineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.OutlineTextView, defStyle, 0);

        mOutlineText = a.getString(R.styleable.OutlineTextView_outlineText);
        if( mOutlineText ==  null ) { mOutlineText = ""; }
        mTextColor = a.getColor(R.styleable.OutlineTextView_outlineTextColor, mTextColor);
        mOutlineColor = a.getColor(R.styleable.OutlineTextView_outlineColor, mOutlineColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mTextSize = a.getDimension(R.styleable.OutlineTextView_outlineTextSize, mTextSize);
        mOutlineStroke = a.getInt(R.styleable.OutlineTextView_outlineStroke, mOutlineStroke);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        //mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setStyle(Paint.Style.FILL);

        mOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlinePaint.setTextSize(mTextSize);
        mOutlinePaint.setColor(mOutlineColor);
        //mOutlinePaint.setTextAlign(Paint.Align.CENTER);
        mOutlinePaint.setTextAlign(Paint.Align.LEFT);
        mOutlinePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(mOutlineStroke);

        invalidateAndMeasureText();
    }

    private void invalidateAndMeasureText() {
        mTextWidth = mTextPaint.measureText(mOutlineText);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        // Draw the outline.
        canvas.drawText(
                mOutlineText,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
                mOutlinePaint);
        // Draw the text.
        canvas.drawText(
                mOutlineText,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
                mTextPaint);

        // Solution that has correct preview in Android Studio
        /*canvas.drawText("Some Text", 99, 99, mOutlinePaint);
        canvas.drawText("Some Text", 99, 101, mOutlinePaint);
        canvas.drawText("Some Text", 101, 99, mOutlinePaint);
        canvas.drawText("Some Text", 101, 101, mOutlinePaint);

        canvas.drawText("Some Text", 100, 100, mTextPaint);*/

    }

    public String getText() {
        return mOutlineText;
    }

    public void setText(String text) {
        this.mOutlineText = text;
        invalidateAndMeasureText();
    }
}
