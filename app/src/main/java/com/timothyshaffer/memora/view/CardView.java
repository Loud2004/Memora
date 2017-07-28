package com.timothyshaffer.memora.view;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.timothyshaffer.memora.R;

import java.util.ArrayList;


/**
 * A View that represents a flashcard
 */
public class CardView extends RelativeLayout {
    public static final float DEFAULT_HEIGHT_RATIO = 0.68f;
    public static final int SWIPE_MIN_DISTANCE = 120;

    // Data members for each card
    private int mCardId;
    private boolean mSpaToEng;
    private String mCategoryNames;
    private ArrayList<String> mExampleUsage;
    // The Card faces that will act as the UI and store front/back text
    private CardFaceView mFrontSide;
    private CardFaceView mBackSide;

    // The ratio of height/width that this View will measure itself as
    private float mHeightRatio;
    // The the text bounds (allocate here instead of each time in onDraw)
    Rect textBounds = new Rect();

    // SpaToEng variables
    private int spaToEngColor;
    private int engToSpaColor;

    // Animation Sets
    private boolean bFrontShowing;
    private AnimatorSet animFlipUpIn;       // Card flip up and fade-in  (back of card)
    private AnimatorSet animFlipUpOut;      // Card flip up and fade-out (front of card)
    private AnimatorSet animFlipDownIn;     // Card flip down and fade-in (back of card)
    private AnimatorSet animFlipDownOut;    // Card flip down and fade-out (front of card)
    private AnimatorSet animFlipUp;         // Full flip up animation (fades front out and back in)
    private AnimatorSet animFlipDown;       // Full flip down animation (fades front out and back in)
    // Simple lock to prevent two animations for playing at once
    private boolean bAnimationPlaying = false;

