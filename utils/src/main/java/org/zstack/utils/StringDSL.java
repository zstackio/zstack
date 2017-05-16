package org.zstack.utils;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class StringDSL {
    public static class StringWrapper {
        private String self;

        public StringWrapper(String self) {
            this.self = self;
        }

        public StringWrapper(String...strs) {
            self = "";
            for (String str : strs) {
                self += str;
            }
        }

        @Override
        public String toString() {
            return self;
        }

        private String substitute(Map<String, Object> tokens) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(self);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                Object replacement = tokens.get(matcher.group(1));
                if (replacement != null) {
                    matcher.appendReplacement(buffer, "");
                    buffer.append(replacement.toString());
                }
            }
            matcher.appendTail(buffer);
            return buffer.toString();
        }

        public String format(Object...params) {
            Map<String, Object> tokens = new HashMap<String, Object>();
            for (int i=0; i<params.length; i++) {
                Object p = params[i];
                tokens.put(String.valueOf(i), p != null ? p.toString() : "null");
            }
            return substitute(tokens);
        }

        public String formatByMap(Map tokens) {
            return substitute(tokens);
        }
    }

    public static StringWrapper s(String...strs) {
        return new StringWrapper(strs);
    }

    public static StringWrapper ln(String...strs) {
        String[] ss = new String[strs.length];
        for (int i=0; i<strs.length; i++) {
            ss[i] = strs[i] + '\n';
        }

        return new StringWrapper(ss);
    }

    public static boolean equals(String a, String b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static String stripStart(String str, String prefix) {
        if (!str.startsWith(prefix)) {
            return str;
        }

        return str.substring(prefix.length());
    }

    public static String stripEnd(String str, String suffix) {
        if (!str.endsWith(suffix)) {
            return str;
        }

        return str.substring(0, str.indexOf(suffix));
    }

    public static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    public static String inputStreamToString(InputStream in) {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isZStackUuid(String uuid) {
        return uuid.matches("[0-9a-f]{8}[0-9a-f]{4}[1-5][0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}");
    }
}
