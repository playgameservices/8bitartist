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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;

/**
 * A View that displays the name, profile picture, and score of a DrawingParticipant.
 */
public class PlayerView extends LinearLayout {

    private Context mContext;

    private ImageView mIconView;
    private TextView mNameView;
    private TextView mScoreView;

    public PlayerView(Context context) {
        this(context, null);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View.inflate(context, R.layout.player_view, this);

        mNameView = (TextView) this.findViewById(R.id.person_name);
        mIconView = (ImageView) this.findViewById(R.id.person_image);
        mScoreView = (TextView) this.findViewById(R.id.person_score);
    }

    /**
     * Update the View to display the information of a DrawingParticipant.
     * @param participant the DrawingParticipant to display.
     */
    public void populateWithParticipant(DrawingParticipant participant) {
        ImageManager imMan = ImageManager.create(mContext);
        if (participant == null) {
            mNameView.setText(mContext.getString(R.string.automatch_player));
            mIconView.setBackground(getResources().getDrawable(R.drawable.none));
        } else {
            mNameView.setText(participant.getDisplayName());
            if (participant.getIconImageUri() != null) {
                imMan.loadImage(new ImageManager.OnImageLoadedListener() {
                    @Override
                    public void onImageLoaded(Uri uri, Drawable drawable, boolean isRequested) {
                        if (drawable != null) {
                            mIconView.setBackground(drawable);
                        }
                    }
                }, participant.getIconImageUri());
            }
        }
        mScoreView.setText(Integer.toString(participant.getScore()));
    }

    /**
     * Set whether the DrawingParticipant displayed is the artist or a guesser. If it is the artist,
     * display a blue border around the PlayerView.
     * @param isArtist true if the DrawingParticipant is the artist, false if it is a guesser.
     */
    public void setIsArtist(boolean isArtist) {
        if (isArtist) {
            this.setBackground(mContext.getResources().getDrawable(R.drawable.artist_bg));
        } else {
            this.setBackground(null);
        }
    }
}
