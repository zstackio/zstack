package org.zstack.utils;

import java.util.Arrays;

public class ProductVersion {
    private ProductVersion() {
    }

    public static String fromSchemaVersion(String schemaVersion) {
        String[] parts = schemaVersion.split("\\.");
        return String.join(".", Arrays.copyOf(parts, Math.min(parts.length, 3)));
    }
}
