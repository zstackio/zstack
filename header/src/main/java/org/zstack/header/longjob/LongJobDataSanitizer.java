package org.zstack.header.longjob;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Locale;
import java.util.Map;

final class LongJobDataSanitizer {
    private LongJobDataSanitizer() {
    }

    static String sanitize(String data) {
        if (data == null) {
            return null;
        }
        try {
            JsonElement value = new JsonParser().parse(data);
            if (!value.isJsonObject() && !value.isJsonArray()) {
                return "{}";
            }
            return sanitize(value, 0).toString();
        } catch (RuntimeException ignored) {
            return "{}";
        }
    }

    private static JsonElement sanitize(JsonElement value, int depth) {
        if (depth >= 64) {
            return new JsonObject();
        }
        if (value.isJsonObject()) {
            JsonObject safe = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                if (!isCredential(entry.getKey())) {
                    safe.add(entry.getKey(), sanitize(entry.getValue(), depth + 1));
                }
            }
            return safe;
        }
        if (value.isJsonArray()) {
            JsonArray safe = new JsonArray();
            for (JsonElement entry : value.getAsJsonArray()) {
                safe.add(sanitize(entry, depth + 1));
            }
            return safe;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString().trim();
            if (text.startsWith("{") || text.startsWith("[")) {
                try {
                    return new JsonPrimitive(sanitize(new JsonParser().parse(text), depth + 1).toString());
                } catch (RuntimeException ignored) {
                    return new JsonPrimitive("{}");
                }
            }
        }
        return value;
    }

    private static boolean isCredential(String name) {
        String key = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return key.equals("session") || key.equals("sessionuuid") || key.equals("sessionid")
                || key.contains("password") || key.contains("secret") || key.contains("credential")
                || key.endsWith("token") || key.equals("accesskey") || key.equals("privatekey")
                || key.equals("authorization") || key.equals("cookie");
    }
}
