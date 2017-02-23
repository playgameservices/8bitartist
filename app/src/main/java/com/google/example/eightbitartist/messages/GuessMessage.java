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

/**
 * Message containing the information about a guess entered by a
 *   non-artist player.
 * guessIndex - the index (in mTurnWords) of the word chosen by the guesser.
 * potentialPoints - the number of points the guesser should get if
 * the guess is correct.
 * guesserId - the endpointId of the guesser.
 */
public class GuessMessage extends Message {
    private int guessIndex;
    private int potentialPoints;
    private String guesserId;

    public GuessMessage() {
    }

    public GuessMessage(int guessIndex, int potentialPoints, String guesserId) {
        this.guessIndex = guessIndex;
        this.potentialPoints = potentialPoints;
        this.guesserId = guesserId;
    }

    public int getGuessIndex() {
        return guessIndex;
    }

    public void setGuessIndex(int guessIndex) {
        this.guessIndex = guessIndex;
    }

    public int getPotentialPoints() {
        return potentialPoints;
    }

    public void setPotentialPoints(int potentialPoints) {
        this.potentialPoints = potentialPoints;
    }

    public String getGuesserId() {
        return guesserId;
    }

    public void setGuesserId(String guesserId) {
        this.guesserId = guesserId;
    }

}
