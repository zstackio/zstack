package org.zstack.utils.gson;

import com.google.gson.ToNumberPolicy;
import com.google.gson.ToNumberStrategy;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;

/**
 * Number strategies for untyped JSON values that preserve the existing Gson
 * representation except where the memory protocol's unsigned generation value
 * cannot be represented safely as a Double.
 */
public final class MemorySafeObjectNumberStrategy {
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger UINT64_MAX = new BigInteger("18446744073709551615");
    private static final BigInteger MAX_SAFE_DOUBLE_INTEGER = new BigInteger("9007199254740991");

    private MemorySafeObjectNumberStrategy() {
    }

    /** Keeps JSONObjectUtil's signed Long behavior and adds exact uint64 overflow values. */
    public static ToNumberStrategy longOrDoubleWithUint64() {
        return new Strategy(ToNumberPolicy.LONG_OR_DOUBLE, false);
    }

    /** Keeps CloudBus's historical Double values inside the safe integer range. */
    public static ToNumberStrategy doubleWithSafeIntegers() {
        return new Strategy(ToNumberPolicy.DOUBLE, true);
    }

    private static final class Strategy implements ToNumberStrategy {
        private final ToNumberStrategy fallback;
        private final boolean preserveSafeDoubleRange;

        private Strategy(ToNumberStrategy fallback, boolean preserveSafeDoubleRange) {
            this.fallback = fallback;
            this.preserveSafeDoubleRange = preserveSafeDoubleRange;
        }

        @Override
        public Number readNumber(JsonReader in) throws IOException {
            boolean lenient = in.isLenient();
            String token = in.nextString();
            String path = in.getPath();
            if (!isBoundedIntegerToken(token)) {
                return fallback(token, lenient, path);
            }

            BigInteger value = new BigInteger(token);
            if (preserveSafeDoubleRange) {
                if (value.abs().compareTo(MAX_SAFE_DOUBLE_INTEGER) <= 0) {
                    return Double.valueOf(token);
                }
                if (value.compareTo(LONG_MIN) >= 0 && value.compareTo(LONG_MAX) <= 0) {
                    return value.longValue();
                }
                if (value.signum() > 0 && value.compareTo(UINT64_MAX) <= 0) {
                    return value;
                }
                return fallback(token, lenient, path);
            }

            if (value.compareTo(LONG_MIN) >= 0 && value.compareTo(LONG_MAX) <= 0) {
                return value.longValue();
            }
            if (value.compareTo(LONG_MAX) > 0 && value.compareTo(UINT64_MAX) <= 0) {
                return value;
            }
            return fallback(token, lenient, path);
        }

        private Number fallback(String token, boolean lenient, String path) throws IOException {
            JsonReader replay = new JsonReader(new StringReader(token)) {
                @Override
                public String getPath() {
                    return path;
                }
            };
            replay.setLenient(lenient);
            return fallback.readNumber(replay);
        }

        private static boolean isBoundedIntegerToken(String token) {
            if (token == null || token.isEmpty()) {
                return false;
            }
            int firstDigit = token.charAt(0) == '-' ? 1 : 0;
            int digits = token.length() - firstDigit;
            if (digits == 0 || digits > 20) {
                return false;
            }
            for (int i = firstDigit; i < token.length(); i++) {
                char c = token.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            return true;
        }
    }
}
