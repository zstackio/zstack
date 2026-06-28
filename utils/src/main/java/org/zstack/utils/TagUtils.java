package org.zstack.utils;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.*;

/**
 */
public class TagUtils {
    private static final String TAG_DELIMITER = "::";
    private static final char TOKEN_START = '{';
    private static final char TOKEN_END = '}';

    public static Map<String, String> parse(String fmt, String tag) {
        List<String> t = new ArrayList<String>();
        t.addAll(splitTagFields(fmt));

        List<String> origins = splitTagFieldsByFormat(t, tag);
        Map<String, String> ret = new HashMap();
        if (origins == null) {
            return ret;
        }

        for (int i=0;i<t.size(); i++) {
            String key = t.get(i);
            if (!key.startsWith("{") || !key.endsWith("}")) {
                continue;
            }

            key = key.replaceAll("\\{", "").replaceAll("\\}", "");
            if (i < origins.size()) {
                ret.put(key, origins.get(i));
            }
        }

        return ret;
    }

    public static boolean isMatch(String fmt, String tag) {
        char fmtFirstChar = fmt.charAt(0);
        if (!Strings.isEmpty(tag) && fmtFirstChar != tag.charAt(0) && fmtFirstChar != '{') {
            return false;
        }

        List<String> t = new ArrayList<String>();
        t.addAll(splitTagFields(fmt));

        if (fmt.indexOf(TAG_DELIMITER) == -1) {
            return fmt.equals(tag);
        }

        List<String> origins = splitTagFieldsByFormat(t, tag);
        if (origins == null || origins.size() != t.size()) {
            return false;
        }

        for (int i=0; i<t.size(); i++) {
            String fmtKey = t.get(i);
            if (fmtKey.startsWith("{") && fmtKey.endsWith("}")) {
                continue;
            }

            String originKey = origins.get(i);
            if (!originKey.equals(fmtKey)) {
                return false;
            }
        }

        return true;
    }

    private static List<String> splitTagFieldsByFormat(List<String> fmtFields, String tag) {
        List<String> fields = new ArrayList<>();
        int offset = 0;

        for (int i = 0; i < fmtFields.size(); i++) {
            String fmtField = fmtFields.get(i);
            boolean lastField = i == fmtFields.size() - 1;
            if (isTokenField(fmtField)) {
                if (lastField) {
                    fields.add(tag.substring(offset));
                    offset = tag.length();
                    continue;
                }

                int end = indexOfDelimiterOutsideToken(tag, offset);
                if (end < 0) {
                    return null;
                }
                fields.add(tag.substring(offset, end));
                offset = end + TAG_DELIMITER.length();
                continue;
            }

            if (!tag.startsWith(fmtField, offset)) {
                return null;
            }

            fields.add(fmtField);
            offset += fmtField.length();
            if (!lastField) {
                if (!tag.startsWith(TAG_DELIMITER, offset)) {
                    return null;
                }
                offset += TAG_DELIMITER.length();
            }
        }

        return offset == tag.length() ? fields : null;
    }

    private static boolean isTokenField(String field) {
        return field.startsWith(String.valueOf(TOKEN_START)) && field.endsWith(String.valueOf(TOKEN_END));
    }

    private static List<String> splitTagFields(String tag) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        int braceDepth = 0;

        for (int i = 0; i < tag.length(); i++) {
            char current = tag.charAt(i);
            if (current == TOKEN_START) {
                braceDepth++;
            } else if (current == TOKEN_END && braceDepth > 0) {
                braceDepth--;
            }

            if (braceDepth == 0 && tag.startsWith(TAG_DELIMITER, i)) {
                fields.add(field.toString());
                field.setLength(0);
                i += TAG_DELIMITER.length() - 1;
                continue;
            }

            field.append(current);
        }

        fields.add(field.toString());
        while (!fields.isEmpty() && fields.get(fields.size() - 1).isEmpty()) {
            fields.remove(fields.size() - 1);
        }

        return fields;
    }

    private static int indexOfDelimiterOutsideToken(String tag, int offset) {
        int braceDepth = 0;
        for (int i = offset; i < tag.length(); i++) {
            char current = tag.charAt(i);
            if (current == TOKEN_START) {
                braceDepth++;
            } else if (current == TOKEN_END && braceDepth > 0) {
                braceDepth--;
            }

            if (braceDepth == 0 && tag.startsWith(TAG_DELIMITER, i)) {
                return i;
            }
        }

        return -1;
    }

    public static Map<String, String> parseIfMatch(String fmt, String tag) {
        if (!isMatch(fmt, tag)) {
            return null;
        }

        return parse(fmt, tag);
    }

    public static String tagPatternToSqlPattern(Enum tag) {
        return tagPatternToSqlPattern(tag.toString());
    }

    public static String tagPatternToSqlPattern(String tag) {
        return tag.replaceAll("\\{(.+?)\\}", "%");
    }
}
