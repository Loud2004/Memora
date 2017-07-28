package com.timothyshaffer.memora.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.view.CardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * ViewPager Adapter that queries an SQLite database and populates objects from that database.
 */
public class MemoraDbPagerAdapter extends PagerAdapter {

    public static final int CRITICAL = 0;
    public static final int HARD = 1;
    public static final int MEDIUM = 2;
    public static final int EASY = 3;

    private SQLiteDatabase memoraDb;
    private Cursor cursor;
    // User Preferences key/value pairs
    SharedPreferences sharedPref;

    // The context_categories to use for creating the Views
    private Context mContext;

    // Number of rows in the Cursor
    private int iRowCount;
    // Each value represents the index into the cursor.
    // This allows shuffling/randomization. Also allows greater memory efficiency
    // by only storing the index into the Cursor instead of the entire Card.
    private LinkedList<Integer> cursorIndices;

    public MemoraDbPagerAdapter(Context context, int categoryId) {
        // Save the context_categories
        mContext = context;

        // TODO: Convert to ContentResolver so we won't need direct access to DB anymore
        // Setup database
        MemoraDbHelper memoraDbHelper = new MemoraDbHelper(mContext);
        memoraDb = memoraDbHelper.getWritableDatabase();
        // Get a handle for storing simple key/value pairs for the User's preferences
        sharedPref = mContext.getSharedPreferences(
                mContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        // Get the User's preferences
        /*boolean bShowSingleLang = sharedPref.getBoolean(
                mContext.getString(R.string.bShowSingleLang), true);
        boolean bShowEng = sharedPref.getBoolean(
                mContext.getString(R.string.bShowEng), false);*/

        // Initialize the index array
        //noinspection Convert2Diamond
        cursorIndices = new LinkedList<Integer>();

        String query;
        String[] args = null;
        // Query the database for cards with the specified category (list_id)
        if( categoryId == 0 ) {
            // Query ALL cards that are due
            query = "SELECT card_union._id, card_union.front, card_union.back, card_union.reversed " +
                    "FROM card_union " +
                    "INNER JOIN card_category ON card_union._id = card_category.card_id " +
                    "LEFT OUTER JOIN latest_card_diff ON (card_union._id = latest_card_diff.card_id AND card_union.reversed = latest_card_diff.card_reversed) " +
                    "INNER JOIN selected_categories ON card_category.category_id = selected_categories._id " +
                    "WHERE ((date('now','localtime') >= latest_card_diff.due_date) OR latest_card_diff.due_date IS NULL) ";
        } else {
            // Query all cards that are in the supplied category,
            // ignore due date as this category was specifically requested
            // TODO: Use ContentResolver for this simple query (once other query also uses it)
            query = "SELECT card_union._id, card_union.front, card_union.back, card_union.reversed " +
                    "FROM card_union " +
                    "INNER JOIN card_category ON card_union._id = card_category.card_id " +
                    "WHERE card_category.category_id = ?";

            // All queries use the same list_id
            args = new String[]{String.valueOf(categoryId)};
        }
        cursor = memoraDb.rawQuery( query, args );

        // Get the number of results
        iRowCount = cursor.getCount();

        // Only add the cursor's row index to the array for better memory efficiency
        for(Integer i = 0; i < iRowCount; i++) {
            cursorIndices.add(i);
        }

        // Randomize the collection results
        Collections.shuffle(cursorIndices);

    }

    @Override
    public int getCount() {
        return iRowCount;
    }

    /**
     * Create a CardView for the given position.  Also add the CardView to the ViewGroup.
     * Must ensure this is done by the time it returns from finishUpdate(ViewGroup)
     *
     * @param collection The containing View in which the page will be shown.
     * @param position   The page position to be instantiated.
     * @return Returns an Object representing the new page.  This does not
     * need to be a View, but can be some other container of the page.
     */
    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        Integer cursorIndex = cursorIndices.get(position);
        cursor.moveToPosition(cursorIndex);

        CardView cardView = new CardView(mContext);
        //card._id, card.front, card.back, card.spa_to_eng, list.name
        // TODO: Get column index for each name instead of assuming column order
        int cardId = cursor.getInt(0);
        cardView.setId(cardId);
        // TODO: Remove duplicate queries from above and set reversed data state here
        cardView.setFrontText( cursor.getString(1) );
        cardView.setBackText( cursor.getString(2) );
        cardView.setSpaToEng( cursor.getInt(3) == 0 );    // Converts a 0 to true and a 1 to false
        // Get all the lists that this card belongs to in a CSV string
        cardView.setListNames( getListNames(cardId) );
        cardView.setExampleUsage( getExamples(cardId) );
        cardView.setTextColors(Color.BLACK, Color.BLACK);
        int langColor = ContextCompat.getColor(mContext, R.color.lightGrey);
        cardView.setLangColors(langColor, langColor);
        cardView.setBackgroundResources(R.drawable.rounded_corner);
        // TODO: Check padding params
        //cardView.setPadding(20,20,20,20);

        // Add a tag so we can find it again later
        cardView.setTag(String.valueOf(position));

        collection.addView(cardView);

        return cardView;
    }


