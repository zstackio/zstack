package org.zstack.observability;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;

final class ExecutionTimestampParser {
    private ExecutionTimestampParser() {
    }

    static Timestamp parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new Timestamp(Long.parseLong(normalized));
        } catch (NumberFormatException ignored) {
            // Continue with the ISO-8601 and JDBC timestamp forms below.
        }
        try {
            return Timestamp.from(Instant.parse(normalized));
        } catch (DateTimeParseException ignored) {
            try {
                return Timestamp.valueOf(normalized);
            } catch (IllegalArgumentException ignoredAgain) {
                return null;
            }
        }
    }
}
