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

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by wilkinsonclay on 2/21/17.
 */

public class MessageAdapter implements JsonSerializer<Message>,
        JsonDeserializer<Message> {

    private static final String TAG = "MessageAdapter";
    private static final String CLASSNAME_TAG = "_classname";

    /**
     * Gson invokes this call-back method during deserialization when it
     * encounters a field of the specified type.
     * <p>In the implementation of this call-back method, you should consider
     * invoking {@link JsonDeserializationContext#deserialize(JsonElement, Type)}
     * method to create objects for any non-trivial field of the returned
     * object. However, you should never invoke it on the same type passing
     * {@code json} since that will cause an infinite loop (Gson will call your
     * call-back method again).
     *
     * @param json    The Json data being deserialized
     * @param typeOfT The type of the Object to deserialize to
     * @param context
     * @return a deserialized object of the specified type typeOfT which
     * is a subclass of {@code T}
     * @throws JsonParseException if json is not in the expected
     *                            format of {@code typeofT}
     */
    @Override
    public Message deserialize(JsonElement json, Type typeOfT,
                               JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject == null) {
            Log.e(TAG, "Cannot get json object " + json);
            throw new JsonParseException("error parsing");
        }
        JsonPrimitive p = jsonObject.getAsJsonPrimitive(CLASSNAME_TAG);
        if (p == null) {
            Log.e(TAG, "_CLASSNAME IS NULL in " + json);
            throw new JsonParseException("Cannot get classname!");
        }
        String clzname = p.getAsString();

        Class<?> clz;
        try {
            clz = Class.forName(clzname);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Could not find class " + clzname);
            throw new JsonParseException("Could not find class " + clzname);
        }

        return context.deserialize(jsonObject.get("_INSTANCE"), clz);
    }

    /**
     * Gson invokes this call-back method during serialization when it
     * encounters a field of the
     * specified type.
     * <p>
     * <p>In the implementation of this call-back method, you should
     * consider invoking
     * {@link JsonSerializationContext#serialize(Object, Type)} method to
     * create JsonElements for any
     * non-trivial field of the {@code src} object. However, you should never
     * invoke it on the
     * {@code src} object itself since that will cause an infinite loop
     * (Gson will call your
     * call-back method again).</p>
     *
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of
     *                  the source object.
     * @param context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(Message src, Type typeOfSrc,
                                 JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty(CLASSNAME_TAG, src.getClass().getName());
        JsonElement ele = context.serialize(src);
        obj.add("_INSTANCE", ele);
        return obj;
    }
}
