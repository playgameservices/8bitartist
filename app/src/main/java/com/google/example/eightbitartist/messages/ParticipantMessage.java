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
package com.google.example.eightbitartist.messages;

import com.google.example.eightbitartist.DrawingParticipant;

/**
 * Message indicating that a DrawingParticipant has either joined or
 *   left the match.
 * DrawingParticipant drawingParticipant - the participant joining or leaving.
 * boolean isJoining - true if the participant is joining, false if leaving.
 */
public class ParticipantMessage extends Message {

    private DrawingParticipant drawingParticipant;
    private boolean isJoining = true;

    public ParticipantMessage() {
    }

    public ParticipantMessage(DrawingParticipant drawingParticipant) {
        this.drawingParticipant = drawingParticipant;
    }

    public DrawingParticipant getDrawingParticipant() {
        return drawingParticipant;
    }

    public void setDrawingParticipant(DrawingParticipant drawingParticipant) {
        this.drawingParticipant = drawingParticipant;
    }

    public boolean getIsJoining() {
        return isJoining;
    }

    public void setIsJoining(boolean isJoining) {
        this.isJoining = isJoining;
    }
}
