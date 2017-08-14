package com.balsdon.bleexample.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import com.balsdon.tank.R;

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

public class SearchImageView extends android.support.v7.widget.AppCompatImageView {

    private static float HANDLE_FACTOR = 0.8f;

    private PointF zoomPos;
    private PointF center;
    private PointF currentPoint;
    private Paint paint;
    private int sizeOfMagnifier = 100;
    private float zoomScale = 1.8f;
    private Bitmap scaledImage;
    private Bitmap circle;
    private Bitmap original;

    private Paint hourGlassPaint;
    private Paint hourGlassBackPaint;
    private Paint hourGlassBorderPaint;

    private float centerX, centerY, viewX, viewY, radius;


    public SearchImageView(Context context) {
        super(context);
        init();

    }

    public SearchImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.post(new Runnable() {
            @Override
            public void run() {
                original = ((BitmapDrawable) getDrawable()).getBitmap();

                scaledImage = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.blueberry_loading_colour), (int) (original.getWidth() * zoomScale), (int) (original.getHeight() * zoomScale), false);

                center = new PointF(getWidth() / 2.0f, getHeight() / 2.0f);
                zoomPos = new PointF(center.x, center.y);
                paint = new Paint();
                hourGlassPaint = new Paint();
                hourGlassPaint.setColor(Color.BLACK);
                hourGlassPaint.setAntiAlias(true);
                hourGlassPaint.setStrokeWidth(10.0f);

                hourGlassBorderPaint = new Paint();
                hourGlassBorderPaint.setColor(Color.LTGRAY);
                hourGlassBorderPaint.setAntiAlias(true);
                hourGlassBorderPaint.setStrokeWidth(20.0f);

                hourGlassBackPaint = new Paint();
                hourGlassBackPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                hourGlassBackPaint.setAntiAlias(true);
                hourGlassBackPaint.setStrokeWidth(20.0f);

                centerX = getWidth() / 2.0f;
                centerY = getHeight() / 2.0f;
                viewX = centerX - (original.getWidth() / 2);
                viewY = centerY - (original.getHeight() / 2);

                radius = Math.min(original.getWidth() / 2, getHeight() / 2) - ((sizeOfMagnifier / 2.0f) + 25);
                currentPoint = new PointF(viewX + (sizeOfMagnifier / 2.0f), centerY);

                start();
            }
        });
    }

    private void start() {
        moveXY(currentPoint.x, currentPoint.y);
        this.invalidate();

        //ValueAnimator animator = ObjectAnimator.ofFloat(currentPoint.x, (centerX + (original.getWidth() / 2.0f)) - (sizeOfMagnifier / 2.0f));
        ValueAnimator animator = ObjectAnimator.ofFloat(0, 360);
        animator.setDuration(2500);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) Math.toRadians((float) animation.getAnimatedValue());

                currentPoint.x = (float) (centerX + (radius * Math.cos(val)));
                currentPoint.y = (float) (centerY + (radius * Math.sin(val)));

                moveXY();
            }
        });
        animator.start();
    }

    private Bitmap getCroppedBitmap(Bitmap bitmap, Integer cx, Integer cy, Integer radius) {
        int diam = radius << 1;
        Bitmap targetBitmap = Bitmap.createBitmap(diam, diam, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        final int color = 0xff424242;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, -cx + radius, -cy + radius, paint);
        return targetBitmap;
    }

    private void moveXY() {
        moveXY(currentPoint.x, currentPoint.y);
        this.invalidate();
    }

    private boolean moveXY(float x, float y) {
        zoomPos.x = x;
        zoomPos.y = y;

        if (
                x < centerX - (original.getWidth() / 2) ||
                        x > centerX + (original.getWidth() / 2) ||
                        y < centerY - (original.getHeight() / 2) ||
                        y > centerY + (original.getHeight() / 2)
                ) {
            return false;
        }

        float vX = x - viewX;
        float vY = y - viewY;

        int nx = (int) (vX * zoomScale);
        int ny = (int) (vY * zoomScale);

        circle = getCroppedBitmap(scaledImage, nx, ny, sizeOfMagnifier);
        return true;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (circle != null) {
            canvas.drawCircle(zoomPos.x, zoomPos.y, (circle.getWidth() / 2.0f) + 10, hourGlassBorderPaint);

            canvas.drawLine(zoomPos.x,
                    zoomPos.y,
                    zoomPos.x + (circle.getWidth() * HANDLE_FACTOR),
                    zoomPos.y + (circle.getHeight() * HANDLE_FACTOR),
                    hourGlassBorderPaint);

            canvas.drawLine(zoomPos.x,
                    zoomPos.y,
                    zoomPos.x + (circle.getWidth() * HANDLE_FACTOR),
                    zoomPos.y + (circle.getHeight() * HANDLE_FACTOR),
                    hourGlassPaint);

            canvas.drawCircle(zoomPos.x, zoomPos.y, (circle.getWidth() / 2.0f) + 5, hourGlassBackPaint);

            canvas.drawBitmap(circle, zoomPos.x - (circle.getWidth() / 2), zoomPos.y - (circle.getHeight() / 2), paint);
        }
    }
}
