package com.timothyshaffer.memora.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * Helper class to make access to the database easier. SQLiteAssetHelper uses the
 * files in /assets/databases/ for database creation instead of raw SQL statements.
 */
public class MemoraDbHelper extends SQLiteAssetHelper  {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Memora.db";
    private String DB_FILEPATH;


    public MemoraDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        final String packageName = context.getPackageName();
        DB_FILEPATH = context.getApplicationInfo().dataDir + "/databases/" + DATABASE_NAME;
    }


    /*
     * Helper Functions to Import/Export the database between the user-accessible
     * filesystem and the internal private /data/data directory
     */

    /**
     * Copy the contents of 'src' into 'dst', replacing anything previously stored there.
     * @param src file to copy the contents from
     * @param dst file to put the copied data in
     * @throws IOException if any I/O error occurs.
     */
    private void copyFile(File src, File dst) throws IOException {
        // Delete the dst file if it exists
        dst.delete();

        // Re-make the dst file using FileOutputStream
        FileChannel fromChannel = new FileInputStream(src).getChannel();
        FileChannel toChannel = new FileOutputStream(dst).getChannel();
        try {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                //noinspection ConstantConditions
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }

    // Returns the location where the backup was create (or an empty string on failure)
    public String backupDatabase() {
        // Exit early if the SD Card is not writable
        if( !isExternalStorageWritable() ) {
            return "";
        }
        // Open the local db as the input stream
        String inFileName = DB_FILEPATH;
        File dbFile = new File(inFileName);

        String outFileName = Environment.getExternalStorageDirectory() + "/" + DATABASE_NAME;
        // Open the empty db as the output stream
        File exportFile = new File(outFileName);
        // Copy the data
        try{
            copyFile(dbFile, exportFile);
            return outFileName;
        } catch(IOException e) {
            return "";
        }
    }


    /**
     * Import a database chosen by the user. Input is checked to be a valid database file before
     * the old database is overwritten.
     * @param inputStream A handle to the data for the file we want to import
     * @return true if the import was successful, otherwise false
     */
    public boolean importDatabase(InputStream inputStream) {
        try {
            // Write the InputStream to a file
            File tempFile = File.createTempFile("temp", null);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            File oldDb = new File(DB_FILEPATH);

            // Check that the file is a valid database
            if (!checkDatabaseValidity(tempFile)) {
                return false;
            }

            copyFile(tempFile, oldDb);

            // Access the copied database so SQLiteHelper
            // will cache it and mark it as created.
            getWritableDatabase().close();

            // Delete the temp file, it is no longer needed
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean checkDatabaseValidity(File testDb) {
        try{
            //throws SQLiteException if the database cannot be opened
            SQLiteDatabase sqlDb = SQLiteDatabase.openDatabase
                    (testDb.getPath(), null, SQLiteDatabase.OPEN_READONLY);

            // Instead of checking each table in the database for the correct column names,
            // we will instead just check the Card table and assume if it is correct that we have
            // a good database.

            // We just need column names so limit the number of rows to 1 to speed things up
            Cursor cursor = sqlDb.query(
                    true, "card", null, null, null, null, null, null, "1"
            );

            // CARD_COLUMN_NAMES should be an array of keys of essential columns.
            // Throws exception if any column is missing
            for (String s : MemoraDbContract.CARD_COLUMN_NAMES) {
                // throws IllegalArgumentException if the column does not exist
                cursor.getColumnIndexOrThrow(s);
            }

            cursor.close();
            sqlDb.close();

        } catch( IllegalArgumentException e ) {
            //Log.d(TAG, "Database is invalid.");
            return false;
        } catch( SQLiteException e ) {
            //Log.d(TAG, "Database file cannot be opened.");
            return false;
        } catch( Exception e){
            //Log.d(TAG, "checkDbIsValid encountered an exception");
            return false;
        }

        return true;

    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    public void resetTestData() {
        File dbFile = new File(DB_FILEPATH);
        dbFile.delete();
        getWritableDatabase().close();  // TODO: Needed?
    }


    // Used to change the `card_union` View. Choose which language to show on the front of the cards.
    // Either Spanish, English, or Both (splits every card into 2 cards)
    public static final int SPA = 0;
    public static final int ENG = 1;
    public static final int BOTH = 2;
    //public static final int RANDOM = 3;

    // Set the `card_union` View based on what the user wants to show on the front of the card.
    public void setCardUnionView( int frontFace ) {
        String query = "";
        switch( frontFace ) {
            // Only show Spanish on the front
            case SPA:
                query = "CREATE VIEW `card_union` AS " +
                        "SELECT `_id` AS `_id`, `spa` AS `front`, `eng` AS `back`, '0' AS `reversed` FROM `card`;";
                break;
            // Only show English on the front
            case ENG:
                query = "CREATE VIEW `card_union` AS " +
                        "SELECT `_id` AS `_id`, `eng` AS `front`, `spa` AS `back`, '1' AS `reversed` FROM `card`;";
                break;
            // Split all cards into 2 so that one shows Spanish and one shows English
            case BOTH:
                query = "CREATE VIEW `card_union` AS " +
                        "SELECT `_id` AS `_id`, `spa` AS `front`, `eng` AS `back`, '0' AS `reversed` FROM `card` " +
                        "UNION ALL " +
                        "SELECT `_id` AS `_id`, `eng` AS `front`, `spa` AS `back`, '1' AS `reversed` FROM `card`;";
                break;
            /*
            case RANDOM:
            default:
                query = "CREATE VIEW `card_union` AS " +
                        "SELECT t1._id, t1.front, t1.back, t1.reversed FROM (SELECT _id, lower(abs(random()) % 2) AS reversed FROM card) AS t2 " +
                        "LEFT OUTER JOIN ( " +
                            "SELECT `_id` AS `_id`, `spa` AS `front`, `eng` AS `back`, '0' AS `reversed` FROM `card` " +
                            "UNION ALL " +
                            "SELECT `_id` AS `_id`, `eng` AS `front`, `spa` AS `back`, '1' AS `reversed` FROM `card` " +
                        ") AS t1 " +
                        "ON (t1._id = t2._id AND t1.reversed = t2.reversed) " +
                        "ORDER BY t2._id";
            */
        }
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP VIEW `card_union`;");  // Drop the existing View
        db.execSQL(query);                      // Re-create the View
        db.close();
    }


}
