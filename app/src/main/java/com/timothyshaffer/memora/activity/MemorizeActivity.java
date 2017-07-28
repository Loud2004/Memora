package com.timothyshaffer.memora.activity;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;
import com.timothyshaffer.memora.db.MemoraDbPagerAdapter;
import com.timothyshaffer.memora.dialog.FeedbackDialog;
import com.timothyshaffer.memora.view.CardView;
import com.timothyshaffer.memora.view.PieChartView;
import com.wunderlist.slidinglayer.SlidingLayer;

import java.util.ArrayList;
import java.util.List;


public class MemorizeActivity extends AppCompatActivity {

    // The views that make up the UI
    private ViewPager viewPager;
    private MemoraDbPagerAdapter pagerAdapter;
    // Track the current position in the ViewPager
    private int currentPage = 0;
    // Used to track which pages have already been answered
    private SparseBooleanArray answeredMap = new SparseBooleanArray();

    // Root layout for showing Snackbar messages
    private LinearLayout rootLayout;
    // Menu
    private Menu mMenu;
    // Sliding Layer and its children
    private SlidingLayer slidingLayer;
    private TextView categoryList;
    private TextView exampleList;
    // Progress Bar
    private TextView textProgressNum;
    private int iProgressNum;           // The int version for easy inc/dec manipulation
    private TextView textRemainingNum;
    private int iRemainingNum;          // The int version for easy inc/dec manipulation
    private ProgressBar progressBar;
    // Finish Screen
    private LinearLayout resultLayout;  // Layout that holds the pie graph and result list
    private PieChartView pieChart;      // Pie chart
    private LinearLayout resultList;    // List of results

    // Buttons
    private FrameLayout btnCritical;
    private FrameLayout btnHard;
    private FrameLayout btnMed;
    private FrameLayout btnEasy;
    private FrameLayout btnFinish;
    private FrameLayout btnAnswered;
    // Animation
    AnimatorSet animBtnSlideOut;
    // Variables to track user progress
    private int iNumCritical;
    private int iNumHard;
    private int iNumMed;
    private int iNumEasy;

