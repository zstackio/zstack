package org.zstack.header.longjob;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class LongJobDataSanitizer {
    private static final TypeAdapter<JsonElement> JSON_ELEMENT = new Gson().getAdapter(JsonElement.class);
    private static final Pattern URI_PREFIX = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");

    private LongJobDataSanitizer() {
    }

    static String sanitize(String data) {
        if (data == null) {
            return null;
        }
        try {
            JsonElement value = parseJson(data);
            if (!value.isJsonObject() && !value.isJsonArray()) {
                return "{}";
            }
            return sanitize(value, 0).toString();
        } catch (RuntimeException ignored) {
            return "{}";
        }
    }

    private static JsonElement parseJson(String text) {
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            JsonElement value = JSON_ELEMENT.read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("trailing data after JSON value");
            }
            return value;
        } catch (IOException e) {
            throw new JsonSyntaxException(e);
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
            String original = value.getAsString();
            String text = original.trim();
            if (text.startsWith("{") || text.startsWith("[") || text.startsWith("\"") || text.startsWith("\\")) {
                try {
                    JsonElement parsed = parseJson(text);
                    JsonElement safe = sanitize(parsed, depth + 1);
                    return safe.equals(parsed) ? value : new JsonPrimitive(safe.toString());
                } catch (RuntimeException ignored) {
                    if (hasJsonStructure(text)) {
                        return new JsonPrimitive("{}");
                    }
                }
            }
            return new JsonPrimitive(stripUriUserInfo(original));
        }
        return value;
    }

    private static boolean hasJsonStructure(String text) {
        int start = 0;
        while (start < text.length() && (Character.isWhitespace(text.charAt(start))
                || text.charAt(start) == '"' || text.charAt(start) == '\\')) {
            start++;
        }
        if (start == text.length() || (text.charAt(start) != '{' && text.charAt(start) != '[')) {
            return false;
        }
        int first = start + 1;
        while (first < text.length() && Character.isWhitespace(text.charAt(first))) {
            first++;
        }
        if (first == text.length()) {
            return true;
        }
        char token = text.charAt(first);
        if (token == '{' || token == '[' || token == '}' || token == ']' || token == '-'
                || Character.isDigit(token)) {
            return true;
        }
        for (int i = first; i < text.length(); i++) {
            if (":,\"'\\".indexOf(text.charAt(i)) >= 0) {
                return true;
            }
        }
        int end = first;
        while (end < text.length() && Character.isLetter(text.charAt(end))) {
            end++;
        }
        String literal = text.substring(first, end);
        return !literal.isEmpty() && ("true".startsWith(literal) || "false".startsWith(literal)
                || "null".startsWith(literal));
    }

    private static String stripUriUserInfo(String original) {
        String text = original.trim();
        if (!URI_PREFIX.matcher(text).find()) {
            return original;
        }
        try {
            URI uri = new URI(text);
            String authority = uri.getRawAuthority();
            if (authority == null) {
                return text.contains("@") ? "{}" : original;
            }
            int at = authority.lastIndexOf('@');
            if (at < 0) {
                return original;
            }
            int authorityStart = text.indexOf("://") + 3;
            String safe = text.substring(0, authorityStart) + authority.substring(at + 1)
                    + text.substring(authorityStart + authority.length());
            int offset = original.indexOf(text);
            return original.substring(0, offset) + safe + original.substring(offset + text.length());
        } catch (URISyntaxException ignored) {
            return text.contains("@") ? "{}" : original;
        }
    }

    private static boolean isCredential(String name) {
        String key = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return key.equals("session") || key.equals("sessionuuid") || key.equals("sessionid")
                || key.contains("password") || key.contains("secret") || key.contains("credential")
                || key.endsWith("token") || key.endsWith("accesskey") || key.endsWith("privatekey")
                || key.endsWith("apikey")
                || key.equals("authorization") || key.equals("cookie");
    }
}
