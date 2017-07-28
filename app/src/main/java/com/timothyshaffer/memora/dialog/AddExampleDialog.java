package com.timothyshaffer.memora.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.fragment.WordExamplesFragment;

public class AddExampleDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Inflate the dialog's layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View rootView = inflater.inflate(R.layout.dialog_add_example, null);
        // TODO: Set Error/Bad Data listeners for the TextInputLayouts (no blank data)
        final EditText editSpa = (EditText)rootView.findViewById(R.id.add_example_spa);
        final EditText editEng = (EditText)rootView.findViewById(R.id.add_example_eng);
        builder.setView(rootView)
                .setTitle(R.string.add_example_title)
                .setPositiveButton(R.string.add_example_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the positive button event back to the host activity
                        ((WordExamplesFragment)getTargetFragment()).onAddExample(
                                editSpa.getText().toString(),
                                editEng.getText().toString());
                    }
                })
                .setNegativeButton(R.string.add_example_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
        return builder.create();
    }

}