package org.zstack.core.errorcode;

import org.zstack.header.errorcode.ErrorCode;

public interface GlobalErrorCodeI18nService {
    /**
     * Get localized message for a globalErrorCode.
     *
     * @param globalErrorCode the global error code key
     * @param locale the locale key (e.g. "zh_CN", "ja-JP")
     * @param formatArgs optional format arguments for %s placeholders
     * @return the localized message, or null if not found
     */
    String getLocalizedMessage(String globalErrorCode, String locale, String[] formatArgs);

    /**
     * Recursively localize an ErrorCode and its cause chain,
     * setting the message field on each ErrorCode.
     *
     * @param error the ErrorCode to localize
     * @param locale the locale key
     */
    void localizeErrorCode(ErrorCode error, String locale);

    /**
     * Get the set of available locale keys loaded from JSON files.
     */
    java.util.Set<String> getAvailableLocales();
}
