package com.timothyshaffer.memora.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;
import com.timothyshaffer.memora.view.DividerItemDecoration;
import com.timothyshaffer.memora.view.recyclerview_fastscroller.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddWordsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener {
    // The tag that other Activities should use when passing data (card_id) into this Activity
    public static final String ARG_CATEGORY_ID = "id";

    private static final String ARG_SELECTION = "selection";

    private CoordinatorLayout mCoordinatorLayout;   // For showing Snackbar messages
    private FastScrollRecyclerView mRecyclerView;
    private TextView mEmptyListView;

    // Variables to hold info on the Category we are adding the Words to.
    private long mCategoryId;
    private String mCategoryName;
    private String mCategoryDesc;

    // Track if we are showing the full list of words or a filtered list
    private boolean mFilterWords = false;

    // Hold a map of each id and a boolean to show which rows/cards are selected
    private HashMap<Long, Boolean> saveMap;

    private WordAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_words);
        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the root CoordinatorLayout for showing the Snackbar messages
        mCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.content_addWords);
        // Setup the list (RecyclerView)
        mEmptyListView = (TextView)findViewById(R.id.emptyList_addWords);
        mRecyclerView = (FastScrollRecyclerView)findViewById(R.id.recyclerView_addWords);
        //mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set the rows to use a custom divider (1dp grey line)
        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        // Look for extra Intent data to determine which category will be associated with the cards chosen.
        Intent intent = getIntent();
        mCategoryId = intent.getLongExtra(ARG_CATEGORY_ID, 0);
        if( mCategoryId != 0 ) {
            // Lookup the Category's other data using the id we just received
            Cursor categoryCursor = getContentResolver().query(MemoraContentProvider.CONTENT_URI_CATEGORY,
                    new String[]{MemoraDbContract.Category.COLUMN_NAME_NAME, MemoraDbContract.Category.COLUMN_NAME_DESCRIPTION},
                    MemoraDbContract.Category._ID + " = ?",
                    new String[]{String.valueOf(mCategoryId)},
                    null);

            if( categoryCursor == null ) {
                // ERROR
                finish();
                return;
            }

            // Get the Category's name and description
            int idxName = categoryCursor.getColumnIndex(MemoraDbContract.Category.COLUMN_NAME_NAME);
            int idxDesc = categoryCursor.getColumnIndex(MemoraDbContract.Category.COLUMN_NAME_DESCRIPTION);
            categoryCursor.moveToFirst();   // Cursor should hold a single result
            mCategoryName = categoryCursor.getString(idxName);
            mCategoryDesc = categoryCursor.getString(idxDesc);  // Currently unused
            categoryCursor.close();
        }


        // Set the toolbar subtitle
        toolbar.setSubtitle(mCategoryName);

        // Setup the adapter to convert data from the Loader into usable Views
        mAdapter = new WordAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);

        // Creates a loader for populating the ListView from our SQLite Database
        // Invokes onCreatedLoader() below
        getLoaderManager().initLoader(0, null, this);


        // Set Floating Action Button click listener
        findViewById(R.id.fab_addWords).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), WordActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create a projection based on what we are searching for
        String[] projectionArgs;
        if( args == null ) {
            // Empty search if there were no args passed (should match everything)
            projectionArgs = new String[]{String.valueOf(mCategoryId), "%", "%"};
        } else {
            // Search in both the spa and eng columns using the query string
            // plus any number of character after the query string.
            projectionArgs = new String[]{String.valueOf(mCategoryId),
                    args.getString(ARG_SELECTION) + "%", args.getString(ARG_SELECTION) + "%"};
        }

        if( !mFilterWords ) { //mFilterWords == false
            Uri uri = MemoraContentProvider.CONTENT_URI_CARD_CATEGORIES;
            return new CursorLoader(this, uri, null, null, projectionArgs, null);
        } else {    // mFilterWords == true
            Uri uri = MemoraContentProvider.CONTENT_URI_CARD_CATEGORIES_FILTER;
            return new CursorLoader(this, uri, null, null, projectionArgs, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // If the list is empty then show an empty list message instead of the RecyclerView
        if( data.getCount() == 0 ) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyListView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyListView.setVisibility(View.GONE);
        }
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Populate the saveMap with Cards that are already associated with this Category
        // Performed in onResume so we can (re)check the card_category table (in case any
        // words were deleted in WordActivity).
        saveMap = new HashMap<>();
        Cursor prevCardsCursor = getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                null,
                MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID + "=?",
                new String[]{String.valueOf(mCategoryId)},
                null);
        if( prevCardsCursor != null ){
            int idxCardId = prevCardsCursor.getColumnIndex(MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID);
            while( prevCardsCursor.moveToNext() ) {
                long cardId = prevCardsCursor.getLong(idxCardId);
                saveMap.put(cardId, true);
            }
            prevCardsCursor.close();
        }
    }

    // Save all the data in the saveMap when the user navigates away from the Activity
    @Override
    protected void onStop() {
        super.onStop();
        saveCardCategories();
    }


    // Save the current selection inside the saveMap to the database
    private void saveCardCategories() {
        // TODO: boolean indicator if user made any changes so we aren't deleting/saving EVERY TIME
        // Return early if "Dismiss Changes" action was selected
        if( saveMap.isEmpty() ) {
            return;
        }

        // First Delete all rows associated with this Category. We will re-save the data later.
        getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID + "=?",
                new String[]{String.valueOf(mCategoryId)});

        // Check if there are any selected rows in the ListView
        List<ContentValues> valuesArray = new ArrayList<>();
        for( Map.Entry<Long, Boolean> saveEntry : saveMap.entrySet() ){
            // Only uses list items that are still checked (e.g. getValue() == true)
            // value can be false if the user unchecked the row
            if(saveEntry.getValue()){
                // Found a checked row, setup a ContentValue map to be used for inserting this row
                ContentValues values = new ContentValues();
                values.put(MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID, mCategoryId);
                values.put(MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID, saveEntry.getKey());
                valuesArray.add(values);
            }
        }

        // Perform the bulkInsert if we found at least one row to be inserted.
        if( !valuesArray.isEmpty() ) {
            // Now associate any selected rows with this Category
            // WORKAROUND: Android won't allow this cast: (ContentValues[]) valuesArray.toArray()
            // So create a new (basic) array [using the alt. version of toArray()] instead of casting.
            ContentValues[] insertArray = new ContentValues[valuesArray.size()];
            insertArray = valuesArray.toArray(insertArray);
            getContentResolver().bulkInsert(MemoraContentProvider.CONTENT_URI_CARD_CATEGORY, insertArray);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_words, menu);
        if( mCategoryId == 0 ){
            // disable "Dismiss Changes"
            menu.getItem(0).setEnabled(false);
            menu.getItem(0).setVisible(false);
            // disable "Filter Words"
            menu.getItem(1).setEnabled(false);
            menu.getItem(1).setVisible(false);
        }

        // Setup the Search bar in the ActionBar
        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_dismiss) {
            saveMap.clear();
            finish();
            return true;
        } else if( id == R.id.action_filter ) {
            if( mFilterWords ) {
                Snackbar.make(mCoordinatorLayout, R.string.add_words_no_filter, Snackbar.LENGTH_SHORT)
                        .show();
                mFilterWords = false;
            } else {
                Snackbar.make(mCoordinatorLayout, R.string.add_words_filter, Snackbar.LENGTH_LONG)
                        .show();
                mFilterWords = true;
            }
            // Save the current progress
            saveCardCategories();

            // Re-start the loader
            getLoaderManager().restartLoader(0, null, this);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onQueryTextChange(String query) {
        // Restart the Loader with a projection using the search text

        // Create a bundle to pass to restartLoader()
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SELECTION, query);

        getLoaderManager().restartLoader(0, bundle, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // The query is performed when the text is changed. There is no separate submit function.
        return false;
    }




    // Adapts data from the Database to Views being inflated by the RecyclerView
    public class WordAdapter extends RecyclerView.Adapter<WordAdapter.ViewHolder> {
        Cursor dataCursor;
        Context context;

        // Holds the data for each View (one row in the list)
        public class ViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener, View.OnLongClickListener {

            private View rootView;  // Used to change background color
            private long cardId;
            private TextView viewSpa;
            private TextView viewEng;
            private CheckBox checkSelected;

            public ViewHolder(View itemView) {
                super(itemView);
                rootView = itemView;
                viewSpa = (TextView) itemView.findViewById(R.id.spaWord);
                viewEng = (TextView) itemView.findViewById(R.id.engWord);
                checkSelected = (CheckBox) itemView.findViewById(R.id.wordCheckBox);
                // Handle our own click events
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (mCategoryId == 0) {
                    // Don't perform clicks when checkboxes are disables (no category_id)
                    return;
                }
                // Flip the checkbox to the opposite state
                checkSelected.toggle();
                // Save the card_id for later (update the card_list table on exit)
                saveMap.put(cardId, checkSelected.isChecked());
            }

            @Override
            public boolean onLongClick(View v) {
                // Send the card_id to WordActivity
                Intent intent = new Intent(context, WordActivity.class);
                intent.putExtra(WordActivity.ARG_CARD_ID, cardId);
                startActivity(intent);

                return true;
            }

        }

        public WordAdapter(Activity mContext, Cursor cursor) {
            dataCursor = cursor;
            context = mContext;
        }

        @Override
        public WordAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_add_words, parent, false);
            return new ViewHolder(rowView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // Move to the correct row in the Cursor
            dataCursor.moveToPosition(position);
            // Get an index for each column that we need data from for this row
            int idxId = dataCursor.getColumnIndex(MemoraDbContract.Card._ID);
            int idxSpa = dataCursor.getColumnIndex(MemoraDbContract.Card.COLUMN_NAME_SPA);
            int idxEng = dataCursor.getColumnIndex(MemoraDbContract.Card.COLUMN_NAME_ENG);
            // TODO: Find a way to use a Contract to get joined/aliased column names
            int idxSelected = dataCursor.getColumnIndex("in_category");

            // Get the data for this cursor row and save/display it
            long cardId = dataCursor.getLong(idxId);
            holder.cardId =  cardId;    // Needed for click events
            holder.viewSpa.setText(dataCursor.getString(idxSpa));
            holder.viewEng.setText(dataCursor.getString(idxEng));

            // Set the checkbox
            if (mCategoryId == 0) {
                // disable all checkboxes when we are not adding to a specific Category
                holder.checkSelected.setEnabled(false);
            }

            // Check for selected state in the database
            // Set to 'true' if equal to 1 (there is no getBoolean() in Cursor)
            boolean selected = dataCursor.getInt(idxSelected) == 1;
            // Check if this item was previously (dis)selected by the user in this session
            Boolean value = saveMap.get(cardId);
            if (value != null) {
                selected = value;
            }
            holder.checkSelected.setChecked(selected);

            // Set the background to red if the word doesn't belong to any Categories
            int numCategories = dataCursor.getInt(dataCursor.getColumnIndex("num_categories"));
            // Shade the background color if this word does not belong to any Categories
            if (numCategories == 0) {
                holder.rootView.setBackgroundResource(R.drawable.card_background_red_tint);
            } else {
                holder.rootView.setBackgroundResource(R.drawable.card_background);
            }
        }

        @Override
        public int getItemCount() {
            return (dataCursor == null) ? 0 : dataCursor.getCount();
        }

        // Copy the functionality of a SimpleCursorAdapter so we can add
        // the Cursor after this adapter class is instantiated
        public Cursor swapCursor(Cursor cursor) {
            // If the given new Cursor is the same instance as the previously set Cursor, then return null
            if (dataCursor == cursor) {
                return null;
            }
            Cursor oldCursor = dataCursor;
            this.dataCursor = cursor;
            if (cursor != null) {
                this.notifyDataSetChanged();
            }
            // Return the previously set Cursor, or null if there was not one
            return oldCursor;
        }
    }


}