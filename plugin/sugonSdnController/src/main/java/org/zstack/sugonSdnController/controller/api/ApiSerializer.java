/*
 * Copyright (c) 2013 Juniper Networks, Inc. All rights reserved.
 */

package org.zstack.sugonSdnController.controller.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigInteger;

public class ApiSerializer {
    private static class ReferenceSerializer implements JsonSerializer<ObjectReference<? extends ApiPropertyBase>> {
        @Override
        public JsonElement serialize(ObjectReference<? extends ApiPropertyBase> objref, Type type,
                JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.add("to", context.serialize(objref.getReferredName()));
            JsonElement js_attr;
            if (objref.getAttr() == null) {
                js_attr = JsonNull.INSTANCE;
            } else {
                js_attr = context.serialize(objref.getAttr());
            }
            obj.add("attr", js_attr);
            if (objref.getUuid() != null) {
                obj.addProperty("uuid", objref.getUuid());
            }
           return obj;
        }
    }

    private static class NullWritingTypeAdapterFactory implements TypeAdapterFactory {
        @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if(type.getRawType() != ObjectReference.class)
                return null;
            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    boolean writeNulls = out.getSerializeNulls();
                    out.setSerializeNulls(true);
                    delegate.write(out, value);
                    out.setSerializeNulls(writeNulls);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    return delegate.read(in);
                }
            };
        }
    }

    private static final TypeAdapter<Number> UNSIGNED_LONG = new UnsignedLongAdapter();

    private static class UnsignedLongAdapter extends TypeAdapter<Number> {
        private final static BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
        private final static BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
        private final static BigInteger MAX_UNSIGNED = MAX_LONG.subtract(MIN_LONG);

        @Override
        public Number read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            BigInteger value = new BigInteger(in.nextString());

            if(value.compareTo(MAX_UNSIGNED) > 0 || value.compareTo(BigInteger.ZERO) < 0)
                return value;

            if(value.compareTo(MAX_LONG) > 0) {
                BigInteger overflow = value.subtract(MAX_LONG);
                return MIN_LONG.add(overflow.subtract(BigInteger.ONE)).longValue();
            } else {
                return value.longValue();
            }
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
            if(value instanceof Long) {
                long l = value.longValue();
                if(l >= 0L) {
                    out.value(value);
                } else {
                    BigInteger bi = BigInteger.valueOf(l);
                    BigInteger overflow = bi.subtract(MIN_LONG);
                    BigInteger unsigned = MAX_LONG.add(overflow).add(BigInteger.ONE);
                    out.value(unsigned);
                }
            }
            else {
                out.value(value);
            }
        }
    }

    static Gson getDeserializer() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(TypeAdapters.newFactory(long.class, Long.class, UNSIGNED_LONG));
        // Do not attempt to deserialize ApiObjectBase.parent
        return builder.excludeFieldsWithModifiers(Modifier.VOLATILE).setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();
    }

    static Gson getSerializer() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ObjectReference.class, new ReferenceSerializer());
        builder.registerTypeAdapterFactory(new NullWritingTypeAdapterFactory());
        builder.registerTypeAdapterFactory(TypeAdapters.newFactory(long.class, Long.class, UNSIGNED_LONG));
        return builder.create();
    }

    static ApiObjectBase deserialize(String str, Class<? extends ApiObjectBase> cls) {
        Gson json = getDeserializer();
        return json.fromJson(str, cls);
    }

    static String serializeObject(String typename, ApiObjectBase obj) {
        Gson json = getSerializer();
        obj.updateQualifiedName();
        if (obj instanceof VRouterApiObjectBase) {
                JsonElement el =  json.toJsonTree(obj);
                return el.toString();
        }
        JsonObject js_dict = new JsonObject();
        js_dict.add(typename, json.toJsonTree(obj));

        return js_dict.toString();
    }
}
