package com.timothyshaffer.memora.db;

import android.provider.BaseColumns;

/**
 * A container class for constants that define names for database specific tables, and columns.
 * Each inner class enumerates the columns of a table in the database.
 */
public final class MemoraDbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public MemoraDbContract() {}

    // A String Array of all the Card column names that we want to check to make sure that
    // an imported database has the correct format before we override the old DB file.
    public static final String[] CARD_COLUMN_NAMES = {
            Card.COLUMN_NAME_SPA, Card.COLUMN_NAME_ENG
    };

    /* Inner class that defines the table contents */
    public static abstract class Card implements BaseColumns {
        public static final String TABLE_NAME = "card";
        public static final String COLUMN_NAME_SPA = "spa";
        public static final String COLUMN_NAME_ENG = "eng";
    }

    public static abstract class Card_Difficulty implements BaseColumns {
        public static final String TABLE_NAME = "card_difficulty";
        public static final String COLUMN_NAME_CARD_ID = "card_id";
        public static final String COLUMN_NAME_CARD_REVERSED = "card_reversed";
        public static final String COLUMN_NAME_SUBMITTED = "submitted";
        public static final String COLUMN_NAME_DUE_DATE = "due_date";
        public static final String COLUMN_NAME_DIFFICULTY = "difficulty";
    }

    public static abstract class Example implements BaseColumns {
        public static final String TABLE_NAME = "example";
        public static final String COLUMN_NAME_CARD_ID = "card_id";
        public static final String COLUMN_NAME_SPA = "spa";
        public static final String COLUMN_NAME_ENG = "eng";
    }

    public static abstract class Category implements BaseColumns {
        public static final String TABLE_NAME = "category";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
    }

    public static abstract class Selected_Categories implements BaseColumns {
        public static final String TABLE_NAME = "selected_categories";
    }

    public static abstract class Card_Category implements BaseColumns {
        public static final String TABLE_NAME = "card_category";
        public static final String COLUMN_NAME_CARD_ID = "card_id";
        public static final String COLUMN_NAME_CATEGORY_ID = "category_id";
    }

    /* NOT YET IMPLEMENTED */
    public static abstract class Card_Meta implements  BaseColumns {
        public static final String TABLE_NAME = "card_meta";
        public static final String COLUMN_NAME_SPA_SIZE = "spa_size";
        public static final String COLUMN_NAME_ENG_SIZE = "eng_size";
    }

    /* NOT YET IMPLEMENTED */
    public static abstract class Pronunciation implements BaseColumns {
        public static final String TABLE_NAME = "pronunciation";
        public static final String COLUMN_NAME_CARD_ID = "card_id";
        public static final String COLUMN_NAME_SOUND_DATA = "sound_data";
        public static final String COLUMN_NAME_SPEAKER = "speaker";
    }

    // TODO: public static abstract class Conjugation implements BaseColumns {}


    /*******************************
     *            Views            *
     *******************************/
    public static abstract class Card_Union implements BaseColumns {
        public static final String TABLE_NAME = "card_union";
        public static final String COLUMN_NAME_FRONT = "front";
        public static final String COLUMN_NAME_BACK = "back";
        public static final String COLUMN_NAME_REVERSED = "reversed";
    }

    public static abstract class Latest_Card_Diff implements BaseColumns {
        public static final String TABLE_NAME = "latest_card_diff";
        public static final String COLUMN_NAME_CARD_ID = Card_Difficulty.COLUMN_NAME_CARD_ID;
        public static final String COLUMN_NAME_CARD_REVERSED = Card_Union.COLUMN_NAME_REVERSED;
        public static final String COLUMN_NAME_SUBMITTED = Card_Difficulty.COLUMN_NAME_SUBMITTED;
        public static final String COLUMN_NAME_DUE_DATE = Card_Difficulty.COLUMN_NAME_DUE_DATE;
        public static final String COLUMN_NAME_DIFFICULTY = Card_Difficulty.COLUMN_NAME_DIFFICULTY;

    }
}
