/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.example.eightbitartist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * View that acts as a palette to let players choose colors.
 */
public class ColorChooser extends View implements OnTouchListener {

    private static final String TAG = "ColorChooser";

    // Allow me to retrieve a list of colors, and someone to send selection events to.
    public interface ColorChooserListener {
        // Set selected color
        void setColor(short color);

        // Find out what color is selected
        short getColor();
    }

    private ColorChooserListener mListener;

    private final Paint p = new Paint();
    private final RectF mRect = new RectF();
    private final RectF mInnerRect = new RectF();

    public ColorChooser(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(this);
    }

    public void setDrawView(ColorChooserListener ccl) {
        mListener = ccl;

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mListener == null) {
            Log.d(TAG, "You appear to have not set a ColorChooser listener.");
            return;
        }

        canvas.drawColor(0xFF000000);

        // draw each of the color swatches in the palette
        for (int i = 0; i < DrawView.COLOR_MAP.length; i++) {
            // Outer rectangle
            mRect.top = (i * getHeight()) / DrawView.COLOR_MAP.length;
            mRect.bottom = ((i + 1) * getHeight()) / DrawView.COLOR_MAP.length;
            mRect.left = 0;
            mRect.right = getWidth();

            // Inner rectangle
            mInnerRect.top = mRect.top + 10;
            mInnerRect.bottom = mRect.bottom - 10;
            mInnerRect.left = 10;
            mInnerRect.right = mRect.right - 10;

            if (mListener.getColor() == i) {
                // Blue border
                p.setColor(0xFF81D4FA);
                canvas.drawRect(mRect, p);

                // Color square
                p.setColor(DrawView.COLOR_MAP[i]);
                canvas.drawRect(mInnerRect, p);
            } else {
                // Color not selected, no border
                p.setColor(DrawView.COLOR_MAP[i]);
                canvas.drawRect(mRect, p);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mListener == null) {
            Log.d(TAG, "You appear to have not set a ColorChooser listener.");
            return false;
        }

        mListener.setColor((short) Math.floor(event.getY()
                / ((getHeight() + 1.0) / DrawView.COLOR_MAP.length)));

        invalidate();

        return false;
    }
}
