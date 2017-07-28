package com.timothyshaffer.memora.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.activity.WordActivity;
import com.timothyshaffer.memora.view.CardView;

/**
 * A fragment containing all the Data for a Word
 * TODO: as well as a Preview of the Card
 */
public class WordDataFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_CARD_ID = "id";
    private static final String ARG_CARD_SPA = "spa";
    private static final String ARG_CARD_ENG = "eng";


    private long mCardId;
    private String mSpa;
    private String mEng;

    private EditText inputSpa, inputEng;
    private TextInputLayout inputLayoutSpa, inputLayoutEng;

    private CardView mCardView;

    public WordDataFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given Card data
     */
    public static WordDataFragment newInstance(long cardId, String cardSpa, String cardEng) {
        WordDataFragment fragment = new WordDataFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CARD_ID, cardId);
        args.putString(ARG_CARD_SPA, cardSpa);
        args.putString(ARG_CARD_ENG, cardEng);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * When creating, retrieve this instance's Card data from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if( args != null ){
            mCardId = args.getLong(ARG_CARD_ID);
            mSpa = args.getString(ARG_CARD_SPA);
            mEng = args.getString(ARG_CARD_ENG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the root View created in XML so that we can find the rest of the elements
        View rootView = inflater.inflate(R.layout.fragment_word_data, container, false);

        // Setup the text inputs
        inputLayoutSpa = (TextInputLayout) rootView.findViewById(R.id.input_layout_word_spa);
        inputLayoutEng = (TextInputLayout) rootView.findViewById(R.id.input_layout_word_eng);
        inputSpa = (EditText) rootView.findViewById(R.id.input_word_spa);
        inputEng = (EditText) rootView.findViewById(R.id.input_word_eng);

        //((SeekBar)rootView.findViewById(R.id.word_spa_size)).setEnabled(false);
        //((SeekBar)rootView.findViewById(R.id.word_eng_size)).setEnabled(false);

        // Set previous Card data
        inputSpa.setText(mSpa);
        inputEng.setText(mEng);

        inputSpa.addTextChangedListener(new MyTextWatcher(inputSpa));
        inputEng.addTextChangedListener(new MyTextWatcher(inputEng));
        View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText editText = (EditText)v;
                if( !hasFocus ){
                    if(validateInput(editText)) {
                        // If the input is good, then trim the whitespace
                        editText.setText(editText.getText().toString().trim());
                    }
                }
            }
        };
        inputSpa.setOnFocusChangeListener(focusListener);
        inputEng.setOnFocusChangeListener(focusListener);

        // Setup the "Preview" CardView
        mCardView = new CardView(getActivity());

        mCardView.setFrontText(mSpa);
        mCardView.setBackText(mEng);
        mCardView.setSpaToEng(true);    // Converts a 0 to true and a 1 to false
        mCardView.setTextColors(Color.BLACK, Color.BLACK);
        int langColor = ContextCompat.getColor(getContext(), R.color.lightGrey);
        mCardView.setLangColors(langColor, langColor);
        mCardView.setBackgroundResources(R.drawable.rounded_corner);

        FrameLayout frameLayout = (FrameLayout)rootView.findViewById(R.id.word_cardView_container);
        frameLayout.addView(mCardView);

        return rootView;
    }

    private class MyTextWatcher implements TextWatcher {
        private EditText view;
        private MyTextWatcher(EditText view) {
            this.view = view;
        }
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        public void afterTextChanged(Editable editable) {
            // This listener is used for both EditText boxes, so determine
            // which one this is assigned to so we don't do double work
            if (view == inputSpa) {
                String spa = inputSpa.getText().toString();
                mCardView.setFrontText(spa);

                // Set the dirty state as soon as the user makes any change to the words
                if( !spa.equals(mSpa) ) {
                    ((WordActivity) getActivity()).setDirtyState();
                }
            } else if (view == inputEng) {
                String eng = inputEng.getText().toString();
                mCardView.setBackText(eng);

                // Set the dirty state as soon as the user makes any change to the words
                if( !eng.equals(mEng) ) {
                    ((WordActivity) getActivity()).setDirtyState();
                }
            }


        }
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private boolean validateInput(EditText editText) {
        // Check the Spanish Word
        if(editText.getText().toString().trim().isEmpty()) {
            ((TextInputLayout)editText.getParent()).setError(getString(R.string.err_msg));
            //requestFocus(editText);
            return false;
        }
        // else
        ((TextInputLayout)editText.getParent()).setErrorEnabled(false);
        return true;
    }

    // Getter methods for the EditText boxes
    public String getSpaText(){
        // First trim the spaces and update the EditText boxes
        inputSpa.setText(inputSpa.getText().toString().trim());
        // Now return the trimmed text
        return inputSpa.getText().toString();
    }

    public String getEngText(){
        // First trim the spaces and update the EditText boxes
        inputEng.setText(inputEng.getText().toString().trim());
        // Now return the trimmed text
        return inputEng.getText().toString();
    }

    public void saveCard(){
        // Update the internal variables to reflect the data shown in the input
        mSpa = inputSpa.getText().toString();
        mEng = inputEng.getText().toString();
    }
}