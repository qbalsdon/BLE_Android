package com.balsdon.bleexample.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class ActionButton extends LinearLayout {

    private Button buttonView;
    private ImageView imageView;

    public ActionButton(Context context) {
        super(context);
    }

    public ActionButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ActionButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        inflate(getContext(), R.layout.action_button, this);


        buttonView = (Button)findViewById(R.id.action_button);
        imageView = (ImageView)findViewById(R.id.action_button_image);

        if (attrs == null) return;
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.action_button, 0, 0);

        setButtonText(typedArray.getResourceId(R.styleable.action_button_displayText, R.string.empty));
        setImage(typedArray.getResourceId(R.styleable.action_button_imageSrc, R.mipmap.ic_launcher));
    }

    public void setButtonText(@StringRes int reference) {
        buttonView.setText(getContext().getString(reference));
    }

    public void setImage(@DrawableRes int reference) {
        imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), reference));
    }
}