    // Animation Sets
    private ValueAnimator addListAnim;      // Fade the result list in


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memorize);
        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Find the root layout
        rootLayout = (LinearLayout) findViewById(R.id.content_memorize);

        // Look for extra Intent data to determine which category we should select the cards from.
        // A value of 0 (zero) means that we will just select all cards from all categories.
        Intent intent = getIntent();
        String strCategory = intent.getStringExtra(StudyCategoryActivity.CATEGORY_ID);

        // Check for bad Intent data passed into Activity
        if(strCategory == null) {
            finish();
            return;
        }

        // Setup the database adapter
        pagerAdapter = new MemoraDbPagerAdapter(this, Integer.valueOf(strCategory));
        // Exit early if there are no cards (should never get here)
        if(pagerAdapter.getCount() == 0) {
            return;
        }
        // Setup the ViewPager that will animate between cards on screen
        viewPager = (ViewPager)findViewById(R.id.card_pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageMargin(50);
        // Setup a listener for when the page is changed
        viewPager.addOnPageChangeListener(mPageChangeListener);
        // HACK: The first page does not trigger the "page change" callback, so call it manually
        viewPager.post(new Runnable() {
            @Override
            public void run() {
                mPageChangeListener.onPageSelected(viewPager.getCurrentItem());
            }
        });

        slidingLayer = (SlidingLayer)findViewById(R.id.sliding_layer);
        categoryList = (TextView)findViewById(R.id.categories_text);
        exampleList = (TextView)findViewById(R.id.examples_text);

        textProgressNum = (TextView)findViewById(R.id.progress_num);
        textRemainingNum = (TextView)findViewById(R.id.remaining_num);
        progressBar = (ProgressBar)findViewById(R.id.progress_bar);

        btnCritical = (FrameLayout) findViewById(R.id.button_critical);
        btnHard = (FrameLayout) findViewById(R.id.button_hard);
        btnMed = (FrameLayout) findViewById(R.id.button_med);
        btnEasy = (FrameLayout) findViewById(R.id.button_easy);
        btnFinish = (FrameLayout) findViewById(R.id.button_finish);
        btnAnswered = (FrameLayout) findViewById(R.id.button_answered);

        // Set the progress bar and text based on the number of rows returned by the database
        iProgressNum = 0;
        iRemainingNum = pagerAdapter.getCount();
        textProgressNum.setText(String.valueOf(iProgressNum));
        textRemainingNum.setText(String.valueOf(iRemainingNum));
        progressBar.setMax(pagerAdapter.getCount());
        progressBar.setProgress(0);

        // Setup the "Results"
        resultLayout = (LinearLayout)findViewById(R.id.result_layout);
        pieChart = (PieChartView)findViewById(R.id.pie_chart);
        resultList = (LinearLayout)findViewById(R.id.result_list);
        iNumCritical = 0;
        iNumHard = 0;
        iNumMed = 0;
        iNumEasy = 0;

        // Setup onClick Listeners
        btnCritical.setOnClickListener(difficultyClickListener);
        btnHard.setOnClickListener(difficultyClickListener);
        btnMed.setOnClickListener(difficultyClickListener);
        btnEasy.setOnClickListener(difficultyClickListener);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFinish();
            }
        });
        btnAnswered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the btn being translated down and faded-out
                animBtnSlideOut.setTarget(btnAnswered);
                // Start the animation
                animBtnSlideOut.start();
            }
        });
        // Setup the animation for the finished button to slide out when the user clicks it
        animBtnSlideOut = (AnimatorSet) AnimatorInflater.loadAnimator(
                this, R.animator.btn_translate_down_out);

        // Animation of result list
        // Add the result's to the list
        addListAnim = ValueAnimator.ofInt(0, 3);
        addListAnim.setDuration(750);
        addListAnim.setInterpolator(new LinearInterpolator());
        addListAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int curAnimValue = (int) animation.getAnimatedValue();
                int nextNumber = 0;
                // Only update/animate each row once. This loop ensures that by tracking which
                // rows have already been updated.
                for (int i = 0; i < 4; i++) {
                    if (resultList.getChildAt(i).getVisibility() == View.VISIBLE) {
                        nextNumber = i + 1;
                    }
                }
                // Setting the View to VISIBLE will animate it for us
                if ((curAnimValue < 1) && (nextNumber == 0)) {
                    TextView textView = (TextView) findViewById(R.id.result_list_critical);
                    textView.setText(String.format(getResources().getString(R.string.result_critical), iNumCritical));
                    textView.setVisibility(View.VISIBLE);
                } else if ((curAnimValue >= 1) && (curAnimValue < 2) && (nextNumber == 1)) {
                    TextView textView = (TextView) findViewById(R.id.result_list_hard);
                    textView.setText(String.format(getResources().getString(R.string.result_hard), iNumHard));
                    textView.setVisibility(View.VISIBLE);
                } else if ((curAnimValue >= 2) && (curAnimValue < 3) && (nextNumber == 2)) {
                    TextView textView = (TextView) findViewById(R.id.result_list_med);
                    textView.setText(String.format(getResources().getString(R.string.result_medium), iNumMed));
                    textView.setVisibility(View.VISIBLE);
                } else if ((curAnimValue >= 3) && (nextNumber == 3)) {
                    TextView textView = (TextView) findViewById(R.id.result_list_easy);
                    textView.setText(String.format(getResources().getString(R.string.result_easy), iNumEasy));
                    textView.setVisibility(View.VISIBLE);
                }

            }
        });


    }

    ViewPager.SimpleOnPageChangeListener mPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            // Get a handle to the current CardView
            CardView cardView = (CardView) viewPager.findViewWithTag(String.valueOf(position));
            // Set the categories and examples using the current card
            categoryList.setText(cardView.getListNames());  // List names may be an empty string
            exampleList.setText(cardView.getSpannableExamples());

            // Block the user from answering again if they already did
            if (answeredMap.get(position)) {
                // Undo anything done by the animation
                btnAnswered.setTranslationY(0f);
                btnAnswered.setAlpha(1f);
                // Show the warning button
                btnAnswered.setVisibility(View.VISIBLE);
            } else {
                btnAnswered.setVisibility(View.INVISIBLE);
            }
            // Find the last view that was just on the screen before the current view
            // and return it to its starting position
            ((CardView) viewPager.findViewWithTag(String.valueOf(currentPage))).showFront();
            // Set the current view as the "last" view
            currentPage = position;
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_memorize, menu);
        // Save a handle to the menu
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get the menu item that was selected
        int id = item.getItemId();

        // Find out which card is currently being shown to the user
        CardView cardView = (CardView) viewPager.findViewWithTag(String.valueOf(currentPage));
        long cardId = (long)cardView.getId();       // Cast to long for WordActivity

        if( id == R.id.action_add_to_category ) {
            // Start WordActivity to add Categories to this card
            Intent intent = new Intent(this, WordActivity.class);
            intent.putExtra(WordActivity.ARG_CARD_ID, cardId);
            intent.putExtra(WordActivity.ARG_TAB_ID, WordActivity.WORD_CATEGORIES_FRAGMENT);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_add_example ) {
            // Start WordActivity to add Examples to this card
            Intent intent = new Intent(this, WordActivity.class);
            intent.putExtra(WordActivity.ARG_CARD_ID, cardId);
            intent.putExtra(WordActivity.ARG_TAB_ID, WordActivity.WORD_EXAMPLES_FRAGMENT);
            startActivity(intent);
            return true;

        } else if( id == R.id.action_removeWord ) {
            // Remove this word from all Categories
            // Get a list of all categories this cardId belongs to (in case of "Undo")
            final List<ContentValues> valuesArray = new ArrayList<>();
            Cursor categories = getContentResolver().query(
                    MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                    null,
                    MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID + "=?",
                    new String[]{String.valueOf(cardId)},
                    null );
            if( categories != null ){
                int idxCatId = categories.getColumnIndex(MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID);
                while( categories.moveToNext() ){
                    int catId = categories.getInt(idxCatId);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID, cardId);
                    contentValues.put(MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID, catId);
                    valuesArray.add(contentValues);
                }

                categories.close();
            }

            // Delete the Card_Category rows for this cardId
            getContentResolver().delete(
                    MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                    MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID + "=?",
                    new String[]{String.valueOf(cardId)} );

            // Show a Snackbar with an Undo button
            Snackbar snackbar = Snackbar
                    .make(rootLayout, "Word will never be shown again.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Re-insert the rows that were deleted
                            if( !valuesArray.isEmpty() ) {
                                // WORKAROUND: Android won't allow this cast: (ContentValues[]) valuesArray.toArray()
                                // So create a new (basic) array [using the alt. version of toArray()] instead of casting.
                                ContentValues[] insertArray = new ContentValues[valuesArray.size()];
                                insertArray = valuesArray.toArray(insertArray);
                                getContentResolver().bulkInsert(MemoraContentProvider.CONTENT_URI_CARD_CATEGORY, insertArray);
                            }

                            Snackbar snackbar1 = Snackbar.make(rootLayout, "Word restored!", Snackbar.LENGTH_SHORT);
                            snackbar1.show();
                        }
                    });

            snackbar.show();
            return true;
        } else if (id == R.id.action_feedback) {
            FeedbackDialog dialog = new FeedbackDialog();
            dialog.show(getSupportFragmentManager(), "FeedbackDialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Manually move to the next card in the ViewPager
     */
    private void getNextCard() {
        // Check if we are on the last page
        int currentItem = viewPager.getCurrentItem();
        if( currentItem == pagerAdapter.getCount()-1 ) {
            // Display the "Finished" screen and exit
            displayResults();
            return;
        }
        // Lower the sliding layer if it is up
        slidingLayer.closeLayer(true);
        // Move to the next item
        viewPager.setCurrentItem(currentItem + 1);
    }

    // TODO: Hint UI fill/update/replace the TextViews....


    private void incrementProgressBar() {
        // Update the completed/remaining numbers
        iProgressNum++;
        iRemainingNum--;
        // Set the UI text
        textProgressNum.setText(String.valueOf(iProgressNum));
        textRemainingNum.setText(String.valueOf(iRemainingNum));
        // Increment the progress bar
        progressBar.incrementProgressBy(1);
    }

    private void displayResults() {
        // Remove the memorization UI components
        viewPager.setVisibility(View.INVISIBLE);
        // Close the sliding layer and disable it
        slidingLayer.closeLayer(true);
        slidingLayer.setSlidingEnabled(false);
        findViewById(R.id.button_bar).setVisibility(View.INVISIBLE);
        btnAnswered.setVisibility(View.INVISIBLE);

        // Disable the menu item(s)
        mMenu.findItem(R.id.action_removeWord).setEnabled(false);

        // Setup pie chart
        pieChart.addSlice("Easy", iNumEasy, ContextCompat.getColor(this, R.color.easyColor));
        pieChart.addSlice("Med", iNumMed, ContextCompat.getColor(this, R.color.medColor));
        pieChart.addSlice("Hard", iNumHard, ContextCompat.getColor(this, R.color.hardColor));
        pieChart.addSlice("Critical", iNumCritical, ContextCompat.getColor(this, R.color.criticalColor));

        // Display the results UI components
        btnFinish.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.VISIBLE);
        pieChart.setVisibility(View.VISIBLE);
        resultList.setVisibility(View.VISIBLE);
        pieChart.startResultAnimation();
        addListAnim.start();

    }

    // A generic onClick Listener for the difficulty buttons that sets a difficulty
    // based on which button was pressed.
    View.OnClickListener difficultyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Check which button was actually pressed
            int difficulty = MemoraDbPagerAdapter.CRITICAL;  // Set to critical as default
            if( v == btnCritical ) {
                difficulty = MemoraDbPagerAdapter.CRITICAL;
                iNumCritical++;
            } else if( v == btnHard ) {
                difficulty = MemoraDbPagerAdapter.HARD;
                iNumHard++;
            } else if( v== btnMed ) {
                difficulty = MemoraDbPagerAdapter.MEDIUM;
                iNumMed++;
            } else if( v == btnEasy ) {
                difficulty = MemoraDbPagerAdapter.EASY;
                iNumEasy++;
            }

            // First check if this word has already been answered
            if( answeredMap.get(currentPage) ){
                // Already answered, replace the latest difficulty in the database
                pagerAdapter.setDifficulty(currentPage, difficulty, true);
                // Move to the next card
                getNextCard();
            } else {
                // Set the difficulty in the database
                pagerAdapter.setDifficulty(currentPage, difficulty, false);
                // Record this word as answered
                answeredMap.put(currentPage, true);
                // Increment the progress bar
                incrementProgressBar();
                // Move to the next card
                getNextCard();
            }
        }
    };


    private void clickFinish() {
        finish();
    }


}