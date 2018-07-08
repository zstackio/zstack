package org.zstack.core.cloudbus;

import com.google.gson.*;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.Message;
import org.zstack.utils.gson.GsonTypeCoder;
import org.zstack.utils.gson.GsonUtil;

import java.lang.reflect.Type;
import java.util.Map;

public class CloudBusGson {
    private static Gson gson = new GsonUtil().setCoder(Message.class, new GsonTypeCoder<Message>() {

        @Override
        public JsonElement serialize(Message message, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jObj = new JsonObject();
            jObj.add(message.getClass().getName(), gson.toJsonTree(message));
            return jObj;
        }

        @Override
        public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jObj = jsonElement.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(String.format("Unable to deserialize class[%s]", className), e);
            }
            return (Message) gson.fromJson(entry.getValue(), clazz);
        }
    }).setExclusionStrategies(new ExclusionStrategy[]{
            new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return fieldAttributes.getAnnotation(GsonTransient.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            }
    }).create();

    public static Message fromJson(String json) {
        return gson.fromJson(json, Message.class);
    }

    public static String toJson(Message msg) {
        return gson.toJson(msg, Message.class);
    }

    public static String toJson(Object obj) {
        if (obj instanceof Message) {
            return gson.toJson(obj, Message.class);
        } else {
            return gson.toJson(obj);
        }
    }
}
