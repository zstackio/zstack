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
        List<String> origins =  new ArrayList<String>();
        origins.addAll(splitTagFields(tag));

        List<String> t = new ArrayList<String>();
        t.addAll(splitTagFields(fmt));

        Map<String, String> ret = new HashMap();
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

        List<String> origins =  new ArrayList<String>();
        origins.addAll(splitTagFields(tag));

        List<String> t = new ArrayList<String>();
        t.addAll(splitTagFields(fmt));

        if (fmt.indexOf(TAG_DELIMITER) == -1) {
            return fmt.equals(tag);
        }

        if (origins.size() != t.size()) {
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