    public CardView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }


    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Instantiate the CardFaceViews now so we can easily set their attributes as they are read
        mFrontSide = new CardFaceView(context);
        mBackSide = new CardFaceView(context);

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CardView, defStyle, 0);

        // Get the text and the text color for each side
        String frontText = a.getString(R.styleable.CardView_cardFrontText);
        if ((frontText == null) || (frontText.isEmpty())) {
            frontText = getContext().getString(R.string.defaultCardText); // Use default string
        }
        String backText = a.getString(R.styleable.CardView_cardBackText);
        if ((backText == null) || (backText.isEmpty())) {
            backText = getContext().getString(R.string.defaultCardText); // Use default string
        }

        int frontTextColor = a.getColor(R.styleable.CardView_cardFrontTextColor,
                ContextCompat.getColor(getContext(), R.color.defaultCardTextColor));
        int backTextColor = a.getColor(R.styleable.CardView_cardBackTextColor,
                ContextCompat.getColor(getContext(), R.color.defaultCardTextColor));

        spaToEngColor = a.getColor(R.styleable.CardView_cardSpaToEngTextColor,
                ContextCompat.getColor(getContext(), R.color.defaultCardLangColor));
        engToSpaColor = a.getColor(R.styleable.CardView_cardEngToSpaTextColor,
                ContextCompat.getColor(getContext(), R.color.defaultCardLangColor));

        // Set the text and text color for each side
        mFrontSide.setText(frontText);
        mFrontSide.setTextColor(frontTextColor);
        mBackSide.setText(backText);
        mBackSide.setTextColor(backTextColor);

        // For testing with Android Studio Preview
        if( isInEditMode() ) {
            // These are normally set in setSpaToEng(boolean spaToEng) when initializing the CardView
            mFrontSide.setLang("Spa");
            mFrontSide.setLangColor(spaToEngColor);
            mBackSide.setLang("Eng");
            mBackSide.setLangColor(engToSpaColor);
            //mFrontSide.setTextSize(100);  // REMOVE THIS AFTER TESTING IS FINISHED
        }

        // Set the background for both if one is passed in
        Drawable backgroundDrawable;
        if (a.hasValue(R.styleable.CardView_cardBackground)) {
            backgroundDrawable = a.getDrawable(R.styleable.CardView_cardBackground);
            //backgroundDrawable.setCallback(this);
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
                mFrontSide.setBackground(backgroundDrawable);
                mBackSide.setBackground(backgroundDrawable);
            } else {    // Uses old version deprecated in API 16
                //noinspection deprecation
                mFrontSide.setBackgroundDrawable(backgroundDrawable);
                //noinspection deprecation
                mBackSide.setBackgroundDrawable(backgroundDrawable);
            }
        }

        // Get the height/width ratio
        mHeightRatio = a.getFloat(R.styleable.CardView_cardHeightRatio, DEFAULT_HEIGHT_RATIO);

        a.recycle();

        // Add the CardFaceViews to the ViewGroup.
        // Make sure the front card face is the first child so it is shown first
        addView(mFrontSide, 0);
        addView(mBackSide, 1);

        // Pass the padding value through to the CardFaceViews only.
        // Don't apply to this ViewGroup itself.
        mFrontSide.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        mBackSide.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        setPadding(0, 0, 0, 0);

        // Set the cards to have horizontal margins so that this ViewGroup can take up the entire
        // ViewPager area to prevent glitches with the "going past first/last" animations.
        // Keep right/left margins equal to center the CardFaceViews horizontally.
        // Also set CardFaceViews to center vertically.
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFrontSide.getLayoutParams();
        params.setMargins(
                getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin),
                0,
                getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin),
                0);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        mFrontSide.setLayoutParams(params);
        mBackSide.setLayoutParams(params);

        // Hide the back side
        mBackSide.setAlpha(0);
        bFrontShowing = true;

        // Animations don't play well with the AndroidStudio Preview window...
        // Skip them when Previewing the layout in AndroidStudio
        if( !isInEditMode() ) {
            // Setup the "Flip" animations
            animFlipUpIn = (AnimatorSet) AnimatorInflater.loadAnimator(
                    context, R.animator.card_flip_up_in);
            animFlipUpOut = (AnimatorSet) AnimatorInflater.loadAnimator(
                    context, R.animator.card_flip_up_out);
            animFlipDownIn = (AnimatorSet) AnimatorInflater.loadAnimator(
                    context, R.animator.card_flip_down_in);
            animFlipDownOut = (AnimatorSet) AnimatorInflater.loadAnimator(
                    context, R.animator.card_flip_down_out);
            // TODO: use play() with() instead of another AnimatorSet
            // Setup the full flip up animation
            animFlipUp = new AnimatorSet();
            animFlipUp.playTogether(animFlipUpIn, animFlipUpOut);
            // Setup the full flip down animation
            animFlipDown = new AnimatorSet();
            animFlipDown.playTogether(animFlipDownIn, animFlipDownOut);
            /* FIX for an animation starting before the previous animation is finished. *
             * Was causing upside-down text when user swiped and then quickly tapped.   */
            Animator.AnimatorListener animListener = new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    bAnimationPlaying = true;
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    bAnimationPlaying = false;
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                    bAnimationPlaying = false;
                }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            };
            animFlipDown.addListener(animListener);
            animFlipUp.addListener(animListener);
            /* END FIX */
        }

        // Detect tap and up/down swipe gestures in order to call the animations
        final GestureDetector gesture = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    // REQUIRED: Start listening to event
                    @Override
                    public boolean onDown(MotionEvent e) {
                            /*if( (e.getY() < mFrontSide.getTop()) || (e.getY() > mFrontSide.getBottom()) ) {
                                return false;
                            }*/
                        return true;
                    }
                    // Flip the card whn it is tapped
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        animateFlipDown();
                        return true;
                    }
                    // Flip the card up when swiped up; flip down when swiped down
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE) {
                            // Bottom to Top Swipe
                            animateFlipUp();
                            return true;
                        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE) {
                            // Top to Bottom Swipe
                            animateFlipDown();
                            return true;
                        }
                        // Otherwise, pass the gesture through to the children
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }
                });

        // CardView takes up the entire parent View, so anything clickable must be
        // on top of CardView, or in a separate ViewGroup
        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gesture.onTouchEvent(event);
            }
        });

    }


    public void showFront() {
        if( !bFrontShowing ) {
            // Instantly flip the card, so the user doesn't see it
            // flip as they scroll to the next card.
            mFrontSide.animate().rotationX(0).alpha(1).setDuration(0);
            mBackSide.animate().rotationX(-180).alpha(0).setDuration(0);
            bFrontShowing = true;
        }
    }

    /**
     * Play card "Flip Up" animation and switch which card view is visible to the user.
     */
    private void animateFlipUp() {
        // If an animation is already playing, then skip this animation
        if( bAnimationPlaying ) {
            return;
        }
        // Set the card being faded-out and the card being faded-in
        // based on which is currently being shown to the user
        animFlipUpIn.setTarget( bFrontShowing ? mBackSide : mFrontSide );
        animFlipUpOut.setTarget( bFrontShowing ? mFrontSide : mBackSide );
        // Start the animation
        animFlipUp.start();
        // The card_background_normal in the back is now being shown
        bFrontShowing = !bFrontShowing;
    }
    /**
     * Play card "Flip Down" animation. Same as above animateFlipUp(), but in opposite direction
     */
    private void animateFlipDown() {
        // If an animation is already playing, then skip this animation
        if( bAnimationPlaying ) {
            return;
        }
        /*if( bShowingFront ) {
            animFlipDownIn.setTarget(cardViewBack);
            animFlipDownOut.setTarget(cardViewFront);
        } else {
            animFlipDownIn.setTarget(cardViewFront);
            animFlipDownOut.setTarget(cardViewBack);
        }*/
        animFlipDownIn.setTarget( bFrontShowing ? mBackSide : mFrontSide );
        animFlipDownOut.setTarget( bFrontShowing ? mFrontSide : mBackSide );

        animFlipDown.start();

        bFrontShowing = !bFrontShowing;
    }


    // Public Getter/Setter functions
    public int getId() {
        return mCardId;
    }
    public void setId(int id) {
        mCardId = id;
    }
    public void setFrontText(String front) {
        mFrontSide.setText(front);
    }
    public void setFrontTextSize( int size ) {
        mFrontSide.setTextSize(size);
    }
    public void setBackText(String back) {
        mBackSide.setText(back);
    }
    public void setBackTextSize( int size ) {
        mBackSide.setTextSize(size);
    }
    public void setTextColors(int frontColor, int backColor) {
        mFrontSide.setTextColor(frontColor);
        mBackSide.setTextColor(backColor);
    }
    public boolean getSpaToEng() {
        return mSpaToEng;
    }
    public void setSpaToEng(boolean spaToEng) {
        mSpaToEng = spaToEng;
        // Now that we know which side is which we can set the Lang indicator values appropriately
        mFrontSide.setLang( mSpaToEng ? "Spa" : "Eng" );
        mFrontSide.setLangColor(mSpaToEng ? spaToEngColor : engToSpaColor);
        mBackSide.setLang(mSpaToEng ? "Eng" : "Spa");
        mBackSide.setLangColor( mSpaToEng ? engToSpaColor : spaToEngColor );
    }
    public void setLangColors( int frontColor, int backColor ) {
        mFrontSide.setLangColor(frontColor);
        mBackSide.setLangColor(backColor);
    }
    public String getListNames() {
        return mCategoryNames;
    }
    public void setListNames(String listNames) {
        mCategoryNames = listNames;
    }
    public ArrayList<String> getExampleUsage() {
        return mExampleUsage;
    }
    public void setExampleUsage(ArrayList<String> examples) {
        mExampleUsage = examples;
    }
    public void setBackgroundResources(int resId) {
        mFrontSide.setBackgroundResource(resId);
        mBackSide.setBackgroundResource(resId);
    }
    // Convert the examples stored into spannable (colored) text for inserting into a TextView
    public SpannableStringBuilder getSpannableExamples() {
        // The Span objects that will be used: font size, font color, font style
        AbsoluteSizeSpan exampleSize = new AbsoluteSizeSpan(20, true);
        AbsoluteSizeSpan translationSize = new AbsoluteSizeSpan(18, true);
        ForegroundColorSpan exampleColor =
                new ForegroundColorSpan( getResources().getColor(R.color.exampleColor) );
        ForegroundColorSpan translationColor =
                new ForegroundColorSpan( getResources().getColor(R.color.translationColor) );
        StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

        // Try to increase the readability of this code by shortening this variable name's length
        int exclusiveFlag = SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE;

        // Create an empty string
        SpannableStringBuilder builder =  new SpannableStringBuilder();
        // Track where in the string we are
        int start = 0;
        int end = 0;

        for( int i = 0; i < mExampleUsage.size(); i+=2 ) {
            //builder.append(mExampleUsage.get(i)+"\n", textColor, 0);
            //builder.append(mExampleUsage.get(i+1)+"\n", translationColor, 0);

            // Get the example and translation text and append newline on to each
            String example = mExampleUsage.get(i) + "\n";
            String translation = mExampleUsage.get(i+1) + "\n\n";

            // Measure the length and append it to the builder, then set the span for that length.
            end = start + example.length();
            builder.append(example);
            builder.setSpan(exampleSize, start, end, exclusiveFlag);
            builder.setSpan(exampleColor, start, end, exclusiveFlag);   // Flag: Don't expand the span
            // Reset the position to include the added text
            start = end;

            end = start + translation.length();
            builder.append(translation);
            builder.setSpan(translationSize, start, end, exclusiveFlag);
            builder.setSpan(translationColor, start, end, exclusiveFlag);
            builder.setSpan(italicSpan, start, end, exclusiveFlag);
            start = end;
        }
        // Return the SpannableString we built
        return builder;
    }

    /**
     * The actual card face which shows and draws the text for one side of the CardView
     */
    public class CardFaceView extends View {
        public static final int MAX_TEXT_SIZE = 120;
        public static final int MIN_TEXT_SIZE = 12;

        private String mText;
        private String mLang;

        private TextPaint mTextPaint;
        private TextPaint mLangPaint;
        private int mTextSize = 0;
        private float mLangWidth;
        private float mLangHeight;

        private StaticLayout mTextLayout;

        // Track which text we have already measured
        private boolean bMeasuredText = false;
        private boolean bMeasuredLang = false;


        public CardFaceView(Context context) {
            super(context);
            // Set up a default TextPaint object
            mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            mLangPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            mLangPaint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // The parent ViewGroup should have set this for us already
            int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
            int viewHeight = (int) (viewWidth * mHeightRatio);
            this.setMeasuredDimension(viewWidth, viewHeight);

            // Measure the small language indicator text before the main text so that we can
            // offset the main text above the language indicator text so they don't overlap

            // Only measure the language hint text if it has been set to something new
            if( !bMeasuredLang ) {
                // Determine the approximate final size of the language text
                float desiredWidth = viewWidth/12f;
                // Smallest recommended text size is 12
                mLangPaint.setTextSize(12);
                // Increase the text size until it is just slightly larger than the allotted size
                while( mLangPaint.measureText(mLang) < desiredWidth ) {
                    // Increase text size by 1
                    mLangPaint.setTextSize(mLangPaint.getTextSize()+1);
                }
                // Decrease it by 1 to make it fit again
                mLangPaint.setTextSize(mLangPaint.getTextSize()-1);

                // Store the final size of the text
                mLangWidth = mLangPaint.measureText(mLang);
                mLangHeight = mLangPaint.descent() + mLangPaint.ascent();

                // Don't re-measure until it is set to something new
                bMeasuredLang = true;
            }


            // Only measure the text if it has been set to something new
            if( !bMeasuredText ) {
                // Determine the final size of the contents with padding applied
                int mContentWidth = viewWidth - getPaddingLeft() - getPaddingRight();
                int mContentHeight = viewHeight - getPaddingTop() - getPaddingBottom();

                // Only measure the size when the user has not explicitly set the size
                if( mTextSize == 0 ) {
                    // Now set the text size based on the area we have available
                    // Initial size to start checking the text at
                    int tryTextSize = MAX_TEXT_SIZE;
                    boolean bFoundDesiredSize = false;
                    mTextPaint.setTextSize(tryTextSize);
                    mTextLayout = new StaticLayout(mText, mTextPaint, mContentWidth,
                            Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

                    // If the initial size is too large then slowly decrement the size until
                    // either: it fits, or the size is less than the minimum
                    while( ((mTextLayout.getWidth() > mContentWidth) || (mTextLayout.getHeight() > mContentHeight))
                            && (tryTextSize > MIN_TEXT_SIZE)) {

                        // Decrease the text size and try again on the next loop
                        tryTextSize--;
                        mTextPaint.setTextSize(tryTextSize);
                        mTextLayout = new StaticLayout(mText, mTextPaint, mContentWidth,
                                Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                    }
                } else {
                    // Use the user define size
                    mTextPaint.setTextSize(mTextSize);
                    mTextLayout = new StaticLayout(mText, mTextPaint, mContentWidth,
                            Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                }

                // Don't measure again again until the text is set to something new
                bMeasuredText = true;
            }


        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Center the text in the CardView
            canvas.save();
            float offsetX = (canvas.getWidth() / 2) - (mTextLayout.getWidth() / 2);
            float offsetY = (canvas.getHeight() / 2) - ((mTextLayout.getHeight() / 2));
            canvas.translate(offsetX, offsetY);

            mTextLayout.draw(canvas);
            canvas.restore();

            // TODO: Handle overlap of word with language hint
            // Draw the Language hint in the bottom right corner
            float rightEdge = getRight() - getLeft();   // The right edge of the card
            float bottomEdge = getBottom() - getTop();  // The bottom edge of the card
            canvas.drawText(mLang,
                    (rightEdge - mLangWidth - (mLangWidth * 0.3f)),
                    (bottomEdge - mLangPaint.descent() + (mLangHeight * 0.3f)), // descent is neg so subtract it
                    mLangPaint);
        }


        // Simple getter and setter methods for the string/color of text that this View displays.
        public String getText() {
            return mText;
        }
        public void setText(String strText) {
            mText = strText;
            bMeasuredText = false;
            requestLayout();    // Size the new text to fit the card
            invalidate();       // Display the new text
        }
        public void setTextSize(int size){
            mTextSize = size;
            bMeasuredText = false;
            requestLayout();    // Measure the text using the supplied size
            invalidate();       // Display the newly re-sized text
        }
        public int getTextColor() {
            return mTextPaint.getColor();
        }
        public void setTextColor(int color) {
            mTextPaint.setColor(color);
        }
        public String getLang() {
            return mLang;
        }
        public void setLang(String lang) {
            mLang = lang;
            bMeasuredLang = false;  // Size the new text to fit the allotted area
            requestLayout();        // Display the new language hint text
            invalidate();
        }
        public int getLangColor() {
            return mLangPaint.getColor();
        }
        public void setLangColor(int color) {
            mLangPaint.setColor(color);
        }
    }
}
