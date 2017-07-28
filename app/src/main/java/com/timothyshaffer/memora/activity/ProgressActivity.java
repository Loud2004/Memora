package com.timothyshaffer.memora.activity;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;

import java.text.DecimalFormat;


public class ProgressActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    ListView mListView;
    SimpleCursorAdapter mAdapter;
    Cursor diffCursor;

    // Summary page
    //View summaryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView)findViewById(R.id.listView_progress);
        // Show a loading bar while the List is being fetched from the database
        mListView.setEmptyView(findViewById(R.id.loadingList_progress));

        // Find the summary view for later
        //summaryView = findViewById(R.id.progress_summary);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        mAdapter = new SimpleCursorAdapter(
                this, // Context.
                R.layout.list_item_progress,    // The row template to use
                null,                           // No cursor to bind to (use Loader)
                new String[] {"name", "description", "NumOfCards", "NumMemorized", "_id"},    // Array of cursor columns to bind to
                new int[] {R.id.categoryName, R.id.categoryDesc, R.id.numOfWords, R.id.percentComplete, R.id.progress_list_item_difficulties},
                0); // Parallel array of which template objects to bind to those columns

        // Fix adapter to work with our custom View that includes latest difficulty numbers
        // For latest diff numbers, the rows bind to the view, not the columns
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.percentComplete) {
                    // Bind the progress bar and all the difficulty TextViews
                    TextView percentText = (TextView) view;
                    // Get the total number of cards and the number of cards that are memorized
                    // One of these must be a float so that the percent will be a float
                    float totalCards = cursor.getFloat(cursor.getColumnIndex("NumOfCards"));
                    int memorized = cursor.getInt(columnIndex);
                    // Set the "Progress"
                    float percent = memorized / totalCards;

                    percentText.setText(new DecimalFormat("##.##").format(percent) + "%");
                    // We have bound this View
                    return true;
                } else if (view.getId() == R.id.progress_list_item_difficulties) {
                    // Set all the difficulties
                    // Get the Cursor for all latest difficulties for this category_id

                    diffCursor = getContentResolver().query(
                            MemoraContentProvider.CONTENT_URI_LATEST_DIFFICULTY,
                            null, null, new String[]{cursor.getString(columnIndex)}, null);
                    if (diffCursor == null) {
                        return false;
                    }

                    // Get all the column indices
                    int idxReversed = diffCursor.getColumnIndex(MemoraDbContract.Latest_Card_Diff.COLUMN_NAME_CARD_REVERSED);
                    int idxDifficulty = diffCursor.getColumnIndex(MemoraDbContract.Latest_Card_Diff.COLUMN_NAME_DIFFICULTY);
                    int idxCount = diffCursor.getColumnIndex("diff_count");

                    while (diffCursor.moveToNext()) {
                        boolean reversed = diffCursor.getInt(idxReversed) == 1;
                        int difficulty = diffCursor.getInt(idxDifficulty);
                        String count = diffCursor.getString(idxCount);

                        // Now set the appropriate TextViews
                        // We know that lla the TextViews are children of the current View,
                        // so use view.findViewById()
                        if (!reversed) {
                            // spa to eng
                            switch (difficulty) {
                                case 0:
                                    ((TextView) view.findViewById(R.id.progress_spa_eng_critical_num)).setText(count);
                                    break;
                                case 1:
                                    ((TextView) view.findViewById(R.id.progress_spa_eng_hard_num)).setText(count);
                                    break;
                                case 2:
                                    ((TextView) view.findViewById(R.id.progress_spa_eng_medium_num)).setText(count);
                                    break;
                                case 3:
                                    ((TextView) view.findViewById(R.id.progress_spa_eng_easy_num)).setText(count);
                                    break;
                            }
                        } else {
                            // eng to spa
                            switch (difficulty) {
                                case 0:
                                    ((TextView) view.findViewById(R.id.progress_eng_spa_critical_num)).setText(count);
                                    break;
                                case 1:
                                    ((TextView) view.findViewById(R.id.progress_eng_spa_hard_num)).setText(count);
                                    break;
                                case 2:
                                    ((TextView) view.findViewById(R.id.progress_eng_spa_medium_num)).setText(count);
                                    break;
                                case 3:
                                    ((TextView) view.findViewById(R.id.progress_eng_spa_easy_num)).setText(count);
                                    break;
                            }
                        }

                    }

                    diffCursor.close();

                    // View has been set
                    return true;
                }

                // We did not bind the View
                return false;

            }
        });
        // Bind to our new adapter.
        mListView.setAdapter(mAdapter);

        // Creates a loader for populating the ListView from our SQLite Database
        // Invokes onCreatedLoader() below
        getLoaderManager().initLoader(0, null, this);
/*
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                 @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: Long Press option to reset progress on selected Category

                // Find which row was clicked
                RelativeLayout item = (RelativeLayout) mListView.getChildAt(position);
                for (int i = 0; i < item.getChildCount(); i++) {
                    LinearLayout child = (LinearLayout) item.getChildAt(i);
                    if (child.getId() == R.id.progress_list_item_middle) {
                        // Get a handle to the NavArrowView so we can change its direction later
                        NavArrowView navArrow = (NavArrowView) item.findViewById(R.id.progress_list_item_top).findViewById(R.id.nav_arrow);
                        // Get collapsed status. True if it is collapsed.
                        Boolean tag = (Boolean) child.getTag();
                        // If this View has never been collapsed then set the default tag
                        if (tag == null) {
                            tag = false;
                        }
                        if (!tag) {
                            // Collapse the child View
                            child.setTag(true);
                            collapse(child);
                            navArrow.animate().rotation(180);
                        } else {
                            // Expand the child View
                            child.setTag(false);
                            expand(child);
                            navArrow.animate().rotation(0);
                        }
                    }
                }
            }
        });*/
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = MemoraContentProvider.CONTENT_URI_CATEGORIES_PROGRESS;
        return new CursorLoader(this, uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // If the list is empty then show an empty list message instead of the loading bar
        mListView.setEmptyView(findViewById(R.id.emptyList_progress));
        findViewById(R.id.loadingList_progress).setVisibility(View.GONE);
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


/*
    public void btnHideSummary(View v) {
        summaryView.setVisibility(View.GONE);
        getListView().setVisibility(View.VISIBLE);
    }


    @Override
    public void onBackPressed(){
        // Redisplay the summary if it was hidden
        if( summaryView.getVisibility() == View.GONE ){
            summaryView.setVisibility(View.VISIBLE);
            getListView().setVisibility(View.INVISIBLE);
        } else {
            super.onBackPressed();
        }
    }
*/

/* Expand/Collapse Animations no longer used
    public static Animation expand(final View view) {
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
        final int targetHeight = view.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0 so use 1 instead.
        view.getLayoutParams().height = 1;
        view.setVisibility(View.VISIBLE);

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                view.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);

                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1.5 is the scale factor
        a.setDuration((int) (1.5 * view.getMeasuredHeight() / view.getContext().getResources().getDisplayMetrics().density));
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(a);

        return a;
    }

    public static Animation collapse(final View view) {
        final int initialHeight = view.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1.5 is the scale factor
        a.setDuration((int) (1.5 * view.getMeasuredHeight() / view.getContext().getResources().getDisplayMetrics().density));
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(a);

        return a;
    }

*/
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_progress, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
