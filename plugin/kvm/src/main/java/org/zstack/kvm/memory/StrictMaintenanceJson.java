package org.zstack.kvm.memory;

import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/** Streaming helpers: deliberately consumes original JSON tokens, never a Map/JsonObject. */
final class StrictMaintenanceJson {
    private StrictMaintenanceJson() { }

    static Set<String> begin(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.BEGIN_OBJECT) { throw invalid("expected object"); }
        in.beginObject();
        return new HashSet<>();
    }

    static String nextName(JsonReader in, Set<String> seen, String... allowed) throws IOException {
        String name = in.nextName();
        boolean known = false;
        for (String candidate : allowed) { if (candidate.equals(name)) { known = true; break; } }
        if (!known) { throw invalid("unknown member"); }
        if (!seen.add(name)) { throw invalid("duplicate member"); }
        return name;
    }

    static void end(JsonReader in, Set<String> seen, String... required) throws IOException {
        in.endObject();
        for (String name : required) {
            if (!seen.contains(name)) { throw invalid("missing required member"); }
        }
    }

    static String string(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.STRING) { throw invalid("expected string"); }
        return in.nextString();
    }

    static boolean bool(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.BOOLEAN) { throw invalid("expected boolean"); }
        return in.nextBoolean();
    }

    static long nonNegativeInteger(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.NUMBER) { throw invalid("expected integer number"); }
        String lexeme = in.nextString();
        try {
            long value = new BigDecimal(lexeme).longValueExact();
            if (value < 0) { throw invalid("expected non-negative integer"); }
            return value;
        } catch (NumberFormatException | ArithmeticException e) {
            throw invalid("integer out of range or not exact");
        }
    }

    static JsonParseException invalid(String reason) {
        // Do not echo payload contents into API errors/logs.
        return new JsonParseException("Invalid memory maintenance request JSON: " + reason);
    }
}
