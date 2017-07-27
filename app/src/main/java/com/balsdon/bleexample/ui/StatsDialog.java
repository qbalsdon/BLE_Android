package com.balsdon.bleexample.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.balsdon.bleexample.R;

/*************************************************************************
 *
 * QUINTIN BALSDON CONFIDENTIAL
 * ____________________________
 *
 *  Quintin Balsdon 
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Quintin Balsdon and other contributors,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Quintin Balsdon
 * and its suppliers and may be covered by U.K. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Quintin Balsdon.
 */

public class StatsDialog {

    public enum Type {
        CPU_INFO(R.string.dialog_version_title, R.string.dialog_cpu_info_description, R.drawable.blueberry),
        TEMPERATURE(R.string.dialog_temperature_title, R.string.dialog_temperature_description, R.string.dialog_temperature_unit, R.drawable.thermometer);

        public @StringRes int titleRes;
        public @StringRes int descriptionRes;
        public @StringRes int measurementRes;
        public @DrawableRes int drawableRes;

        Type(@StringRes int title, @StringRes int description, @StringRes int measurement, @DrawableRes int drawable) {
            titleRes = title;
            descriptionRes = description;
            measurementRes = measurement;
            drawableRes = drawable;
        }

        Type(@StringRes int title, @StringRes int description, @DrawableRes int drawable) {
            titleRes = title;
            descriptionRes = description;
            measurementRes = -1;
            drawableRes = drawable;
        }
    }

    public static Dialog create(Activity activity, Type type, String details) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_stat_result, null);
        dialogBuilder.setView(dialogView);

        ((ImageView)dialogView.findViewById(R.id.dialog_stat_result_image)).setImageDrawable(ContextCompat.getDrawable(activity, type.drawableRes));
        ((TextView) dialogView.findViewById(R.id.dialog_stat_result_description)).setText(type.descriptionRes);

        TextView info = (TextView) dialogView.findViewById(R.id.dialog_stat_result_value);
        if (type == Type.TEMPERATURE) {
           info.setText(String.format("%s%s", details, activity.getString(type.measurementRes)));
        } else {
            info.setTextSize(12.0f);
            info.setText(String.format("%s", details));
        }

        dialogBuilder.setTitle(type.titleRes);
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return dialogBuilder.create();
    }
}
