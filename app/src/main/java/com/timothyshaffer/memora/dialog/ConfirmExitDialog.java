package com.timothyshaffer.memora.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.timothyshaffer.memora.R;


public class ConfirmExitDialog extends DialogFragment {

    private OnConfirmExitListener mListener;

    public ConfirmExitDialog() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnConfirmExitListener) {
            mListener = (OnConfirmExitListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + getActivity().getString(R.string.confirm_exit_attach_error));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_action_warning)
                .setTitle(getActivity().getString(R.string.confirm_exit_title))
                .setMessage(getActivity().getString(R.string.confirm_exit_message))
                .setPositiveButton(getActivity().getString(R.string.confirm_exit_discard), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onConfirmExitDiscard();
                    }
                })
                .setNeutralButton(getActivity().getString(R.string.confirm_exit_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onConfirmExitSave();
                    }
                })
                .setNegativeButton(getActivity().getString(R.string.confirm_exit_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing, just dismiss the dialog
                    }
                })
                .create();
    }

    /**
     * An Interface that will allow the parent Activity to receive a notification
     * about which button the user pressed.
     */
    public interface OnConfirmExitListener {
        void onConfirmExitSave();
        void onConfirmExitDiscard();
    }
}
