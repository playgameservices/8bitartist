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

import android.net.Uri;

import com.google.android.gms.games.multiplayer.Participant;

/**
 * A player participating in an 8BitArtist match, Can either be a RealTime Multiplayer Participant
 * or a Nearby Connections (Local) endpoint.  This abstraction reduces duplication of logic by
 * handling all participants in the same way, independent of which API the game is actually using.
 */
public class DrawingParticipant {

    // True if the player is connected via Nearby Connections, false otherwise.
    private boolean isLocal;

    // Id used to target messages (for RTMP this is Participant ID, for Nearby this is endpoint ID)
    private String messagingId;

    // Id used to identify the player (for RTMP this is Participant ID, for Nearby this is device ID)
    private String persistentId;

    // The name to display on the PlayerView for this Participant
    private String displayName;

    // A Uri to a picture to display next to the displayName, or null
    private Uri iconImageUri;

    // The participant's score in this round
    private int score;

    /** Default constructor required for Jackson **/
    public DrawingParticipant() {}

    /**
     * Initialize for a Nearby Connections player
     */
    public DrawingParticipant(String endpointId, String deviceId, String name) {
        isLocal = true;
        this.messagingId = endpointId;
        this.persistentId = deviceId;
        this.displayName = name;
        this.iconImageUri = null;
        this.score = 0;
    }

    /**
     * Initialize for a remote (RTMP) player
     */
    public DrawingParticipant(Participant participant) {
        isLocal = false;
        this.messagingId = participant.getParticipantId();
        this.persistentId = participant.getParticipantId();
        this.displayName = participant.getDisplayName();
        this.iconImageUri = participant.getIconImageUri();
        this.score = 0;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        } else if (!(that instanceof DrawingParticipant)) {
            return false;
        } else {
            DrawingParticipant other = (DrawingParticipant) that;
            boolean isMessagingIdEqual = (other.getMessagingId().equals(this.getMessagingId()));
            boolean isPersistentIdEqual = (other.getPersistentId().equals(this.getPersistentId()));
            return (isMessagingIdEqual || isPersistentIdEqual);
        }
    }

    @Override
    public int hashCode() {
      if (persistentId != null && messagingId != null) {
        return (messagingId + persistentId).hashCode();
      } else {
        return super.hashCode();
      }
    }

    public String getMessagingId() {
        return messagingId;
    }

    public void setMessagingId(String messagingId) {
        this.messagingId = messagingId;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Uri getIconImageUri() {
        return iconImageUri;
    }

    public void setIconImageUri(Uri iconImageUri) {
        this.iconImageUri = iconImageUri;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean getIsLocal() {
        return isLocal;
    }

    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }
}
