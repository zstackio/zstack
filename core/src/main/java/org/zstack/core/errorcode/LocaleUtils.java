package org.zstack.core.errorcode;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

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
     * Uses {@link Locale.LanguageRange#parse(String)} for RFC 7231 compliant
     * parsing with proper q-value priority sorting.
     *
     * @param acceptLanguage the Accept-Language header value
     * @param availableLocales the set of locale keys loaded from JSON files
     * @return the best matching locale key, or en_US as fallback
     */
    public static String resolveLocale(String acceptLanguage, Set<String> availableLocales) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return DEFAULT_LOCALE;
        }

        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(acceptLanguage);
        } catch (IllegalArgumentException e) {
            logger.debug(String.format("failed to parse Accept-Language [%s]: %s", acceptLanguage, e.getMessage()));
            return DEFAULT_LOCALE;
        }

        // ranges are already sorted by q-value descending
        for (Locale.LanguageRange range : ranges) {
            if (range.getWeight() <= 0) {
                continue;
            }

            String tag = range.getRange();
            String normalized = normalizeTag(tag);
            if (availableLocales.contains(normalized)) {
                return normalized;
            }

            String lang = tag.split("[-_]")[0].toLowerCase();
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
}
