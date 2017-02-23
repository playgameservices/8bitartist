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

import com.google.example.eightbitartist.EPoint;

/**
 * Message that the artist has drawn a point and the recipient
 * should mirror it on their own DrawView instance.
 * EPoint point - the location of the drawn point.
 * int color - the index of the drawn color in the array of colors.
 */
public class EPointMessage extends Message {

    private EPoint point;
    private int color;

    public EPointMessage() {
    }

    public EPointMessage(EPoint point, int color) {
        this.point = point;
        this.color = color;
    }

    public EPoint getPoint() {
        return point;
    }

    public void setPoint(EPoint point) {
        this.point = point;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
