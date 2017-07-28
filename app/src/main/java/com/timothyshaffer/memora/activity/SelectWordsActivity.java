package com.timothyshaffer.memora.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;
import com.timothyshaffer.memora.dialog.ConfirmDeleteDialog;

public class SelectWordsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, ConfirmDeleteDialog.OnConfirmDeleteListener {

    // TODO: Unused?
    public static final String CATEGORY_BUNDLE = "com.timothyshaffer.memora.CATEGORY_BUNDLE";

    ListView mListView;
    SimpleCursorAdapter mAdapter;
    ActionMode actionMode = null;   // Track if the Contextual Action Bar is active
    long selectedId;                // The currently selected item (for the Contextual Action Bar)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_words);
        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView) findViewById(R.id.listView_selectWords);
        // Show a loading bar while the List is being fetched from the database
        mListView.setEmptyView(findViewById(R.id.loadingList_selectWords));

        // Add the Header to the ListView
        View header = getLayoutInflater().inflate(R.layout.list_header_select_words, null);
        mListView.addHeaderView(header, null, false);

        // Create a SimpleCursorAdapter to map values in the Cursor to the ListView Items
        mAdapter = new SimpleCursorAdapter(
                this,   // Context.
                R.layout.list_item_select_words,   // The row template to use
                null,   // No cursor (use ContentProvider)
                // Array of cursor columns to bind to
                new String[]{"name", "description", "NumOfCards", "selected"},
                // Parallel array of which template objects to bind to those columns
                new int[]{R.id.categoryName, R.id.categoryDesc, R.id.numOfWords, R.id.selected},
                0);

        // Fix adapter to work with checkbox and progress bar
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.selected) {
                    // Set the checked state
                    CheckBox checkbox = (CheckBox) view;
                    // Get the selection and set the checkbox appropriately
                    int selected = cursor.getInt(columnIndex);
                    if (selected == 1) {
                        checkbox.setChecked(true);
                    } else {
                        checkbox.setChecked(false);
                    }
                    // We have bound this View
                    return true;
                } else {
                    // We did not bind the View
                    return false;
                }
            }
        });
        // Bind to our new adapter.
        mListView.setAdapter(mAdapter);

        // Creates a loader for populating the ListView from our SQLite Database
        // Invokes onCreatedLoader() below
        getLoaderManager().initLoader(0, null, this);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // @param position holds position in the list
                // @param id holds the cursor's _id for this item
                // First find the checkbox within the View (view should be R.layout.list_item_select_words)
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.selected);
                // Flip the checkbox to the opposite state
                checkbox.toggle();

                if( checkbox.isChecked() ) {
                    // Add this category to the list of selected categories
                    ContentValues values = new ContentValues();
                    values.put(MemoraDbContract.Selected_Categories._ID, id);
                    getContentResolver().insert(
                            MemoraContentProvider.CONTENT_URI_SELECTED_CATEGORIES, values);
                } else {
                    // Remove this from the list of selected Categories
                    getContentResolver().delete(
                            Uri.parse(MemoraContentProvider.CONTENT_URI_SELECTED_CATEGORIES
                                    + "/" + id),
                            null,   // WHERE
                            null    // WHERE ARGS
                    );
                }
            }

        });


        // Set the "Long Click" listener
        // The Multi Choice Listener is used, but the Listener will ensure
        // that only one List Item is ever selected at any time
        // NOTE: Multi Choice Mode is required to have a Contextual Action Bar
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                  long id, boolean checked) {
                // Store the id of the selected row if this item is being selected
                if( checked ) {
                    selectedId = id;
                }

                // Check for any previously selected Items and deselect them
                // Exit early if this is the first item selected (most of the time the user
                // will not select a second item)
                if (mListView.getCheckedItemCount() > 1) {
                    // Get array of positions that _MAY_ be checked
                    SparseBooleanArray checkArray = mListView.getCheckedItemPositions();
                    // Iterate through each entry in the SparseBooleanArray
                    for( int i = 0; i < checkArray.size(); i++ ){
                        // Check if the value is true, indicating that this key is/was checked
                        if( checkArray.valueAt(i) ){
                            // Is it NOT the item that was just clicked
                            if( position != checkArray.keyAt(i) ){
                                // Un-Select the row
                                mListView.setItemChecked(checkArray.keyAt(i), false);
                            }
                        }
                    }
                }
            }

            // Respond to clicks on the Contextual Action Bar (CAB)
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                // Get the Category Title and Description for the selected id
                Cursor nameCursor = getContentResolver().query(MemoraContentProvider.CONTENT_URI_CATEGORY,
                        new String[]{MemoraDbContract.Category.COLUMN_NAME_NAME, MemoraDbContract.Category.COLUMN_NAME_DESCRIPTION},
                        MemoraDbContract.Category._ID + " = ?",
                        new String[]{String.valueOf(selectedId)},
                        null);

                if (nameCursor == null) {
                    // ERROR
                    return false;
                }

                // SUCCESS, We found the id for this Category
                int idxName = nameCursor.getColumnIndex(MemoraDbContract.Category.COLUMN_NAME_NAME);
                int idxDesc = nameCursor.getColumnIndex(MemoraDbContract.Category.COLUMN_NAME_DESCRIPTION);
                nameCursor.moveToFirst();   // First and only row in the Cursor
                String name = nameCursor.getString(idxName);
                String desc = nameCursor.getString(idxDesc);
                nameCursor.close();


                // Handle the actual button press
                switch (item.getItemId()) {
                    case R.id.context_bar_cat_add:
                        // Set the Category's _id for the new Activity
                        Intent intent = new Intent(getApplicationContext(), AddWordsActivity.class);
                        intent.putExtra(AddWordsActivity.ARG_CATEGORY_ID, selectedId);
                        startActivity(intent);

                        // Action picked, so close the CAB
                        mode.finish();
                        return true;
                    case R.id.context_bar_cat_edit:
                        // Show Dialog to edit a Category
                        DialogFragment dialogEdit = new EditCategoryDialog();
                        // Add the id, name, and desc to a bundle for easier access inside the dialog
                        Bundle bundle = new Bundle();
                        bundle.putLong("id", selectedId);
                        bundle.putString("name", name);
                        bundle.putString("desc", desc);
                        dialogEdit.setArguments(bundle);
                        dialogEdit.show(getFragmentManager(), "dialogEditCategory");

                        mode.finish();
                        return true;
                    case R.id.context_bar_cat_delete:
                        // Show a confirm delete dialog for the Category that is selected
                        ConfirmDeleteDialog dialog = ConfirmDeleteDialog.newInstance("Category", "<b>" + name + "</b><br /><i>" + desc + "</i>");
                        dialog.show(getSupportFragmentManager(), "ConfirmDeleteDialog");

                        // Action picked, so close the CAB
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
                // Should never get here, 'default' switch statement should return false
                //return false;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Enables tracking the action mode from outside the listener
                actionMode = mode;
                // Inflate the menu for the CAB
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.context_categories, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Here you can make any necessary updates to the activity when
                // the CAB is removed. By default, selected items are deselected/unchecked.
                actionMode = null;
                mListView.clearChoices();
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Here you can perform updates to the CAB due to
                // an invalidate() request
                return false;
            }
        });
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = MemoraContentProvider.CONTENT_URI_SELECT_WORDS_LIST;
        return new CursorLoader(this, uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // If the list is empty then show an empty list message instead of the loading bar
        mListView.setEmptyView(findViewById(R.id.emptyList_selectWords));
        findViewById(R.id.loadingList_selectWords).setVisibility(View.GONE);
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    public void insertNewCategory(String name, String desc) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", desc);

        // TODO: Error message
        // Check for blank name input
        if( !name.isEmpty() ) {
            getContentResolver().insert(MemoraContentProvider.CONTENT_URI_CATEGORY, values);
        }
    }

    public void editPrevCategory(long id, String newName, String newDesc) {
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("description", newDesc);

        // TODO: Error message
        // Check for blank name input
        if( !newName.isEmpty() ) {
            getContentResolver().update(
                    Uri.parse(MemoraContentProvider.CONTENT_URI_CATEGORY + "/" + id),
                    values, null, null);
        }
    }

    // Set from XML as onClick method for the Add Button
    public void showDialogAddCategory(View v) {
        // Stop the Contextual Action Bar if it is enabled
        if (actionMode != null) {
            actionMode.finish();
        }
        DialogFragment dialog = new AddCategoryDialog();
        dialog.show(getFragmentManager(), "dialogAddCategory");
    }

    public static class AddCategoryDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setTitle(R.string.add_category_header)
                    .setView(inflater.inflate(R.layout.dialog_add_edit_category, null))
                    .setNegativeButton(R.string.add_edit_category_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getDialog().cancel();
                                }
                            }
                    )
                    .setPositiveButton(R.string.add_category_create,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    EditText editName = (EditText) getDialog().findViewById(R.id.input_cat_name);
                                    String name = editName.getText().toString().trim();
                                    EditText editDesc = (EditText) getDialog().findViewById(R.id.input_cat_desc);
                                    String desc = editDesc.getText().toString().trim();
                                    ((SelectWordsActivity) getActivity()).insertNewCategory(name, desc);
                                }
                            }
                    );
            return builder.create();

        }
    }


    public static class EditCategoryDialog extends DialogFragment {
        private long mId;
        private String mPrevName;
        private String mPrevDesc;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle bundle = this.getArguments();
            mId = bundle.getLong("id");
            mPrevName = bundle.getString("name");
            mPrevDesc = bundle.getString("desc");

            // Inflate and pass null as the parent view because its going in the dialog layout
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View addEditView = inflater.inflate(R.layout.dialog_add_edit_category, null);
            // Set the previous name/description
            EditText editTextName = (EditText) addEditView.findViewById(R.id.input_cat_name);
            EditText editTextDesc = (EditText) addEditView.findViewById(R.id.input_cat_desc);
            editTextName.setText(mPrevName);
            editTextDesc.setText(mPrevDesc);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.edit_category_header)
                    .setView(addEditView)
                    .setNegativeButton(R.string.add_edit_category_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getDialog().cancel();
                                }
                            }
                    )
                    .setPositiveButton(R.string.edit_category_save,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    EditText editName = (EditText) getDialog().findViewById(R.id.input_cat_name);
                                    String name = editName.getText().toString().trim();
                                    EditText editDesc = (EditText) getDialog().findViewById(R.id.input_cat_desc);
                                    String desc = editDesc.getText().toString().trim();
                                    ((SelectWordsActivity) getActivity()).editPrevCategory(mId, name, desc);
                                }
                            }
                    );
            return builder.create();

        }
    }


    @Override
    public void onConfirmDelete(){
        // The last selected id should still be the id for the Category we are deleting
        getContentResolver().delete(
                Uri.parse(MemoraContentProvider.CONTENT_URI_CATEGORY + "/" + selectedId), null, null);
        // Delete any cards associated with this Category
        getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID + "=?",
                new String[]{String.valueOf(selectedId)}
        );

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_words, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_words) {
            Intent intent = new Intent(this, AddWordsActivity.class);
            // Start AddWordsActivity without a category_id
            intent.putExtra(AddWordsActivity.ARG_CATEGORY_ID, 0);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
