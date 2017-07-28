package com.timothyshaffer.memora.fragment;


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.activity.WordActivity;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;

// TODO: Convert to RecyclerView Framgemt
public class WordCategoriesFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Fragment initialization parameters
    private static final String ARG_CARD_ID = "id";

    private long mCardId;

    private SimpleCursorAdapter mAdapter;


    public WordCategoriesFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given Card data
     */
    public static WordCategoriesFragment newInstance(long id) {
        WordCategoriesFragment fragment = new WordCategoriesFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CARD_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mCardId = args.getLong(ARG_CARD_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_word_categories, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Add the Header to the ListView
        View header = getActivity().getLayoutInflater().inflate(R.layout.list_header_categories_word, null);
        getListView().addHeaderView(header, null, false);

        // Create a SimpleCursorAdapter to map values in the Cursor to the ListView Items
        mAdapter = new SimpleCursorAdapter(
                getActivity(),   // Context
                R.layout.list_item_word_categories,   // The row template to use
                null,   // No cursor (use ContentProvider)
                // Array of cursor columns to bind to (_id is used in the custom ViewBinder below)
                new String[]{MemoraDbContract.Category.COLUMN_NAME_NAME,
                        MemoraDbContract.Category.COLUMN_NAME_DESCRIPTION, "selected"},
                // Parallel array of which template objects to bind to those columns
                new int[]{R.id.categoriesWordName, R.id.categoriesWordDesc, R.id.categoriesWordCheckBox},
                0);


        // Fix adapter to work with checkbox
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.categoriesWordCheckBox) {
                    CheckBox checkbox = (CheckBox) view;
                    // Check if the Word is in this Category
                    boolean inCat = cursor.getInt(columnIndex) == 1;
                    // Set the checkbox based on the "selected" column
                    checkbox.setChecked(inCat);

                    // Tell the SimpleCursorAdapter that we manually bound this column
                    return true;
                }
                // We did not bind any View
                return false;
            }
        });

        // Bind to our new adapter.
        setListAdapter(mAdapter);

        // Creates a loader for populating the ListView from our SQLite Database
        // Invokes onCreatedLoader() below
        getLoaderManager().initLoader(0, null, this);

        // Save a list of all the selected Categories (for rollback if changes are discarded)
        saveCategoriesList();
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = MemoraContentProvider.CONTENT_URI_WORD_CATEGORIES;
        return new android.support.v4.content.CursorLoader(
                getActivity(), uri, null, null, new String[]{String.valueOf(mCardId)}, null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    // Save a list of all the selected Categories (for rollback if changes are discarded)
    public void saveCategoriesList(){
        // Clear any previous data
        ((WordActivity)getActivity()).mOriginalCategoriesRollback.clear();
        // Populate the Map with Categories that are already associated with this Card
        Cursor categoriesCursor = getActivity().getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                null,
                MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID + "=?",
                new String[]{String.valueOf(mCardId)},
                null);
        if( categoriesCursor != null ){
            int idxCategoryId = categoriesCursor.getColumnIndex(MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID);
            while( categoriesCursor.moveToNext() ) {
                long categoryId = categoriesCursor.getLong(idxCategoryId);
                ((WordActivity)getActivity()).mOriginalCategoriesRollback.add(categoryId);
            }
            categoriesCursor.close();
        }
    }

    // @param position holds position in the list
    // @param id holds the cursor's _id for this item (card_id)
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Find the checkbox within the View (view should be R.layout.list_item_word_categories)
        CheckBox checkbox = (CheckBox) v.findViewById(R.id.categoriesWordCheckBox);
        // Flip the checkbox to the opposite state (user just clicked it)
        checkbox.toggle();

        // Save/Delete an association of this card_id with the specified Category
        if (checkbox.isChecked()) {
            // Save
            ContentValues values = new ContentValues();
            values.put(MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID, mCardId);
            values.put(MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID, id);
            getActivity().getContentResolver().insert(MemoraContentProvider.CONTENT_URI_CARD_CATEGORY, values);
        } else {
            // Delete
            getActivity().getContentResolver().delete(MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                    MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID + "=? AND "
                            + MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID + "=?",
                    new String[]{String.valueOf(mCardId), String.valueOf(id)});
        }

        // Set the dirty flag
        ((WordActivity)getActivity()).setDirtyState();
    }

}