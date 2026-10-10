package org.zstack.kvm.memory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Unmodified, sanitized real Kylin observation bundled for portable replay. */
final class MemoryKylinFixture {
    private MemoryKylinFixture() { }
    static JsonObject nativeOff() throws Exception {
        try (InputStream stream = MemoryKylinFixture.class.getResourceAsStream("kylin-r8-native-off.json")) {
            if (stream == null) throw new IllegalStateException("missing Kylin observation fixture");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
