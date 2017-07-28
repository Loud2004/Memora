package com.timothyshaffer.memora.activity;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;


public class StudyCategoryActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String CATEGORY_ID = "com.timothyshaffer.memora.CATEGORY_ID";

    ListView mListView;
    SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_category);
        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView)findViewById(R.id.listView_studyCategory);
        // Show a loading bar while the List is being fetched from the database
        mListView.setEmptyView(findViewById(R.id.loadingList_studyCategory));

        // Add the Header to the ListVIew
        View header = getLayoutInflater().inflate(R.layout.list_header_select_words, null);
        mListView.addHeaderView(header, null, false);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        mAdapter = new SimpleCursorAdapter(
                this, // Context.
                R.layout.list_item_category,    // The row template to use
                null,   // No cursor (use ContentProvider)
                new String[] {"name", "description", "NumOfCards"},    // Array of cursor columns to bind to
                new int[] {R.id.categoryName, R.id.categoryDesc, R.id.numOfWords},
                0); // Parallel array of which template objects to bind to those columns

        // Bind to our new adapter.
        mListView.setAdapter(mAdapter);

        // Creates a loader for populating the ListView from our SQLite Database
        getLoaderManager().initLoader(0, null, this);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Pass the category_id so the Memorize Activity knows which words to use
                String categoryId = String.valueOf(id);
                Intent intent = new Intent(view.getContext(), MemorizeActivity.class);
                intent.putExtra(StudyCategoryActivity.CATEGORY_ID, categoryId);
                // Start the Memorize Activity
                startActivity(intent);

            }
        });
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = MemoraContentProvider.CONTENT_URI_STUDY_CATEGORY;
        return new CursorLoader(this, uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // If the list is empty then show an empty list message instead of the loading bar
        mListView.setEmptyView(findViewById(R.id.emptyList_studyCategory));
        findViewById(R.id.loadingList_studyCategory).setVisibility(View.GONE);
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

}
