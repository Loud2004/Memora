package com.timothyshaffer.memora.activity;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;
import com.timothyshaffer.memora.db.MemoraDbHelper;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class SettingsActivity extends AppCompatActivity {
    public static final String TAG = "SettingsActivity";

    private static final int FILE_OPEN = 1000;   // Random value

    private SharedPreferences sharedPref;

    private MemoraDbHelper memoraDbHelper;

    // Get the view we will display the results snackbar
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Setup the Toolbar/ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ) {
            // Enable the Up button
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Find the root view for showing snackbar notifications
        rootView = findViewById(R.id.settings_content);

        // Get a handle for storing simple key/value pairs for the User's preferences
        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        // Get the User's preferences
        int frontView = sharedPref.getInt(getString(R.string.frontView), MemoraDbHelper.SPA);
        // Set the UI elements to match the stored preferences
        switch(frontView) {
            case MemoraDbHelper.SPA:
                ((RadioButton)findViewById(R.id.radioSpa)).setChecked(true);
                break;
            case MemoraDbHelper.ENG:
                ((RadioButton)findViewById(R.id.radioEng)).setChecked(true);
                break;
            case MemoraDbHelper.BOTH:
                ((RadioButton)findViewById(R.id.radioBoth)).setChecked(true);
                break;
            /*
            case MemoraDbHelper.RANDOM:
            default:
                ((RadioButton)findViewById(R.id.radioRandom)).setChecked(true);
                break;
            */
        }


        // Set the version number shown in settings
        TextView versionText = (TextView) findViewById(R.id.versionText);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionText.setText(getString(R.string.settings_version, packageInfo.versionName));
        } catch (PackageManager.NameNotFoundException e){
            // Error: set version to blank text
            versionText.setText("");
        }


        // Get a handle to the database for Reset/Backup/Import
        memoraDbHelper = new MemoraDbHelper(getApplicationContext());

    }


    // Save the User's Preferences each time they select a change, and update the DB accordingly
    public void onRadioClicked(View view) {
        SharedPreferences.Editor editor = sharedPref.edit();
        // Check which button was clicked
        switch(view.getId()) {
            case R.id.radioSpa:
                editor.putInt(getString(R.string.frontView), MemoraDbHelper.SPA);
                memoraDbHelper.setCardUnionView(MemoraDbHelper.SPA);
                break;
            case R.id.radioEng:
                editor.putInt(getString(R.string.frontView), MemoraDbHelper.ENG);
                memoraDbHelper.setCardUnionView(MemoraDbHelper.ENG);
                break;
            case R.id.radioBoth:
                editor.putInt(getString(R.string.frontView), MemoraDbHelper.BOTH);
                memoraDbHelper.setCardUnionView(MemoraDbHelper.BOTH);
                break;
            /*
            case R.id.radioRandom:
                editor.putInt(getString(R.string.frontView), MemoraDbHelper.RANDOM);
                memoraDbHelper.setCardUnionView(MemoraDbHelper.RANDOM);
                break;
            */
        }
        // Apply the changes made to Preferences
        editor.apply();
    }


    public void resetTestData(View v) {
        memoraDbHelper.resetTestData();
        // We need to reset the ContentProvider to use the new database file that
        // we just created. Get a handle to our specific provider, then call the
        // reset function which resets its internal database handle.
        Uri memoraUri = Uri.parse("content://com.timothyshaffer.memora.contentprovider");
        ContentProviderClient client = getContentResolver().acquireContentProviderClient(memoraUri);
        MemoraContentProvider provider = (MemoraContentProvider) client.getLocalContentProvider();
        provider.resetDatabase();
        client.release();   // Release our handle to the provider now that we are done with it

        // Show success message to the user
        Snackbar.make(rootView, "Test Data Reset", Snackbar.LENGTH_SHORT).show();
    }

    public void backupDatabase(View v) {
        // Attempt to Save the DB
        String filename = memoraDbHelper.backupDatabase();


        if( !filename.isEmpty() ) {
            Snackbar.make(rootView, "Backup Successful: " + filename, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(rootView, "Backup Failed", Snackbar.LENGTH_LONG).show();
        }
    }

    public void importDatabase(View v ) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Import File"),
                    FILE_OPEN);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Snackbar.make(rootView, "Please install a File Manager.", Snackbar.LENGTH_LONG).show();
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_OPEN:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());

                    InputStream importFile = null;
                    try {
                        importFile = getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException e) {
                        Snackbar.make(rootView, "Import Failed: Cannot open file", Snackbar.LENGTH_SHORT).show();
                    }

                    if (memoraDbHelper.importDatabase(importFile)) {
                        Snackbar.make(rootView, "Import Successful", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(rootView, "Import Failed", Snackbar.LENGTH_SHORT).show();
                    }

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
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
