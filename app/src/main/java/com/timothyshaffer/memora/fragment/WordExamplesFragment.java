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
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.activity.WordActivity;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;
import com.timothyshaffer.memora.dialog.DeleteExampleDialog;

/**
 * A simple {@link ListFragment} subclass.
 * Use the {@link WordExamplesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WordExamplesFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Fragment initialization parameters
    private static final String ARG_CARD_ID = "id";

    private long mCardId;

    private SimpleCursorAdapter mAdapter;


    public WordExamplesFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given Card data
     */
    public static WordExamplesFragment newInstance(long id) {
        WordExamplesFragment fragment = new WordExamplesFragment();
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
        return inflater.inflate(R.layout.fragment_word_examples, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create a SimpleCursorAdapter to map values in the Cursor to the ListView Items
        mAdapter = new SimpleCursorAdapter(
                getActivity(),   // Context.
                R.layout.list_item_word_examples,   // The row template to use
                null,   // No cursor (use ContentProvider)
                // Array of cursor columns to bind to (_id is used in the custom ViewBinder below)
                new String[]{MemoraDbContract.Example.COLUMN_NAME_SPA,
                        MemoraDbContract.Example.COLUMN_NAME_ENG},
                // Parallel array of which template objects to bind to those columns
                new int[]{R.id.example_spa, R.id.example_eng},
                0);

        // Bind to our new adapter.
        setListAdapter(mAdapter);

        // Delete list items when long clicked
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                DeleteExampleDialog dialog = new DeleteExampleDialog();
                Bundle args = new Bundle();
                args.putLong(DeleteExampleDialog.ARG_EXAMPLE_ID, id);
                dialog.setArguments(args);
                dialog.setTargetFragment(WordExamplesFragment.this, 0);
                dialog.show(getFragmentManager(), "DeleteExampleDialog");
                return true;
            }
        });

        // Creates a loader for populating the ListView from our SQLite Database
        // Invokes onCreatedLoader() below
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = MemoraContentProvider.CONTENT_URI_EXAMPLE;
        return new android.support.v4.content.CursorLoader(
                getActivity(), uri,
                new String[] {MemoraDbContract.Example._ID, MemoraDbContract.Example.COLUMN_NAME_SPA, MemoraDbContract.Example.COLUMN_NAME_ENG},
                MemoraDbContract.Example.COLUMN_NAME_CARD_ID + "=?",
                new String[]{String.valueOf(mCardId)},
                null );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    // When the AddExampleDialog finishes it will callback here with
    // the data the user entered into the dialog box.
    // The original dialog needs to setTargetFragment to this WorExamplesFragment
    public void onAddExample(String spa, String eng) {
        ContentValues values = new ContentValues();
        values.put(MemoraDbContract.Example.COLUMN_NAME_CARD_ID, mCardId);
        values.put(MemoraDbContract.Example.COLUMN_NAME_SPA, spa);
        values.put(MemoraDbContract.Example.COLUMN_NAME_ENG, eng);
        Uri uriExample = getActivity().getContentResolver().insert(MemoraContentProvider.CONTENT_URI_EXAMPLE, values);

        // Now save the new Uri in case we need to delete the row later
        ((WordActivity)getActivity()).mAddedExamplesRollback.add(uriExample);

        // Set the dirty flag
        ((WordActivity)getActivity()).setDirtyState();
    }

    public void onDeleteExample(long example_id) {
        ContentValues values = new ContentValues();
        Cursor tempCursor = getActivity().getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_EXAMPLE,
                new String[]{MemoraDbContract.Example.COLUMN_NAME_SPA, MemoraDbContract.Example.COLUMN_NAME_ENG},
                MemoraDbContract.Example._ID + "=?",
                new String[]{String.valueOf(example_id)},
                null);
        // TODO: use if != null instead of assert
        assert tempCursor != null;
        int idxSpa = tempCursor.getColumnIndex(MemoraDbContract.Example.COLUMN_NAME_SPA);
        int idxEng = tempCursor.getColumnIndex(MemoraDbContract.Example.COLUMN_NAME_ENG);
        tempCursor.moveToFirst();

        values.put(MemoraDbContract.Example._ID, example_id);
        values.put(MemoraDbContract.Example.COLUMN_NAME_CARD_ID, mCardId);
        values.put(MemoraDbContract.Example.COLUMN_NAME_SPA, tempCursor.getString(idxSpa));
        values.put(MemoraDbContract.Example.COLUMN_NAME_ENG, tempCursor.getString(idxEng));
        tempCursor.close();
        ((WordActivity)getActivity()).mDeletedExamplesRollback.add(values);

        getActivity().getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_EXAMPLE,
                MemoraDbContract.Example._ID + "=?",
                new String[]{String.valueOf(example_id)});

        // Set the dirty flag
        ((WordActivity)getActivity()).setDirtyState();
    }


}
