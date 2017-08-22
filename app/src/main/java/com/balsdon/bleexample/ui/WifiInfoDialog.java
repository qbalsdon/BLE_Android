package com.balsdon.bleexample.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RadioGroup;

import com.balsdon.bleexample.Command;
import com.balsdon.pi_ble.R;
import com.balsdon.bleexample.linux.TerminalCommands;

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

public class WifiInfoDialog {

    public static Dialog create(final Activity activity, final String wifiName, final Command<String> command) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_wifi_input, null);
        dialogBuilder.setView(dialogView);

        final TextInputEditText password = (TextInputEditText) dialogView.findViewById(R.id.wifi_password);
        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                password.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        dialogBuilder.setTitle(String.format(activity.getString(R.string.dialog_wifi_title), wifiName));
        final RadioGroup wifiType = (RadioGroup) dialogView.findViewById(R.id.wifi_type);

        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton(android.R.string.ok, null);

        dialogBuilder.setNeutralButton(R.string.forget, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (command != null) {
                    command.execute(String.format(TerminalCommands.FORGET_WIFI, wifiName));
                }
            }
        });

        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                password.requestFocus();
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(password, InputMethodManager.SHOW_IMPLICIT);

                Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String pwd = "";
                        if (wifiType.getCheckedRadioButtonId() == R.id.wifi_type_secure) {
                            if (validate(password)) {
                                pwd = password.getText().toString();
                            } else {
                                int y = 0;
                                return;
                            }
                        }

                        if (command != null) {
                            command.execute(String.format(TerminalCommands.CONNECT_WIFI, wifiName, pwd));
                        }
                        dialog.dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    private static boolean validate(TextInputEditText editText) {
        if (editText == null) return false;
        if (editText.getText() == null) return false;
        String value = editText.getText().toString();
        if (value == null || value.trim().length() == 0) {
            editText.setError(editText.getContext().getString(R.string.error_password_empty));
            return false;
        }

        if (value.trim().length() < 8) {
            editText.setError(editText.getContext().getString(R.string.error_password_too_short));
            return false;
        }

        if (value.trim().length() > 64) {
            editText.setError(editText.getContext().getString(R.string.error_password_too_long));
            return false;
        }

        return true;
    }
}
