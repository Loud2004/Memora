package com.timothyshaffer.memora.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.timothyshaffer.memora.R;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * A View to display a pie chart
 */
public class PieChartView extends View {

    /**
     * Helper class to store the data for each "slice" of the graph/pie
     */
    public static class PieSlice {
        // User supplied values
        public String label;
        public int value;
        public int color;
        // Computed values
        public float startAngle;
        public float endAngle;
        public int highlightColor;
        public Shader shader;
        public PointF labelPosition;


        PieSlice(String label, int value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }


    }

    // The slice data for this pie graph
    private ArrayList<PieSlice> slices = new ArrayList<PieSlice>();

    // Currently unused
    private int mTextColor = Color.RED; // TODO: use a default from R.color...
    private float mTextSize = 10; // TODO: use a default from R.dimen...
    private TextPaint mTextPaint;
    Paint.FontMetrics mTextPaintMetrics;    // Used to describe mTextPaint

    // Paint for the slices
    private Paint mPiePaint;
    // How much highlight to apply to the gradient painted on each slice
    private float mHighlightStrength;

    // Animation variables
    private float mSweep;   // How much of the pie chart to show
    private int mAnimationDuration;
    ObjectAnimator sweepAnimator;
    private int mCurrentTotal;   // How much of the total value to show (while counting up)
    ObjectAnimator countAnimator;


    public PieChartView(Context context) {
        super(context);
        init(null, 0);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.PieChartView, defStyle, 0);

