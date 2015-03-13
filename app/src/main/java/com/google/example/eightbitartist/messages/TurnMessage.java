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

import java.util.List;

/**
 * Message containing the data relevant to one turn of a match.
 *      int turnNumber - the absolute turn number in this match, beginning at 0 and increasing.
 *      List words - the words to be displayed as guessing choices, in order.
 *      int correctWord - the index into `words` of the correct choice.
 */
public class TurnMessage extends Message {

    public static final String TAG = TurnMessage.class.getSimpleName();

    private int turnNumber;
    private List<String> words;
    private int correctWord;

    /** Default constructor required for Jackson **/
    public TurnMessage() {}

    public TurnMessage(int turnNumber, List<String> words, int correctWord) {
        super(TAG);
        this.turnNumber = turnNumber;
        this.words = words;
        this.correctWord = correctWord;
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public int getCorrectWord() {
        return correctWord;
    }

    public void setCorrectWord(int correctWord) {
        this.correctWord = correctWord;
    }

}
