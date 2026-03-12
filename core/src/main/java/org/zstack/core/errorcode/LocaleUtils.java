package org.zstack.core.errorcode;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocaleUtils {
    private static final CLogger logger = Utils.getLogger(LocaleUtils.class);
    public static final String DEFAULT_LOCALE = "en_US";

    private static final Map<String, String> LANGUAGE_TO_LOCALE = new HashMap<>();

    // Languages that use underscore format in locale file names (e.g. zh_CN, en_US)
    private static final Set<String> UNDERSCORE_LANGS = new HashSet<>(Arrays.asList("zh", "en"));

    static {
        LANGUAGE_TO_LOCALE.put("zh", "zh_CN");
        LANGUAGE_TO_LOCALE.put("en", "en_US");
        LANGUAGE_TO_LOCALE.put("ja", "ja-JP");
        LANGUAGE_TO_LOCALE.put("ko", "ko-KR");
        LANGUAGE_TO_LOCALE.put("de", "de-DE");
        LANGUAGE_TO_LOCALE.put("fr", "fr-FR");
        LANGUAGE_TO_LOCALE.put("ru", "ru-RU");
        LANGUAGE_TO_LOCALE.put("th", "th-TH");
        LANGUAGE_TO_LOCALE.put("id", "id-ID");
    }

    /**
     * Parse Accept-Language header and return the best matching locale key
     * from the set of available locales.
     *
     * @param acceptLanguage the Accept-Language header value
     * @param availableLocales the set of locale keys loaded from JSON files
     * @return the best matching locale key, or en_US as fallback
     */
    public static String resolveLocale(String acceptLanguage, Set<String> availableLocales) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return DEFAULT_LOCALE;
        }

        List<LocaleEntry> entries = parseAcceptLanguage(acceptLanguage);
        for (LocaleEntry entry : entries) {
            if (entry.quality <= 0) {
                continue;
            }

            String normalized = normalizeTag(entry.tag);
            if (availableLocales.contains(normalized)) {
                return normalized;
            }

            String lang = entry.tag.split("[-_]")[0].toLowerCase();
            String mapped = LANGUAGE_TO_LOCALE.get(lang);
            if (mapped != null && availableLocales.contains(mapped)) {
                return mapped;
            }
        }

        return DEFAULT_LOCALE;
    }

    /**
     * Normalize an HTTP language tag to match file locale keys.
     * e.g. "zh-CN" -> "zh_CN", "en-US" -> "en_US", "ja-JP" -> "ja-JP"
     * See UNDERSCORE_LANGS for languages that use underscore format.
     */
    static String normalizeTag(String tag) {
        tag = tag.trim();
        String[] parts = tag.split("[-_]");
        if (parts.length == 2) {
            String lang = parts[0].toLowerCase();
            String region = parts[1].toUpperCase();
            if (UNDERSCORE_LANGS.contains(lang)) {
                return lang + "_" + region;
            }
            return lang + "-" + region;
        }
        return tag;
    }

    private static List<LocaleEntry> parseAcceptLanguage(String header) {
        List<LocaleEntry> entries = new ArrayList<>();
        String[] parts = header.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            String[] tagAndParams = part.split(";");
            if (tagAndParams.length == 0) {
                continue;
            }
            String tag = tagAndParams[0].trim();
            if (tag.isEmpty()) {
                continue;
            }
            double quality = 1.0;
            for (int i = 1; i < tagAndParams.length; i++) {
                String param = tagAndParams[i].trim();
                if (param.startsWith("q=")) {
                    try {
                        quality = Double.parseDouble(param.substring(2).trim());
                    } catch (NumberFormatException e) {
                        logger.debug(String.format("failed to parse quality value [%s]: %s", param, e.getMessage()));
                        quality = 0;
                    }
                }
            }
            entries.add(new LocaleEntry(tag, quality));
        }
        entries.sort((a, b) -> Double.compare(b.quality, a.quality));
        return entries;
    }

    private static class LocaleEntry {
        final String tag;
        final double quality;

        LocaleEntry(String tag, double quality) {
            this.tag = tag;
            this.quality = quality;
        }
    }
}
