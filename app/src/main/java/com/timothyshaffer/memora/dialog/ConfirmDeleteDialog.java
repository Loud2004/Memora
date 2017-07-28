package com.timothyshaffer.memora.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import com.timothyshaffer.memora.R;

/**
 * A dialog to confirm the deletion of objects. The type of object to be deleted is displayed
 * in the dialog's title bar, and the description in the dialog's message. If the user confirms
 * deletion then the dialog's callback is invoked.
 */
public class ConfirmDeleteDialog extends DialogFragment {

    private OnConfirmDeleteListener mListener;

    // @param title  The item being deleted in the title bar of the dialog
    public static ConfirmDeleteDialog newInstance(String title, String message) {
        ConfirmDeleteDialog frag = new ConfirmDeleteDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnConfirmDeleteListener) {
            mListener = (OnConfirmDeleteListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + getActivity().getString(R.string.confirm_delete_attach_error));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_delete)
                .setTitle(getActivity().getString(R.string.confirm_delete_title, title))
                .setMessage(Html.fromHtml(getActivity().getString(R.string.confirm_delete_message, title, message)))
                .setPositiveButton(getActivity().getString(R.string.confirm_delete_delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onConfirmDelete();
                    }
                })
                .setNegativeButton(getActivity().getString(R.string.confirm_delete_cancel), new DialogInterface.OnClickListener() {
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
    public interface OnConfirmDeleteListener {
        void onConfirmDelete();
    }
}
