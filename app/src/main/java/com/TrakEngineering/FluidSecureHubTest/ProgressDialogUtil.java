package com.TrakEngineering.FluidSecureHubTest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

public class ProgressDialogUtil {

    public static ProgressDialog createProgressDialog(Context context, String message, boolean showSpannableString) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle("");
        if (showSpannableString) {
            SpannableString ss2 = new SpannableString(message);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            dialog.setMessage(ss2);
        } else {
            dialog.setMessage(message);
        }
        dialog.setCancelable(false);
        return dialog;
    }

    public static void runAnimatedLoadingDots(Activity activity, String messagePrefix, final ProgressDialog progressDialog, boolean showSpannableString) {

        int dotsCount = 0;
        while (progressDialog != null && progressDialog.isShowing()) {

            dotsCount++;
            dotsCount = dotsCount % 5; // looks good w/4 dots

            try {
                Thread.sleep(800);
            }
            catch (Exception ex) {}

            final StringBuffer updateValue = new StringBuffer(messagePrefix);
            for (int i = 0; i < dotsCount; i++) {
                updateValue.append('.');
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (showSpannableString) {
                        SpannableString ss2 = new SpannableString(updateValue.toString());
                        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
                        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
                        progressDialog.setMessage(ss2);
                    } else {
                        progressDialog.setMessage(updateValue.toString());
                    }
                }
            });
        }

    }

}
