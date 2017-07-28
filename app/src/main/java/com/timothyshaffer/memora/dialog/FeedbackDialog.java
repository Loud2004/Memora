package com.timothyshaffer.memora.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import com.timothyshaffer.memora.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;


public class FeedbackDialog extends DialogFragment {
    public static final String SEND_FEEDBACK_URL = "http://www.timothyshaffer.com/website/feedback/index.php";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View rootView = inflater.inflate(R.layout.dialog_feedback, null);
        final TextInputLayout inputLayout = (TextInputLayout) rootView.findViewById(R.id.feedback_input_layout);
        final EditText input = (EditText) rootView.findViewById(R.id.feedback_input);

        final View parentView = getActivity().findViewById(android.R.id.content);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(rootView)
                // Add action buttons
                .setPositiveButton(R.string.feedback_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Check for empty content
                        String strFeedback = input.getText().toString().trim();
                        if (strFeedback.isEmpty()) {
                            // Don't send feedback, show SnackBar with error message to the user
                            Snackbar.make(parentView, "Cannot send empty feedback", Snackbar.LENGTH_LONG).show();
                        } else {
                            // Find out which RadioButon was selected
                            RadioButton radioBug = (RadioButton) rootView.findViewById(R.id.radio_bug);
                            String radioSelected = "";
                            if (radioBug.isChecked()) {
                                radioSelected = "BUG";
                            } else {
                                radioSelected = "SUGGESTION";
                            }

                            // Build a JSON object to hold all the data we wish to send as feedback
                            JSONObject json = new JSONObject();
                            try {
                                // Include some meta-data about this feedback
                                // Timestamp
                                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
                                String datetime = df.format(Calendar.getInstance().getTime());
                                json.put("timestamp", datetime);
                                // Device Mfg, Model, and Android Version
                                json.put("mfg", Build.MANUFACTURER);
                                json.put("model", Build.MODEL);
                                json.put("sdk", Build.VERSION.SDK_INT);
                                // User UUID
                                json.put("uuid", getUUID());
                                // App Name
                                String packageName = getActivity().getPackageName();
                                json.put("package", packageName);
                                // App Version Name and Code
                                PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(packageName, 0);
                                json.put("version_code", packageInfo.versionCode);
                                json.put("version_name", packageInfo.versionName);
                                // Add any extra info we may need, e.g. which activity was active when feedback was sent
                                json.put("extra", getActivity().getClass().getSimpleName());
                                // Include the actual feedback
                                json.put("type", radioSelected);
                                json.put("comment", strFeedback);
                            } catch (JSONException | NameNotFoundException e) {
                                // Display Error message to user
                                Snackbar.make(parentView, "Internal Error: Feedback not sent", Snackbar.LENGTH_LONG).show();
                            }

                            // POST this data on a separate background thread
                            Thread t = new Thread(new FeedbackRunnable(json, ""));
                            t.start();

                            // Send feedback using background task
                            Snackbar.make(parentView, "Feedback Sent", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.feedback_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //FeedbackDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }


    /**
     * A class to send user feedback to a web server.
     * This Runnable will perform its work on a separate thread so that it doesn't affect the UI.
     */
    private class FeedbackRunnable implements Runnable {
        private JSONObject jsonFeedback;
        private String jpegPath;

        FeedbackRunnable(JSONObject jsonFeedback, String jpegPath) {
            this.jsonFeedback = jsonFeedback;
            this.jpegPath = jpegPath;
        }

        public void run() {
            // Check connectivity
            ConnectivityManager connMgr = (ConnectivityManager)
                    getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            // If the network is connected then send the POST
            if (networkInfo != null && networkInfo.isConnected()) {
                try {
                    // Setup static variables for POSTing data
                    String attachmentName = "picture";
                    String attachmentFileName = "picture.jpg";
                    String crlf = "\r\n";
                    String twoHyphens = "--";
                    // boundary is any random string of data that will never appear in the data we are sending
                    String boundary = "---------------------------arglebargle";

                    //Setup the request:
                    URL url = new URL(SEND_FEEDBACK_URL);
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setUseCaches(false);
                    httpUrlConnection.setDoOutput(true);
                    httpUrlConnection.setRequestMethod("POST");
                    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                    httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    //Start content wrapper
                    DataOutputStream postData = new DataOutputStream(httpUrlConnection.getOutputStream());

                    // JSON data
                    postData.writeBytes(twoHyphens + boundary + crlf);
                    postData.writeBytes("Content-Disposition: form-data; name=\"feedback\"" + crlf);
                    postData.writeBytes("Content-Type: application/json" + crlf + crlf);
                    String json = jsonFeedback.toString();
                    postData.writeBytes( json + crlf);

                    // Binary data
                    if (!jpegPath.isEmpty()) {
                        postData.writeBytes(twoHyphens + boundary + crlf);
                        postData.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\"" + attachmentFileName + "\"" + crlf);
                        postData.writeBytes("Content-Type: image/jpeg" + crlf + crlf);
                        // Compress bitmap to JPEG and write the binary picture data
                        //thumbnailBmp.compress(Bitmap.CompressFormat.JPEG, 70, postData);  // This is just the thumbnail
                        Bitmap fullsize = BitmapFactory.decodeFile(jpegPath);
                        fullsize.compress(Bitmap.CompressFormat.JPEG, 70, postData);
                        postData.writeBytes(crlf);
                    }

                    // End boundary (has extra hyphens after the boundary mark)
                    postData.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

                    // Flush output buffer (make sure everything is written)
                    postData.flush();
                    postData.close();

                    // Read the response
                    int responseCode = httpUrlConnection.getResponseCode();
                    if( responseCode == 200 ){
                        // TODO: The feedback was sent successfully, let the user know with a SnackBar
                    }
                    /*
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = reader.readLine();
                    // Read Server Response
                    while (line != null) {
                        sb.append(line);
                        line = reader.readLine();
                    }
                    reader.close();
                    */
                    httpUrlConnection.disconnect();
                    // TODO: Make sure we have response code 200 (success)
                    //return sb.toString();
                } catch (Exception e) {
                    // TODO: Display Error SnackBar
                    //return "Exception: " + e.getMessage();
                }
            } else {
                // TODO: Display Error SnackBar
                //textView.setText("No network connection available.");
            }
        }
    }


    private String getUUID() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String uuid = sharedPref.getString("UUID", "");
        if (uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("UUID", uuid);
            editor.commit();
        }
        return uuid;
    }

}
