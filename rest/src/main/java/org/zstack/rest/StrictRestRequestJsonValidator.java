package org.zstack.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.zstack.header.rest.StrictRestRequestJson;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

/** Opt-in raw request validation; call with HttpEntity.getBody() before Map conversion. */
public final class StrictRestRequestJsonValidator {
    private static final Gson GSON = new GsonBuilder().create();
    private StrictRestRequestJsonValidator() { }

    public static void validateIfOptedIn(String rawBody, String actionWrapper, Class<?> apiClass) {
        if (!apiClass.isAnnotationPresent(StrictRestRequestJson.class)) { return; }
        if (rawBody == null || actionWrapper == null) { throw invalid("missing request body/action wrapper"); }
        try (JsonReader in = new JsonReader(new StringReader(rawBody))) {
            in.setLenient(false);
            if (in.peek() != JsonToken.BEGIN_OBJECT) { throw invalid("root must be an object"); }
            in.beginObject();
            boolean selected = false;
            TypeAdapter<?> adapter = GSON.getAdapter(apiClass);
            while (in.hasNext()) {
                String name = in.nextName();
                if (!actionWrapper.equals(name)) { in.skipValue(); continue; }
                if (selected) { throw invalid("duplicate action wrapper"); }
                selected = true;
                rejectDuplicateObjectKeys(in);
                // Re-read the selected wrapper through the API adapter so DTO adapters
                // validate their original tokens; the scan above catches duplicates
                // in reflective API fields before Gson can collapse them.
                try (JsonReader actionReader = new JsonReader(new StringReader(
                        extractSelectedAction(rawBody, actionWrapper)))) {
                    actionReader.setLenient(false);
                    adapter.read(actionReader);
                    if (actionReader.peek() != JsonToken.END_DOCUMENT) {
                        throw invalid("trailing action JSON");
                    }
                }
            }
            in.endObject();
            if (!selected) { throw invalid("missing action wrapper"); }
            if (in.peek() != JsonToken.END_DOCUMENT) { throw invalid("trailing JSON content"); }
        } catch (IOException e) {
            throw invalid("malformed JSON");
        }
    }

    private static void rejectDuplicateObjectKeys(JsonReader selectedValue) throws IOException {
        // JsonReader is forward-only. Copy the selected value's raw JSON token stream
        // while validating each object scope; keeping a Set per object preserves
        // duplicate detection at every nesting level without a Map/JsonObject pass.
        scanValue(selectedValue);
    }

    private static void scanValue(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.BEGIN_OBJECT) {
            in.beginObject();
            Set<String> names = new HashSet<>();
            while (in.hasNext()) {
                String name = in.nextName();
                if (!names.add(name)) { throw invalid("duplicate object member"); }
                scanValue(in);
            }
            in.endObject();
        } else if (token == JsonToken.BEGIN_ARRAY) {
            in.beginArray();
            while (in.hasNext()) { scanValue(in); }
            in.endArray();
        } else {
            in.skipValue();
        }
    }

    private static String extractSelectedAction(String rawBody, String actionWrapper) {
        // The action value is validated separately using a token scan above. This
        // helper returns its exact JSON value through a tiny raw-token copier.
        try (JsonReader in = new JsonReader(new StringReader(rawBody))) {
            in.setLenient(false);
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if (actionWrapper.equals(name)) { return copyValue(in); }
                in.skipValue();
            }
        } catch (IOException e) {
            throw invalid("malformed JSON");
        }
        throw invalid("missing action wrapper");
    }

    private static String copyValue(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.BEGIN_OBJECT) {
            StringBuilder out = new StringBuilder("{");
            in.beginObject();
            boolean first = true;
            while (in.hasNext()) {
                if (!first) { out.append(','); }
                first = false;
                out.append(GSON.toJson(in.nextName()));
                out.append(':').append(copyValue(in));
            }
            in.endObject();
            return out.append('}').toString();
        }
        if (token == JsonToken.BEGIN_ARRAY) {
            StringBuilder out = new StringBuilder("[");
            in.beginArray();
            boolean first = true;
            while (in.hasNext()) {
                if (!first) { out.append(','); }
                first = false;
                out.append(copyValue(in));
            }
            in.endArray();
            return out.append(']').toString();
        }
        if (token == JsonToken.STRING) { return GSON.toJson(in.nextString()); }
        if (token == JsonToken.NUMBER) { return in.nextString(); }
        if (token == JsonToken.BOOLEAN) { return Boolean.toString(in.nextBoolean()); }
        if (token == JsonToken.NULL) { in.nextNull(); return "null"; }
        throw invalid("invalid action value");
    }

    private static JsonParseException invalid(String message) {
        return new JsonParseException("Invalid strict REST request JSON: " + message);
    }
}
