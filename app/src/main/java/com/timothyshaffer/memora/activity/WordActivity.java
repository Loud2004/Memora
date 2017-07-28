package com.timothyshaffer.memora.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;
import com.timothyshaffer.memora.dialog.AddExampleDialog;
import com.timothyshaffer.memora.dialog.ConfirmDeleteDialog;
import com.timothyshaffer.memora.dialog.ConfirmExitDialog;
import com.timothyshaffer.memora.fragment.WordCategoriesFragment;
import com.timothyshaffer.memora.fragment.WordDataFragment;
import com.timothyshaffer.memora.fragment.WordExamplesFragment;
import com.timothyshaffer.memora.fragment.WordHistoryFragment;

import java.util.ArrayList;

// TODO: Buttons (Save, Delete), Save Categories on Exit (Transactions?), Transactions for Examples
public class WordActivity extends AppCompatActivity
    implements ConfirmExitDialog.OnConfirmExitListener, ConfirmDeleteDialog.OnConfirmDeleteListener {

    // The tag that other Activities should use when passing data (card_id) into this Activity
    public static final String ARG_CARD_ID = "com.timothyshaffer.memora.activity.WordActivity.card_id";
    public static final String ARG_TAB_ID = "com.timothyshaffer.memora.activity.WordActivity.tab_id";

    // The static Fragments that compose this Activity
    public static final int WORD_DATA_FRAGMENT = 0;
    public static final int WORD_CATEGORIES_FRAGMENT = 1;
    public static final int WORD_EXAMPLES_FRAGMENT = 2;
    public static final int WORD_HISTORY_FRAGMENT = 3;
    // The number of static Fragments in this Activity
    private static final int NUM_FRAGMENTS = 4;


    private WordSectionsPagerAdapter mWordSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private Menu mMenu;

    private boolean bDirtyState;     // Track when changes are made so we can tell the user to Save

    // Arguments passed into the Activity
    private long mCardId;
    private String mSpa;
    private String mEng;


    // Map of Categories or Examples that have been added or deleted since last "save" or "dismiss".
    // Any inserts or deletes happen instantly (to keep the ListViews current), when the user
    // "saves" we don't do anything but discard the temporary data. If the user "dismisses changes"
    // then the data is used to re-create the database as it was when the Activity was started.
    // Categories: Delete all Categories associated with this Card, then
    //             Restore an array of all the originally associated Categories (created each "Save")
    // Examples: For added items: Save an array of the inserted Uris to be deleted
    //           For deleted items: Save an array of ContentValues (id, spa, eng) to be re-inserted
    public ArrayList<Long> mOriginalCategoriesRollback;
    public ArrayList<Uri> mAddedExamplesRollback;
    public ArrayList<ContentValues> mDeletedExamplesRollback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word);

        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        }

        // Look for extra Intent data to determine which card we are editing
        // and which tab we should be displaying
        Intent intent = getIntent();
        mCardId = intent.getLongExtra(ARG_CARD_ID, 0L);
        if( mCardId == 0 ) {
            mSpa = "";
            mEng = "";
        } else {
            // Lookup the Card's other data using the id we just received
            Cursor cardCursor = getContentResolver().query(MemoraContentProvider.CONTENT_URI_CARD,
                    new String[]{MemoraDbContract.Card.COLUMN_NAME_SPA, MemoraDbContract.Card.COLUMN_NAME_ENG},
                    MemoraDbContract.Card._ID + " = ?",
                    new String[]{String.valueOf(mCardId)},
                    null);

            if( cardCursor == null ) {
                // ERROR
                finish();
                return;
            }

            int idxSpa = cardCursor.getColumnIndex(MemoraDbContract.Card.COLUMN_NAME_SPA);
            int idxEng = cardCursor.getColumnIndex(MemoraDbContract.Card.COLUMN_NAME_ENG);
            cardCursor.moveToFirst();
            mSpa = cardCursor.getString(idxSpa);
            mEng = cardCursor.getString(idxEng);
            cardCursor.close();

        }

        // Set the ActionBar title
        if( actionBar != null ) {
            if (mSpa.isEmpty()) {
                actionBar.setTitle(getString(R.string.title_new_word));
            } else {
                actionBar.setTitle(mSpa);
                actionBar.setSubtitle(mEng);
            }
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the WordActivity.
        mWordSectionsPagerAdapter = new WordSectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mWordSectionsPagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);   // Get Tab titles from ViewPager
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_word);
        fab.hide(); // Hide for now. The first page does not use the fab yet.
        /*
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout){
            // Hide the fab when scrolling
            /*@Override
            public void onPageScrollStateChanged(int state) {
                if(state==ViewPager.SCROLL_STATE_IDLE){
                    fab.show();
                } else if(state==ViewPager.SCROLL_STATE_DRAGGING) {
                    fab.hide();
                }
            }*/

            // Change the FloatingActionButton's onClick listener for each page
            @Override
            public void	onPageSelected(int position){
                switch (position){
                    case WORD_EXAMPLES_FRAGMENT:
                        // Make sure we have the right fab icon
                        fab.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_add_white_48dp));
                        fab.show();
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                AddExampleDialog dialog = new AddExampleDialog();
                                dialog.setTargetFragment(findFragmentByPosition(WORD_EXAMPLES_FRAGMENT), 0);
                                dialog.show(getSupportFragmentManager(), "AddExampleDialog");
                            }
                        });
                        break;
                    case WORD_HISTORY_FRAGMENT:
                        fab.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_delete_forever));
                        fab.show();
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getContentResolver().delete(MemoraContentProvider.CONTENT_URI_DIFFICULTY,
                                        MemoraDbContract.Card_Difficulty._ID + "=?",
                                        new String[]{String.valueOf(mCardId)});
                                WordHistoryFragment fragment = (WordHistoryFragment) findFragmentByPosition(WORD_HISTORY_FRAGMENT);
                                fragment.clearChart();
                            }
                        });
                        break;
                    case WORD_DATA_FRAGMENT:
                    case WORD_CATEGORIES_FRAGMENT:
                    default:
                        fab.hide();
                }
            }
        });


        // Go directly to a specific tab if it was passed when creating this Activity
        int tab = intent.getIntExtra(ARG_TAB_ID, WORD_DATA_FRAGMENT);
        mViewPager.setCurrentItem(tab);         // Move to the tab

        mOriginalCategoriesRollback = new ArrayList<>();
        mAddedExamplesRollback = new ArrayList<>();
        mDeletedExamplesRollback = new ArrayList<>();

        // Monitor changes to the Card to track when we need to Save
        bDirtyState = false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_word, menu);
        mMenu = menu;
        menu.findItem(R.id.action_save_word).setEnabled(false);    // Initial state of "Save" button
        menu.findItem(R.id.action_save_word).setIcon(R.drawable.ic_save_disabled);
        if( mCardId == 0 ){
            menu.findItem(R.id.action_delete_word).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                // If any data has been altered then allow user to discard the changes
                if( bDirtyState ) {
                    // Display "Discard Changes" confirmation dialog
                    ConfirmExitDialog dialog = new ConfirmExitDialog();
                    dialog.show(getSupportFragmentManager(), "ConfirmExitDialog");
                } else {
                    NavUtils.navigateUpFromSameTask(this);
                    // TODO: Fix "up" arrow transition animation to match other Activities (fade upward). Right now it does the opposite (fade downward)
                    // TODO: all calls to NavUtils.navigateUpFromSameTask() need to be re-routed to the fixed version (in its own function)
                    //finish();
                    //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
                return true;
            case R.id.action_save_word:
                saveChanges();
                return true;
            case R.id.action_delete_word:
                ConfirmDeleteDialog dialog = ConfirmDeleteDialog.newInstance("Card", "<b>" + mSpa + "</b><br><i>" + mEng + "</i>");
                dialog.show(getSupportFragmentManager(), "ConfirmDeleteDialog");
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        discardChanges();
        // TODO: Show SnackBar notification in parent app if possible
        super.onBackPressed();
    }

    public void setDirtyState(){
        if( !bDirtyState ) {
            bDirtyState = true;
            mMenu.findItem(R.id.action_save_word).setEnabled(true);
            mMenu.findItem(R.id.action_save_word).setIcon(R.drawable.ic_save);
            // Change the "up" icon to a "discard changes" icon
            getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        }
    }

    public void clearDirtyState(){
        if( bDirtyState ) {
            bDirtyState = false;
            mMenu.findItem(R.id.action_save_word).setEnabled(false);
            mMenu.findItem(R.id.action_save_word).setIcon(R.drawable.ic_save_disabled);
            // Change the "up" icon back to the original "back arrow" icon
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
    }

    private void saveChanges() {
        // Save changes in the data fragment
        WordDataFragment wordDataFragment = (WordDataFragment) findFragmentByPosition(WORD_DATA_FRAGMENT);
        String spa = wordDataFragment.getSpaText();
        String eng = wordDataFragment.getEngText();
        // Check for bad user input
        if( spa.isEmpty() || eng.isEmpty() ){
            // Do nothing; Notify the user that no changes were made
            Snackbar.make(findViewById(R.id.content_word), getString(R.string.blank_error), Snackbar.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MemoraDbContract.Card.COLUMN_NAME_SPA, spa);
        values.put(MemoraDbContract.Card.COLUMN_NAME_ENG, eng);
        if (mCardId == 0) {
            // Insert a new card and store the returned id
            Uri uriNewCard = getContentResolver().insert(MemoraContentProvider.CONTENT_URI_CARD, values);

            // Reset the Activity (using the data just saved) to a state where we can now
            // edit the Card that was just saved (allow adding Categories and Examples)
            mCardId = Long.valueOf(uriNewCard.getLastPathSegment());
            mSpa = spa;
            mEng = eng;
            // Show the other tabs now that the word is created
            mWordSectionsPagerAdapter.notifyDataSetChanged();
            mTabLayout.setTabsFromPagerAdapter(mWordSectionsPagerAdapter);
            mMenu.findItem(R.id.action_delete_word).setEnabled(true);
        } else {
            // Update an existing card
            getContentResolver().update(
                    Uri.parse(MemoraContentProvider.CONTENT_URI_CARD + "/" + String.valueOf(mCardId)),
                    values, null, null );
        }

        // Tell the fragment to update itself with the current input
        wordDataFragment.saveCard();

        // Update the title and subtitle
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            actionBar.setTitle(spa);
            actionBar.setSubtitle(eng);
        }

        // Confirm changes made in Categories and Examples Fragments
        // Save by re-saving list
        ((WordCategoriesFragment)findFragmentByPosition(WORD_CATEGORIES_FRAGMENT)).saveCategoriesList();
        // Save by discarding the rollback data
        mAddedExamplesRollback.clear();
        mDeletedExamplesRollback.clear();

        // Display a success message to the user
        Snackbar.make(mViewPager, "Save Successful", Snackbar.LENGTH_SHORT).show();
        clearDirtyState();      // Clear dirty flag
    }

    // Undo any changes made since the last Save. The Categories and Examples tabs save as they go,
    // so they need their changes rolled back. The word data tab does make changes until "Save"
    // is pressed, so it doesn't need anything rolled back. Just ignore changes made in that tab.
    private void discardChanges() {
        // Rollback changes made to the Categories tab (since there are relatively few Categories,
        // we will just delete all rows and then re-insert the rows that were originally there)
        // First Delete all rows associated with this Card. We will re-save the data later.
        getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID + "=?",
                new String[]{String.valueOf(mCardId)});

        // Now associate original Categories
        // Convert category_ids to ContentValues that insert can use
        ArrayList<ContentValues> valuesArray = new ArrayList<>();
        for( long category_id : mOriginalCategoriesRollback ){
            ContentValues values = new ContentValues();
            values.put(MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID, mCardId);
            values.put(MemoraDbContract.Card_Category.COLUMN_NAME_CATEGORY_ID, category_id);
            valuesArray.add(values);
        }
        // WORKAROUND: Android won't allow this cast: (ContentValues[]) valuesArray.toArray()
        // So create a new (basic) array [using the alt. version of toArray()] instead of casting.
        ContentValues[] categoriesInsertArray = new ContentValues[valuesArray.size()];
        categoriesInsertArray = valuesArray.toArray(categoriesInsertArray);
        getContentResolver().bulkInsert(MemoraContentProvider.CONTENT_URI_CARD_CATEGORY, categoriesInsertArray);


        // Rollback changes in the Examples tab
        // Delete any newly created Examples
        for( Uri uri : mAddedExamplesRollback ){
            getContentResolver().delete(uri, null, null);
        }
        // Re-create any deleted Examples
        // WORKAROUND: Android won't allow this cast: (ContentValues[]) valuesArray.toArray()
        // So create a new (basic) array [using the alt. version of toArray()] instead of casting.
        ContentValues[] insertArray = new ContentValues[mDeletedExamplesRollback.size()];
        insertArray = mDeletedExamplesRollback.toArray(insertArray);
        getContentResolver().bulkInsert(MemoraContentProvider.CONTENT_URI_EXAMPLE, insertArray);
    }

    @Override
    public void onConfirmExitSave() {
        saveChanges();
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public void onConfirmExitDiscard() {
        discardChanges();
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public void onConfirmDelete() {
        // Delete all Examples associated with this card_id
        getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_EXAMPLE,
                MemoraDbContract.Example.COLUMN_NAME_CARD_ID + "=?",
                new String[] {String.valueOf(mCardId)}
        );
        // Delete all Category associations
        getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_CARD_CATEGORY,
                MemoraDbContract.Card_Category.COLUMN_NAME_CARD_ID + "=?",
                new String[] {String.valueOf(mCardId)}
        );
        // Delete Difficulty
        getContentResolver().delete(
                MemoraContentProvider.CONTENT_URI_DIFFICULTY,
                MemoraDbContract.Card_Difficulty._ID + "=?",
                new String[] {String.valueOf(mCardId)}
        );
        // TODO: Delete Metadata (once it is implemented)
        // TODO: Delete Pronunciation (once it is implemented)
        // Delete the Card
        getContentResolver().delete(
                Uri.parse(MemoraContentProvider.CONTENT_URI_CARD + "/" + String.valueOf(mCardId)),
                null, null
        );
        // Exit the Card editor since we just deleted the Card
        NavUtils.navigateUpFromSameTask(this);
    }

    // Helper function to get the child fragments based on their position.
    // FragmentPagerAdapter auto-generates each fragment's tag using
    // syntax that is re-created in this function.
    public Fragment findFragmentByPosition(int position) {
        return getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + mViewPager.getId() + ":"
                        + mWordSectionsPagerAdapter.getItemId(position));
    }


    /**
     * A FragmentPagerAdapter that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class WordSectionsPagerAdapter extends FragmentPagerAdapter {

        public WordSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case WORD_DATA_FRAGMENT:
                    return WordDataFragment.newInstance(mCardId, mSpa, mEng);
                case WORD_CATEGORIES_FRAGMENT:
                    return WordCategoriesFragment.newInstance(mCardId);
                case WORD_EXAMPLES_FRAGMENT:
                    return WordExamplesFragment.newInstance(mCardId);
                case WORD_HISTORY_FRAGMENT:
                    return WordHistoryFragment.newInstance(mCardId);
            }
            return null;
        }

        @Override
        public int getCount() {
            // Three Pages: Card Data, Categories, Examples
            if( mCardId == 0 ){
                return 1;
            }
            return NUM_FRAGMENTS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case WORD_DATA_FRAGMENT:
                    return "Card";  // "Card Data"
                case WORD_CATEGORIES_FRAGMENT:
                    return "Categories";
                case WORD_EXAMPLES_FRAGMENT:
                    return "Examples";
                case WORD_HISTORY_FRAGMENT:
                    return "History";
            }
            return null;
        }
    }


}