        mTextColor = a.getColor(R.styleable.PieChartView_legendTextColor, mTextColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mTextSize = a.getDimension(R.styleable.PieChartView_legendTextSize, mTextSize);
        mHighlightStrength = a.getFloat(R.styleable.PieChartView_highlightStrength, 1.0f);
        mAnimationDuration = a.getInt(R.styleable.PieChartView_animationDuration, 1500);

        a.recycle();

        // Set the View to use Software rendering otherwise there will be
        // small gaps between sections of the arc segments.
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Setup a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        // Get the size data for the current font
        mTextPaintMetrics = mTextPaint.getFontMetrics();

        // Setup the paint for the pie slices
        mPiePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPiePaint.setStyle(Paint.Style.STROKE);
        mPiePaint.setStrokeWidth(45);   // TODO: set stroke using XML
        mPiePaint.setStrokeJoin(Paint.Join.ROUND);
        mPiePaint.setStrokeCap(Paint.Cap.BUTT);

        // By default the circle is animated into view
        mSweep = 0.0f;
        sweepAnimator = ObjectAnimator.ofFloat(this, "sweep", 0f, 360f);
        // TODO: Make duration editable in XML
        sweepAnimator.setDuration(mAnimationDuration);
        sweepAnimator.setInterpolator(new DecelerateInterpolator());
        // Initialize the list of animators for each value in the pie
        //valueAnimators = new ArrayList<ValueAnimator>();

        // Random Sample data for Android Studio UI display
        if (this.isInEditMode()) {
            addSlice("Easy", 1, getResources().getColor(R.color.easyColor));
            addSlice("Med", 1, getResources().getColor(R.color.medColor));
            addSlice("Hard", 0, getResources().getColor(R.color.hardColor));
            addSlice("Critical",1,getResources().getColor(R.color.criticalColor));

            // Generate the slice data
            int paddingLeft = getPaddingLeft();
            int paddingTop = getPaddingTop();
            int paddingRight = getPaddingRight();
            int paddingBottom = getPaddingBottom();
            int contentWidth = getWidth() - paddingLeft - paddingRight;
            int contentHeight = getHeight() - paddingTop - paddingBottom;
            RectF rectPieChart = new RectF(0, 0, contentWidth, contentHeight);
            rectPieChart.offsetTo(paddingLeft, paddingTop);
            updateSlices(rectPieChart,mPiePaint, mTextPaint);

            // By default we will draw the entire circle we in Android Studio
            mSweep = 360.0f;
            mCurrentTotal = 276;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Ask for a height that matches the width
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.setMeasuredDimension(parentWidth, parentWidth);
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

        RectF rectPieChart = new RectF(0, 0, contentWidth, contentHeight);
        rectPieChart.offsetTo(paddingLeft, paddingTop);

        // Rotate the canvas for the pie chart draw calls so that 0 degrees is pointing up
        canvas.save();
        // TODO: Make this value adjustable in the XML
        canvas.rotate(-90, rectPieChart.centerX(), rectPieChart.centerY());
        boolean hitMaxSweep = false;
        float totalSweep = 0.0f;
        for( PieSlice slice : slices ) {
            float sliceSweep = slice.endAngle - slice.startAngle;
            totalSweep += sliceSweep;
            // Check if we have hit the current MAX sweep angle
            if( totalSweep >= mSweep ) {
                // Sweep until we hit the maximum
                sliceSweep = sliceSweep - (totalSweep - mSweep);
                hitMaxSweep = true;
            }
            mPiePaint.setColor(slice.color);
            mPiePaint.setShader(slice.shader);
            //canvas.drawArc(rectPieChart, (360.0f - slice.endAngle), sliceSweep, false, mPiePaint);
            canvas.drawArc(rectPieChart, slice.startAngle, sliceSweep, false, mPiePaint);

            // Exit early if we hit the max sweep distance
            if(hitMaxSweep) {
                break;
            }
        }
        canvas.restore();


        // Draw the Text
        // TODO: Remove After Debugging is done
        // Determine the Rect that the text will occupy, Get a square that fits inside the circle
        /*RectF textRect = getInscribedRect( rectPieChart, mPiePaint.getStrokeWidth() );
        Paint testPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        testPaint.setStyle(Paint.Style.STROKE);
        testPaint.setStrokeWidth(1);
        testPaint.setColor(0xFF000000);
        canvas.drawRect(textRect, testPaint);*/


        // Determine the full spacing one line takes
        float lineSpace = mTextPaintMetrics.bottom - mTextPaintMetrics.top;
        // Center the text (ascent is measured from the baseline and is negative, so invert the math)
        float textY = ( rectPieChart.centerY() - (lineSpace/2) - mTextPaintMetrics.ascent );

        // Draw the current total (counts up)
        canvas.drawText(
                String.valueOf(mCurrentTotal),
                rectPieChart.centerX(),
                textY,
                mTextPaint);

    }

    public int getHighlightColor( int color ) {
        // Calculate the highlight color. Saturate at 0xff to make sure that high values
        // don't result in aliasing.
        return Color.argb(
                0xff,
                Math.min((int) (mHighlightStrength * (float) Color.red(color)), 0xff),
                Math.min((int) (mHighlightStrength * (float) Color.green(color)), 0xff),
                Math.min((int) (mHighlightStrength * (float) Color.blue(color)), 0xff)
        );
    }

    /** Update all of slices generated values.
     * Label, Value, and Color MUST be set for each PieSlice in slices
     * @param rectDrawArea The area in which the slice will be drawn
     * @param piePaint The paint that will be used to draw the pie sections
     * @param textPaint The paint that will be used to draw the text sections
     */
    public void updateSlices( RectF rectDrawArea, Paint piePaint, TextPaint textPaint ) {
        // Remove any slices that have a value of zero, they won't be displayed
        // Generate a total value from all the slices
        int iTotal = 0;
        Iterator<PieSlice> iter = slices.iterator();
        while( iter.hasNext() ) {
            int sliceValue = iter.next().value;
            if( sliceValue == 0 ) {
                iter.remove();
            } else {
                iTotal += sliceValue;
            }
        }

        // Generate each slice's data
        float currentAngle = 0.0f;
        long elapsedTime = 0;
        for( PieSlice slice : slices ) {
            slice.startAngle = currentAngle;
            slice.endAngle = slice.startAngle + (slice.value * 360.0f / (float)iTotal);
            currentAngle = slice.endAngle;
            slice.highlightColor = getHighlightColor(slice.color);
            // Generate the Shader
            slice.shader = new SweepGradient(
                    rectDrawArea.centerX(),
                    rectDrawArea.centerY(),
                    new int[]{
                            slice.highlightColor,
                            slice.highlightColor,
                            slice.color,
                            slice.color
                    },
                    /* {0f, start Xdeg/360, end Xdeg/360, 1f}*/
                    new float[]{
                            0.0f,
                            slice.startAngle / 360.0f,
                            slice.endAngle / 360.0f,
                            1.0f
                    }
            );

        }

        // Determine the Rect that the text will occupy, Get a square that fits inside the circle
        //RectF textRect = getInscribedRect( rectDrawArea, piePaint.getStrokeWidth() );

        /*// Find a text size that will fit into the inscribed rectangle
        boolean bFoundTextSize = false;
        // The max width that will be set after the loop has finished and found an appropriate size
        float maxTextWidth = 0f;
        while( !bFoundTextSize ) {
            // Find the widest text of all the slices (max value + label)
            float tempMaxTextWidth = 0f;
            for (PieSlice slice : slices) {
                String tempText = String.valueOf(slice.value) + " " + slice.label;
                float tempTextWidth = textPaint.measureText(tempText, 0, tempText.length());
                // If this is largest, set it as the new max
                if (tempTextWidth > tempMaxTextWidth) {
                    tempMaxTextWidth = tempTextWidth;
                }
            }
            // Check if the largest text will fit within the bounds
            if (tempMaxTextWidth > textRect.width()) {
                // Get the current text size
                float textSize = getTextSize();
                // Doesn't fit, shrink the text and loop again
                if( textSize > 2 ) {
                    // Skip 2 at a time as this is an expensive operation to keep performing
                    setTextSize(textSize-2);
                } else {
                    // Already at smallest size so keep it
                    bFoundTextSize = true;
                }
            } else {
                // Fits! Save the final max width and Exit the loop
                maxTextWidth = tempMaxTextWidth;
                bFoundTextSize = true;
            }
        }

        // Get the size data for the current font
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        // Determine the full spacing one line takes
        float lineSpace = metrics.bottom - metrics.top;
        float totalHeight = slices.size() * lineSpace;  // Spacing for all the lines
        // Center the text (ascent is measured from the baseline and is negative, so invert the math)
        float currentTextY = ( textRect.centerY() - (totalHeight/2) - metrics.ascent );
        // Center the X axis using the max width obtained previously
        float currentTextX = textRect.centerX() - (maxTextWidth/2);
        for( PieSlice slice : slices ) {

            slice.labelPosition = new PointF( textRect.left , currentTextY );
            // Add the text's height to move to the next line
            currentTextY += lineSpace;
        }*/

        // Generate the count animator that will count up in the circle based on the total value
        countAnimator = ObjectAnimator.ofInt(this, "currentTotal", 0, iTotal);
        countAnimator.setDuration(mAnimationDuration);
        // TODO: Find a better Interpolator
        countAnimator.setInterpolator(new DecelerateInterpolator());

    }

    /**
     * Add a slice to the pie chart
     * @param label The name of the slice
     * @param value How large the slice is
     * @param color The color to use to display the slice
     */
    public void addSlice(String label, int value, int color) {
        slices.add(new PieSlice(label, value, color));
    }

    public void startResultAnimation() {
        // TODO: consider storing these as member variables to reduce allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        RectF rectPieChart = new RectF(0, 0, contentWidth, contentHeight);
        rectPieChart.offsetTo(paddingLeft, paddingTop);

        // Generate the slice data
        updateSlices(rectPieChart, mPiePaint, mTextPaint);
        sweepAnimator.start();
        countAnimator.start();

    }

    /**
     * Return a rectangle that is completely inscribed by the circle formed by drawing an
     * arc (full circle) with the specified stroke width inside the given rectangle.
     *  The largest possible rectangle (by area) is a perfect square inside the circle.
     * @param rect The coordinates that the arc (circle) is drawn in
     * @param stroke The width of the stroke for the arc
     * @return The largest possible RectF that is completely inscribed by the arc/circle
     */
    public RectF getInscribedRect( RectF rect, float stroke ) {
        // Get the inside diameter of the circle
        // Only subtract ONE stroke width because the stroke is drawn on a path,
        // half the stroke is inside the path and half is outside.
        float diameter = rect.width() - stroke;
        // Compute the length of the sides (All sides are equal length)
        // a^2 + b^2 = c^2 --(a = b)--> 2(a^2) = c^2 --> a^2 = (c^2)/2 --> a = sqrt( (c^2)/2 )
        float side = (float) Math.sqrt( Math.pow(diameter, 2)/2 );
        // Make a new RectF without adjusted coordinates
        RectF inscribedRect = new RectF(0, 0, side, side);
        // Offset the square to have the same coordinate system as the circle
        inscribedRect.offsetTo(
                (rect.centerX()-inscribedRect.centerX()),
                (rect.centerY()-inscribedRect.centerY()) );
        return inscribedRect;
    }

    // Getter and Setter for the sweep so we can animate with an ObjectAnimator
    public float getSweep() {
        return mSweep;
    }

    public void setSweep(float sweep) {
        this.mSweep = sweep;
        invalidate();
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float mTextSize) {
        this.mTextSize = mTextSize;
    }

    public int getCurrentTotal() {
        return mCurrentTotal;
    }

    public void setCurrentTotal(int currentTotal) {
        this.mCurrentTotal = currentTotal;
    }
}
