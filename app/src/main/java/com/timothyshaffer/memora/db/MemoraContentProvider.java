package com.timothyshaffer.memora.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

@SuppressWarnings({"ConstantConditions", "NullableProblems", "UnusedAssignment"})
public class MemoraContentProvider extends ContentProvider {

    // Define the URI that this ContentProvider will handle.
    public static final String AUTHORITY = "com.timothyshaffer.memora.contentprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri CONTENT_URI_CATEGORY =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Category.TABLE_NAME);
    public static final Uri CONTENT_URI_SELECT_WORDS_LIST =
            Uri.withAppendedPath(CONTENT_URI, "select_words_list");
    public static final Uri CONTENT_URI_CARD =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Card.TABLE_NAME);
    public static final Uri CONTENT_URI_CARD_CATEGORY =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Card_Category.TABLE_NAME);
    public static final Uri CONTENT_URI_CARD_CATEGORIES =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Card.TABLE_NAME + "/" +
                    MemoraDbContract.Card_Category.TABLE_NAME);
    public static final Uri CONTENT_URI_WORD_CATEGORIES =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Card.TABLE_NAME + "/" +
                    MemoraDbContract.Category.TABLE_NAME);
    public static final Uri CONTENT_URI_CARD_CATEGORIES_FILTER =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Card.TABLE_NAME + "/" +
                    MemoraDbContract.Category.TABLE_NAME + "/filter");
    public static final Uri CONTENT_URI_STUDY_CATEGORY =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Category.TABLE_NAME + "/filter");
    public static final Uri CONTENT_URI_LATEST_DIFFICULTY =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Category.TABLE_NAME + "/" +
                    MemoraDbContract.Card_Difficulty.TABLE_NAME + "/latest");
    public static final Uri CONTENT_URI_CATEGORIES_PROGRESS =
            Uri.withAppendedPath(CONTENT_URI, "categories_progress");
    public static final Uri CONTENT_URI_EXAMPLE =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Example.TABLE_NAME);
    public static final Uri CONTENT_URI_DIFFICULTY =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Card_Difficulty.TABLE_NAME);
    public static final Uri CONTENT_URI_SELECTED_CATEGORIES =
            Uri.withAppendedPath(CONTENT_URI, MemoraDbContract.Selected_Categories.TABLE_NAME);



    // Define which operations we will handle
    private static final int SELECT_WORDS_LIST = 1;
    private static final int CATEGORIES = 2;
    private static final int CATEGORY_ID = 3;
    private static final int CARDS = 4;
    private static final int CARD_ID = 5;
    private static final int CARD_CATEGORY = 6;
    private static final int CARD_CATEGORIES = 7;
    private static final int WORD_CATEGORIES = 8;
    private static final int CARD_CATEGORIES_FILTER = 9;
    private static final int STUDY_CATEGORY = 10;
    private static final int LATEST_DIFFICULTY = 11;
    private static final int CATEGORIES_PROGRESS = 12;
    private static final int EXAMPLES = 13;
    private static final int EXAMPLE_ID = 14;
    private static final int DIFFICULTIES = 15;
    private static final int DIFFICULTIES_ID = 16;
    private static final int SELECTED_CATEGORIES = 17;
    private static final int SELECTED_CATEGORIES_ID = 18;


    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // No table name for joined tables so make one up: "select_words_list"
        uriMatcher.addURI(AUTHORITY, "select_words_list", SELECT_WORDS_LIST);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Category.TABLE_NAME, CATEGORIES);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Category.TABLE_NAME + "/#", CATEGORY_ID);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card.TABLE_NAME, CARDS);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card.TABLE_NAME + "/#", CARD_ID);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card_Category.TABLE_NAME, CARD_CATEGORY);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card.TABLE_NAME + "/" +
                MemoraDbContract.Card_Category.TABLE_NAME, CARD_CATEGORIES);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card.TABLE_NAME + "/" +
                MemoraDbContract.Category.TABLE_NAME, WORD_CATEGORIES);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card.TABLE_NAME + "/" +
                MemoraDbContract.Category.TABLE_NAME + "/filter", CARD_CATEGORIES_FILTER);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Category.TABLE_NAME + "/filter", STUDY_CATEGORY);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Category.TABLE_NAME + "/" +
                MemoraDbContract.Card_Difficulty.TABLE_NAME + "/latest", LATEST_DIFFICULTY);
        uriMatcher.addURI(AUTHORITY, "categories_progress", CATEGORIES_PROGRESS);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Example.TABLE_NAME, EXAMPLES);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Example.TABLE_NAME + "/#", EXAMPLE_ID);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card_Difficulty.TABLE_NAME, DIFFICULTIES);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Card_Difficulty.TABLE_NAME + "/#", DIFFICULTIES_ID);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Selected_Categories.TABLE_NAME, SELECTED_CATEGORIES);
        uriMatcher.addURI(AUTHORITY, MemoraDbContract.Selected_Categories.TABLE_NAME + "/#", SELECTED_CATEGORIES_ID);
    }

    // The database we will be performing operations on
    public MemoraDbHelper memoraDbHelper;

    @Override
    public boolean onCreate() {
        // Setup an SQLiteOpenHelper so we can get the writable database in each CRUD operation
        memoraDbHelper = new MemoraDbHelper(getContext());
        return true;
    }

    public void resetDatabase() {
        memoraDbHelper = new MemoraDbHelper(getContext());
    }


    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteDatabase memoraDb = memoraDbHelper.getWritableDatabase();
        Cursor cursor;
        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case SELECT_WORDS_LIST:
                cursor = memoraDb.rawQuery(
                        "SELECT category._id, category.name, category.description, COUNT(card_category.card_id) AS 'NumOfCards', CASE WHEN selected_categories._id IS NULL THEN 0 ELSE 1 END AS selected, ifnull(category_memorized.NumMemorized, 0) AS NumMemorized " +
                        "FROM card_category " +
                        "INNER JOIN category " +
                            "ON card_category.category_id = category._id " +
                        "LEFT OUTER JOIN category_memorized " +
                            "ON card_category.category_id = category_memorized._id " +
                        "LEFT OUTER JOIN selected_categories " +
                            "ON category._id = selected_categories._id " +
                        "GROUP BY category.name " +
                        "UNION ALL " +
                        "SELECT category._id, category.name, category.description, 0, ifnull(selected_categories._id, 0) AS selected, 0 " +
                        "FROM category " +
                        "LEFT OUTER JOIN selected_categories " +
                            "ON category._id = selected_categories._id " +
                        "WHERE NOT EXISTS ( " +
                            "SELECT 1 " +
                            "FROM card_category " +
                            "WHERE category._id = card_category.category_id " +
                        ")", null);
                break;
            case CATEGORIES:
                // A simple query, pass all args through
                cursor = memoraDb.query(MemoraDbContract.Category.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case CARDS:
                // A simple query, pass all args through
                cursor = memoraDb.query(MemoraDbContract.Card.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case CARD_CATEGORY:
                cursor = memoraDb.query(MemoraDbContract.Card_Category.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case CARD_CATEGORIES:
                cursor = memoraDb.rawQuery(
                        "SELECT card._id, card.spa, card.eng, ifnull(A.num_categories, 0) AS num_categories, ifnull(B.in_category, 0) AS in_category " +
                            "FROM card " +
                            "LEFT OUTER JOIN ( " +
                                "SELECT card_id, COUNT(*) AS num_categories " +
                                    "FROM card_category " +
                                "GROUP BY card_category.card_id " +
                            ") AS A ON card._id = A.card_id " +
                            "LEFT OUTER JOIN ( " +
                                "SELECT card_id, '1' AS in_category " +
                                    "FROM card_category " +
                                "WHERE category_id = ? " +
                            ") AS B ON card._id = B.card_id " +
                            "WHERE (card.spa LIKE ?) OR (card.eng LIKE ?)", selectionArgs);
                break;
            // Uses INNER JOIN instead of OUTER to get only the Cards that are in the specified Category
            case CARD_CATEGORIES_FILTER:
                cursor = memoraDb.rawQuery(
                        "SELECT card._id AS _id, card.spa AS spa, card.eng AS eng, ifnull(A.num_categories, 0) AS num_categories, ifnull(B.in_category, 0) AS in_category " +
                            "FROM card " +
                            "LEFT OUTER JOIN ( " +
                                "SELECT card_id, COUNT(*) AS num_categories " +
                                    "FROM card_category " +
                                "GROUP BY card_category.card_id " +
                            ") AS A ON card._id = A.card_id " +
                            "INNER JOIN ( " +
                                "SELECT card_id, '1' AS in_category " +
                                    "FROM card_category " +
                                "WHERE category_id = ? " +
                            ") AS B ON card._id = B.card_id " +
                            "WHERE (card.spa LIKE ?) OR (card.eng LIKE ?)", selectionArgs);
                break;
            case WORD_CATEGORIES:
                cursor = memoraDb.rawQuery(
                        "SELECT category._id, category.name, category.description, " +
                            "CASE " +
                                "WHEN category._id IN (SELECT category_id FROM card_category WHERE card_id = ?) THEN 1 " +
                                "ELSE 0 " +
                            "END AS selected " +
                            "FROM category " +
                            "ORDER BY category.name", selectionArgs);
                break;
            case STUDY_CATEGORY:
                // Return a list of the selected Categories with a count of how many cards are in each
                cursor = memoraDb.rawQuery(
                        "SELECT category._id, category.name, category.description, COUNT(card_category.card_id) AS 'NumOfCards' " +
                        "FROM card_category " +
                        "INNER JOIN selected_categories " +
                            "ON card_category.category_id = selected_categories._id " +
                        "INNER JOIN category " +
                            "ON card_category.category_id = category._id " +
                        "GROUP BY category.name", null);
                break;
            case LATEST_DIFFICULTY:
                // Get the Progress for a Category, count how many cards are at each difficulty level (using latest diff ratings)
                cursor = memoraDb.rawQuery(
                        "SELECT card_union.reversed AS reversed, card_difficulty_latest.difficulty AS difficulty, COUNT(card_difficulty_latest.difficulty) AS diff_count " +
                        "FROM card_union " +
                        "INNER JOIN card_category ON card_union._id = card_category.card_id " +
                        "INNER JOIN card_difficulty_latest ON (card_union._id = card_difficulty_latest.card_id AND card_union.reversed = card_difficulty_latest.card_reversed) " +
                        "WHERE card_category.category_id = ? " +
                        "GROUP BY card_difficulty_latest.card_reversed, card_difficulty_latest.difficulty " +
                        "ORDER BY card_union.reversed ASC, card_difficulty_latest.difficulty ASC", selectionArgs);
                break;
            case CATEGORIES_PROGRESS:
                cursor = memoraDb.rawQuery(
                        "SELECT category._id, category.name, category.description, COUNT(card_category.card_id) AS 'NumOfCards', ifnull(category_memorized.NumMemorized, 0) AS NumMemorized " +
                        "FROM card_category " +
                        "INNER JOIN category " +
                            "ON card_category.category_id = category._id " +
                        "LEFT OUTER JOIN category_memorized " +
                            "ON card_category.category_id = category_memorized._id " +
                        "INNER JOIN selected_categories " +
                            "ON category._id = selected_categories._id " +
                        "GROUP BY category.name", null);
                break;
            case EXAMPLES:
                cursor = memoraDb.query(MemoraDbContract.Example.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case DIFFICULTIES:
                cursor = memoraDb.query(MemoraDbContract.Card_Difficulty.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case DIFFICULTIES_ID:
                String cardId = uri.getLastPathSegment();
                cursor = memoraDb.query(MemoraDbContract.Card_Difficulty.TABLE_NAME, projection,
                        MemoraDbContract.Card_Difficulty._ID + "=?", new String[]{cardId},
                        null, null, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Set each query to listen for changes on the URI they were called with
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase memoraDb = memoraDbHelper.getWritableDatabase();
        int uriType = uriMatcher.match(uri);
        Uri returnUri;
        switch (uriType) {
            case CATEGORIES:
                long list_id = memoraDb.insert(MemoraDbContract.Category.TABLE_NAME, null, values);
                returnUri = ContentUris.withAppendedId(uri, list_id);
                // Notify non-standard queries that their data may have changed
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_STUDY_CATEGORY, null);
                break;
            case CARDS:
                long card_id = memoraDb.insert(MemoraDbContract.Card.TABLE_NAME, null, values);
                returnUri = ContentUris.withAppendedId(uri, card_id);
                break;
            case CARD_CATEGORY:
                // TODO: Return a real URI once compound PK URI's are implemented
                memoraDb.insert(MemoraDbContract.Card_Category.TABLE_NAME, null, values);
                returnUri = CONTENT_URI_CARD_CATEGORY;   // TODO: HACK HACK HACK
                // Notify non-standard queries that their data may have changed
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_WORD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CARD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_STUDY_CATEGORY, null);
                break;
            case EXAMPLES:
                long example_id = memoraDb.insert(MemoraDbContract.Example.TABLE_NAME, null, values);
                returnUri = ContentUris.withAppendedId(uri, example_id);
                break;
            case DIFFICULTIES:
                long difficulty_id = memoraDb.insert(MemoraDbContract.Card_Difficulty.TABLE_NAME, null, values);
                returnUri = ContentUris.withAppendedId(uri, difficulty_id);
                break;
            case SELECTED_CATEGORIES:
                long cat_id = memoraDb.insert(MemoraDbContract.Selected_Categories.TABLE_NAME, null, values);
                returnUri = ContentUris.withAppendedId(uri, cat_id);
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CATEGORIES_PROGRESS, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Notify all queries that the table we just altered may have changes
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }


    // Handle bulkInsert() ourselves. The default implementation
    // is horribly slow because it doesn't use a transaction.
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase memoraDb = memoraDbHelper.getWritableDatabase();
        int uriType = uriMatcher.match(uri);
        int numInserted = 0;
        switch (uriType) {
            case CARD_CATEGORY:
                memoraDb.beginTransaction();
                for( ContentValues value : values ) {
                    memoraDb.insert(MemoraDbContract.Card_Category.TABLE_NAME, null, value);
                    numInserted++;
                }
                memoraDb.setTransactionSuccessful();
                memoraDb.endTransaction();
                // Notify non-standard queries that their data may have changed
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_WORD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CARD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_STUDY_CATEGORY, null);
                break;
            case EXAMPLES:
                memoraDb.beginTransaction();
                for( ContentValues value : values ) {
                    memoraDb.insert(MemoraDbContract.Example.TABLE_NAME, null, value);
                    numInserted++;
                }
                memoraDb.setTransactionSuccessful();
                memoraDb.endTransaction();
                break;
            default:
                return super.bulkInsert(uri, values);
        }

        // Notify all queries that the table we just altered may have changes
        getContext().getContentResolver().notifyChange(uri, null);
        return numInserted;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase memoraDb = memoraDbHelper.getWritableDatabase();
        int rowsUpdated = 0;
        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case CATEGORY_ID:
                String listId = uri.getLastPathSegment();
                rowsUpdated = memoraDb.update(
                        MemoraDbContract.Category.TABLE_NAME,
                        values,
                        MemoraDbContract.Category._ID + "=" + listId,
                        null);
                // Notify non-standard queries that their data may have changed
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_STUDY_CATEGORY, null);
                break;
            case CARD_ID:
                String cardId = uri.getLastPathSegment();
                rowsUpdated = memoraDb.update(
                        MemoraDbContract.Card.TABLE_NAME,
                        values,
                        MemoraDbContract.Card._ID + "=" + cardId,
                        null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CARD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CARD_CATEGORIES_FILTER, null);
                break;
            case EXAMPLES:
                rowsUpdated = memoraDb.update(MemoraDbContract.Example.TABLE_NAME, values, selection, selectionArgs);
                break;
            case DIFFICULTIES:
                rowsUpdated = memoraDb.update(MemoraDbContract.Card_Difficulty.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Notify all queries that the table we just altered may have changes
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase memoraDb = memoraDbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case CATEGORY_ID:
                String listId = uri.getLastPathSegment();
                rowsDeleted = memoraDb.delete(
                        MemoraDbContract.Category.TABLE_NAME,
                        MemoraDbContract.Category._ID + "=?",
                        new String[]{listId});
                // Notify non-standard queries that their data may have changed
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_STUDY_CATEGORY, null);
                break;
            case CARD_ID:
                String cardId = uri.getLastPathSegment();
                rowsDeleted = memoraDb.delete(
                        MemoraDbContract.Card.TABLE_NAME,
                        MemoraDbContract.Card._ID + "=?",
                        new String[]{cardId});
                getContext().getContentResolver().notifyChange(CONTENT_URI_CARD_CATEGORIES, null);
                break;
            case CARD_CATEGORY:
                rowsDeleted = memoraDb.delete(MemoraDbContract.Card_Category.TABLE_NAME, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_WORD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CARD_CATEGORIES, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_STUDY_CATEGORY, null);
                break;
            case EXAMPLES:
                rowsDeleted = memoraDb.delete(MemoraDbContract.Example.TABLE_NAME, selection, selectionArgs);
                break;
            case EXAMPLE_ID:
                String exampleId = uri.getLastPathSegment();
                rowsDeleted = memoraDb.delete(
                        MemoraDbContract.Example.TABLE_NAME,
                        MemoraDbContract.Example._ID + "=?",
                        new String[] {exampleId});
                break;
            case DIFFICULTIES:
                rowsDeleted = memoraDb.delete(MemoraDbContract.Card_Difficulty.TABLE_NAME, selection, selectionArgs);
                break;
            case SELECTED_CATEGORIES_ID:
                String catId = uri.getLastPathSegment();
                rowsDeleted = memoraDb.delete(
                        MemoraDbContract.Selected_Categories.TABLE_NAME,
                        MemoraDbContract.Selected_Categories._ID + "=?",
                        new String[]{catId});
                getContext().getContentResolver().notifyChange(CONTENT_URI_SELECT_WORDS_LIST, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_CATEGORIES_PROGRESS, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Notify all queries that the table we just altered may have changes
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

}