    /**
     * Remove a page for the given position.  The adapter is responsible
     * for removing the view from its container, although it only must ensure
     * this is done by the time it returns from finishUpdate(ViewGroup)
     *
     * @param collection The containing View from which the page will be removed.
     * @param position   The page position to be removed.
     * @param view       The same object that was returned by instantiateItem(View, int)
     */
    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((CardView) view);
    }


    /**
     * Determines whether a page View is associated with a specific key object
     * as returned by instantiateItem(ViewGroup, int). This method is required
     * for a PagerAdapter to function properly.
     *
     * @param view   Page View to check for association with object
     * @param object Object to check for association with view
     * @return true if it is associated
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == object);
    }


    // Our own functions we are adding to help query the database

    /**
     * Get every category name that is associated with this card
     * @param cardId The actual database id for the card we are referencing
     * @return A string containing a comma separated list of category names
     */
    private String getListNames(int cardId) {
        // TODO: Look into aggregate function group_concat(X) to do this inside SQLite
        String query =
                "SELECT category.name " +
                        "FROM card_category " +
                        "INNER JOIN category " +
                        "ON card_category.category_id = category._id " +
                        "WHERE card_category.card_id = ? " +
                        "ORDER BY category.name ASC";
        String[] args = new String[] { String.valueOf(cardId) };
        Cursor listCursor = memoraDb.rawQuery(query, args);

        String listNames = "";
        while( listCursor.moveToNext() )  {
            listNames += listCursor.getString(0) + ", ";
        }
        // Close the Cursor now that we have read all the data
        listCursor.close();

        // Return the list without the final ", " attached
        return listNames.substring(0, listNames.length()-2);
    }


    private ArrayList<String> getExamples(int cardId) {
        // Setup a default return value
        ArrayList<String> examples = new ArrayList<>();
        examples.add("No Examples Available");
        examples.add(" ");

        Cursor exampleCursor = mContext.getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_EXAMPLE,
                new String[] {MemoraDbContract.Example.COLUMN_NAME_SPA, MemoraDbContract.Example.COLUMN_NAME_ENG},
                MemoraDbContract.Example.COLUMN_NAME_CARD_ID + "=?",
                new String[] {String.valueOf(cardId)},
                null );

        if( exampleCursor != null ) {
            if (exampleCursor.getCount() != 0) {
                // There is at least one row so build the example array
                examples.clear();   // Remove the default return value
                // Find the column indices that we are going to be reading for every row
                int idxSpa = exampleCursor.getColumnIndex(MemoraDbContract.Example.COLUMN_NAME_SPA);
                int idxEng = exampleCursor.getColumnIndex(MemoraDbContract.Example.COLUMN_NAME_ENG);
                while (exampleCursor.moveToNext()) {
                    examples.add(exampleCursor.getString(idxSpa));
                    examples.add(exampleCursor.getString(idxEng));
                }
            }
            exampleCursor.close();
        }

        return examples;
    }


    /**
     * Set the due date based on how difficult the card was previously. Store the user's choice.
     * @param position Position in the ViewPager of the element to be updated
     * @param difficulty Number of days until this card is "due" again
     * @param update if True then update the last difficulty rating that
     *                  was set (user is changing their answer)
     *               if False add the difficulty rating as the newest
     *                  one and move the rest over by one
     */
    public void setDifficulty(int position, int difficulty, boolean update) {
        // Find the card's Id in the database using the given position in the ViewPager
        cursor.moveToPosition(cursorIndices.get(position));
        int cardId = cursor.getInt(0);
        int cardReversed = cursor.getInt(3);
        // Get a new cursor for this word in the card_difficulty table
        /* Old style difficulty table
        Cursor diffCursor = mContext.getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_DIFFICULTY,
                new String[] {MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY0,
                        MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY1,
                        MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY2,
                        MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY3,
                        MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY4 },
                MemoraDbContract.Card_Difficulty._ID + "=? AND "
                        + MemoraDbContract.Card_Difficulty.COLUMN_NAME_CARD_REVERSED + "=?",
                new String[]{String.valueOf(cardId), String.valueOf(cardReversed)}, null );*/
        // TODO: Use ContentResolver for this simple query
        // Query previous (upto) 5 difficulty ratings with the newest first
        String selectQuery =
                "SELECT _id, card_id, card_reversed, submitted, due_date, difficulty " +
                "FROM card_difficulty " +
                "WHERE card_id = ? AND card_reversed = ? " +
                "ORDER BY submitted DESC, _id DESC " +
                "LIMIT 5";
        String[] args = new String[] { String.valueOf(cardId), String.valueOf(cardReversed)};
        Cursor diffCursor = memoraDb.rawQuery(selectQuery, args);

        // Get column indices
        int idxId = diffCursor.getColumnIndex(MemoraDbContract.Card_Difficulty._ID);
        int idxDifficulty = diffCursor.getColumnIndex(MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY);

        // Fill an array with all the latest difficulty ratings
        ArrayList<Integer> diffArray = new ArrayList<>();
        while( diffCursor.moveToNext() ){
            diffArray.add(diffCursor.getInt(idxDifficulty));
        }

        // Throw out the latest rating when updating; we are replacing it
        if(update) {
            diffArray.remove(0);
        }

        // Calculate the number of days before the word is 'due' again
        int days = calculateDays( difficulty, diffArray );

        // Setup the query string manually
        // Build the SET statement based on how many days the due_date is being set for
        String setDateStatement;
        if( days == 0 ) {
            // Android's SQLite does NOT accept date('now','localtime','+0 days')
            setDateStatement = "date('now','localtime')";
        } else {
            setDateStatement = "date('now','localtime','+" + String.valueOf(days) + " days')";
        }

        // If we are updating a previous answer then UPDATE the latest rating.
        // Otherwise, insert a new row into the DB.
        if( update ){
            // Update newest row in cursor (the first row)
            diffCursor.moveToFirst();

            String query = "UPDATE card_difficulty " +
                    "SET submitted = datetime('now','localtime'), " +
                    "due_date = " + setDateStatement + ", " +
                    "difficulty = " + difficulty + " " +
                    "WHERE _id = " + String.valueOf(diffCursor.getLong(idxId));
            memoraDb.execSQL(query);

        } else {
            // Insert a new row into the DB for this difficulty rating
            String query = "INSERT INTO card_difficulty (card_id, card_reversed, submitted, due_date, difficulty) " +
                    "VALUES (" + cardId + "," + cardReversed + ",datetime('now','localtime')," + setDateStatement + "," + difficulty + ")";
            memoraDb.execSQL(query);
        }

        // We are done with the cursor
        diffCursor.close();
    }

    // Calculate the number of days based on the difficulty and how many times in a row
    // the difficulty has been the same
    // rating = 0 = CRITICAL (always 0 days)
    // rating = 1 = HARD
    // rating = 2 = MED
    // rating = 3 = EASY
    // times can be 0, 1, 2, 3, 4, or 5
    // 0 = first time this time,
    // 5 = this time PLUS the last five times (six times)
    // WARNING - This is a semi-random way of calculating the days. The return values may
    //           need to be adjusted slightly, depending on user feedback. I have pulled
    //           them out of my butt for the time being...
    // TODO: Consider when a user improves or gets worse, how will it affect the number of days?
    // TODO: Update UI difficulty buttons to show how many days to the user before the difficulty button is pressed.
    private int calculateDays( int rating, ArrayList<Integer> diffArray ) {
        // First calculate how many times this rating has been seen in a row without interruption
        // Count the number of times IN A ROW, STARTING AT THE FIRST ELEMENT, WITHOUT INTERRUPTION
        int count = 0;
        for( Integer candidate : diffArray ) {
            if( rating == candidate ) {
                count++;
            } else {
                break;
            }
        }

        // Now use both the rating and the count to determine how many
        // days before the word is 'due' again
        int days = 0;
        if( rating == CRITICAL ) {
            // A Critical rating means the word will always be due
            days = 0;
        } else if ( rating == HARD ) {
            // A Hard rating
            if( count < 3 ) {
                days = 1;
            } else {
                days = 2;
            }
        } else if( rating == MEDIUM ) {
            // A Med rating
            if( count < 2 ) {
                days = 2;
            } else if( count < 4 ) {
                days = 3;
            } else {
                days = 4;
            }
        } else if( rating == EASY ) {
            // An Easy rating
            if( count < 1 ) {
                days = 6;
            } else if( count < 2 ) {
                days = 9;
            } else if( count < 4 ) {
                days = 15;
            } else if( count < 5 ) {
                days = 21;
            } else {
                days = 30;
            }
        }
        // Return the value
        return days;
    }

} // END MemoraDbPagerAdapter