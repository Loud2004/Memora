package com.timothyshaffer.memora.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraDbHelper;


public class MainMenuActivity extends Activity {

    // Database variables
    private MemoraDbHelper memoraDbHelper;
    private SQLiteDatabase memoraDb;
    // Preferences
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Setup access to the database
        memoraDbHelper = new MemoraDbHelper(getApplicationContext());

        // Setup access to preferences key/value pairs
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check the database for words that are due today
        memoraDb = memoraDbHelper.getWritableDatabase();
        Cursor cursor = memoraDb.rawQuery(
                "SELECT card_union._id " +
                "FROM card_union " +
                "LEFT OUTER JOIN latest_card_diff " +
                    "ON (card_union._id = latest_card_diff.card_id AND card_union.reversed = latest_card_diff.card_reversed) " +
                "INNER JOIN card_category " +
                    "ON card_union._id = card_category.card_id " +
                "INNER JOIN selected_categories " +
                    "ON card_category.category_id = selected_categories._id " +
                "WHERE ((date('now','localtime') >= latest_card_diff.due_date) OR latest_card_diff.due_date IS NULL) ", null);
        // Get the number of results
        int cursorRows = cursor.getCount();
        // If there are no words due then disable the "Study All" button
        Button btnMemorize = (Button)findViewById(R.id.btnMemorize);
        if( cursorRows == 0 ) {
            btnMemorize.setEnabled(false);
            btnMemorize.setText(R.string.no_words_due);
        } else {
            btnMemorize.setEnabled(true);
            btnMemorize.setText(R.string.study_all_words);
        }

        cursor.close();
    }

    /**
     * Launch the Memorize Activity with a generic Category ID so that all words are practiced.
     * Set by OnClick in the XML layout file.
     * @param view The Button that was clicked
     */
    public void onClickStudyAll( View view ){
        Intent intent = new Intent(this, MemorizeActivity.class);
        // Use the special category id of 0 (zero) to refer to all cards.
        // SQLite starts AUTOINCREMENT rowid at 1, so a rowid of 0 should not be found in the table.
        intent.putExtra(StudyCategoryActivity.CATEGORY_ID, "0");
        startActivity(intent);
    }


    public void onClickStudyCategory( View view ){
        Intent intent = new Intent(this, StudyCategoryActivity.class);
        startActivity(intent);
    }


    public void onClickAddWords( View view ){
        Intent intent = new Intent(this, SelectWordsActivity.class);
        startActivity(intent);
    }


    public void onClickProgress( View view ){
        Intent intent = new Intent(this, ProgressActivity.class);
        startActivity(intent);
    }


    public void onClickAbout( View view ){
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }


    public void onClickSettings( View view ){
        // Close DB to prevent crash if it is reset. Will be re-opened on Resume().
        memoraDb.close();
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
