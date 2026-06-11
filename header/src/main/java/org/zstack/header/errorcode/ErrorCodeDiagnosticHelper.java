package org.zstack.header.errorcode;

import org.zstack.utils.string.ErrorCodeElaboration;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorCodeDiagnosticHelper {
    public static final String COMPONENT_CLOUD = "Cloud";
    public static final String CATEGORY_OTHER = "Other";
    public static final String OPAQUE_REVIEWED = "diagnosticReviewed";
    public static final String OPAQUE_CAUSE = "diagnosticCause";
    public static final String OPAQUE_SOLUTION = "diagnosticSolution";
    public static final String OPAQUE_NOTE = "diagnosticNote";

    private static final int RESOURCE_CODE_BASE = 10000;
    private static final int ARGUMENT_CODE_BASE = 20000;
    private static final int AUTH_CODE_BASE = 30000;
    private static final int EXTERNAL_CODE_BASE = 40000;
    private static final int INTERNAL_CODE_BASE = 90000;
    private static final int CODE_BAND_SIZE = 10000;
    public static final String FALLBACK_CODE = String.format("%05d", INTERNAL_CODE_BASE);
    private static final String CLOUD_ERROR_CODE_PREFIX = "ORG_ZSTACK_";

    private static final Pattern DIAGNOSTIC_CODE_PATTERN = Pattern.compile(".*?(\\d+)$");
    private static final Pattern CATEGORY_CODE_SUFFIX_PATTERN = Pattern.compile("_\\d+$");
    private static final String[] RESOURCE_KEYWORDS = new String[]{"_VM_", "_HOST_", "_STORAGE_", "_NETWORK_", "_IMAGE_", "_COMPUTE_"};
    private static final String[] ARGUMENT_KEYWORDS = new String[]{"_CONFIG_", "_PARAM_", "_PARAMETER_", "_ARGUMENT_", "_QUERY_"};
    private static final String[] AUTH_KEYWORDS = new String[]{"_IDENTITY_", "_LOGIN_", "_AUTH_", "_ACCOUNT_", "_PERMISSION_", "_IAM_", "_RBAC_"};
    private static final String[] EXTERNAL_KEYWORDS = new String[]{"_DB_", "_DATABASE_", "_AGENT_", "_KVMAGENT_", "_THIRD_PARTY_", "_EXTERNAL_", "_HTTP_", "_REST_CALL_", "_EXTERNALSERVICE_"};

    private ErrorCodeDiagnosticHelper() {
    }

    public static ErrorCodeDiagnostic toDiagnostic(ErrorCode error) {
        return toDiagnostic(error, Locale.getDefault().toString());
    }

    public static ErrorCodeDiagnostic toDiagnostic(ErrorCode error, String locale) {
        ErrorCodeDiagnostic diagnostic = new ErrorCodeDiagnostic();
        diagnostic.setComponent(truncate(COMPONENT_CLOUD, 32));

        if (error == null) {
            diagnostic.setCategory(CATEGORY_OTHER);
            diagnostic.setCode(FALLBACK_CODE);
            diagnostic.setMessage("");
            diagnostic.setRawMessage(toFallbackRawMessage());
            return diagnostic;
        }

        diagnostic.setCategory(resolveCategory(error));
        diagnostic.setCode(resolveCode(error));
        diagnostic.setMessage(truncate(resolveMessage(error), 200));
        diagnostic.setReviewed(isReviewed(error));
        diagnostic.setRawMessage(toRawMessage(error, diagnostic.isReviewed(), locale));
        return diagnostic;
    }

    private static ErrorCodeDiagnostic.RawMessage toRawMessage(ErrorCode error, boolean reviewed, String locale) {
        ErrorCodeDiagnostic.RawMessage raw = new ErrorCodeDiagnostic.RawMessage();
        String symptom = firstNonBlank(resolveElaborationMessage(error, locale), error.getReadableDetails(), error.getMessage(), error.getDescription(), error.getCode());
        raw.setSymptom(truncate(firstNonBlank(symptom, resolveMessage(error), FALLBACK_CODE), 200));

        if (reviewed) {
            raw.setCause(truncate(stringFromOpaque(error, OPAQUE_CAUSE), 500));
            raw.setSolution(truncate(stringFromOpaque(error, OPAQUE_SOLUTION), 1000));
            raw.setNote(truncate(stringFromOpaque(error, OPAQUE_NOTE), 500));
        }

        return raw;
    }

    private static ErrorCodeDiagnostic.RawMessage toFallbackRawMessage() {
        ErrorCodeDiagnostic.RawMessage raw = new ErrorCodeDiagnostic.RawMessage();
        raw.setSymptom("");
        return raw;
    }

    private static String resolveCategory(ErrorCode error) {
        String cloudCode = resolveCloudErrorCode(error);
        if (cloudCode != null) {
            String category = categoryFromCloudErrorCode(cloudCode);
            if (category != null) {
                return category;
            }
        }

        ErrorCode cause = firstCause(error);
        if (cause != null) {
            return resolveCategory(cause);
        }

        return CATEGORY_OTHER;
    }

    // ZCF uses component + category + code as the unique diagnostic key.
    // Cloud modules reuse 10000-based suffixes, so category must come from
    // the stable globalErrorCode prefix instead of display groups like Storage/Network.
    private static String categoryFromCloudErrorCode(String cloudCode) {
        String normalizedCode = cloudCode == null ? "" : cloudCode.toUpperCase(Locale.ROOT).replace('-', '_');
        if (!normalizedCode.startsWith(CLOUD_ERROR_CODE_PREFIX)) {
            return null;
        }

        String category = normalizedCode.substring(CLOUD_ERROR_CODE_PREFIX.length());
        category = CATEGORY_CODE_SUFFIX_PATTERN.matcher(category).replaceFirst("");
        if (!isNotBlank(category)) {
            return null;
        }

        if (category.startsWith("COMPUTE_VM_") || category.equals("COMPUTE_VM")) {
            return category.substring("COMPUTE_".length());
        }

        if (category.startsWith("COMPUTE_HOST_") || category.equals("COMPUTE_HOST")) {
            return category.substring("COMPUTE_".length());
        }

        return category;
    }

    private static String resolveCode(ErrorCode error) {
        String diagnosticCode = toDiagnosticCode(error);
        if (diagnosticCode != null) {
            return diagnosticCode;
        }

        ErrorCode cause = firstCause(error);
        return cause == null ? FALLBACK_CODE : resolveCode(cause);
    }

    private static String resolveCloudErrorCode(ErrorCode error) {
        String code = firstNonBlank(error.getGlobalErrorCode(), error.getCode());
        if (code != null) {
            return code;
        }

        ErrorCode cause = firstCause(error);
        return cause == null ? null : resolveCloudErrorCode(cause);
    }

    private static String toDiagnosticCode(ErrorCode error) {
        String code = resolveCloudErrorCode(error);
        if (code == null) {
            return null;
        }

        Integer suffix = numericSuffix(code);
        if (suffix == null) {
            return null;
        }

        int normalized = suffix % CODE_BAND_SIZE;
        int base = resolveCodeBase(error, code);
        return String.format("%05d", base + normalized);
    }

    private static Integer numericSuffix(String code) {
        Matcher matcher = DIAGNOSTIC_CODE_PATTERN.matcher(code);
        if (!matcher.matches()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Band priority: ARGUMENT > AUTH > EXTERNAL > RESOURCE > CORE/REST > INTERNAL.
    private static int resolveCodeBase(ErrorCode error, String cloudCode) {
        String upperCode = cloudCode == null ? "" : cloudCode.toUpperCase(Locale.ROOT).replace('-', '_');
        String normalizedCode = normalizeCode(cloudCode);

        if (containsAny(normalizedCode, ARGUMENT_KEYWORDS)) {
            return ARGUMENT_CODE_BASE;
        }

        if (containsAny(normalizedCode, AUTH_KEYWORDS)) {
            return AUTH_CODE_BASE;
        }

        if (containsAny(normalizedCode, EXTERNAL_KEYWORDS)) {
            return EXTERNAL_CODE_BASE;
        }

        if (containsAny(normalizedCode, RESOURCE_KEYWORDS)) {
            return RESOURCE_CODE_BASE;
        }

        if (isInvalidArgument(error)) {
            return ARGUMENT_CODE_BASE;
        }

        if (upperCode.startsWith("ORG_ZSTACK_CORE_") || upperCode.startsWith("ORG_ZSTACK_PORTAL_") ||
                upperCode.startsWith("ORG_ZSTACK_REST_")) {
            return INTERNAL_CODE_BASE;
        }

        return INTERNAL_CODE_BASE;
    }

    private static boolean isInvalidArgument(ErrorCode error) {
        return error != null && error.isError(SysErrors.INVALID_ARGUMENT_ERROR);
    }

    private static boolean containsAny(String value, String[] keywords) {
        if (value == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : "_" + code.toUpperCase(Locale.ROOT).replace('-', '_') + "_";
    }

    private static String resolveMessage(ErrorCode error) {
        String message = firstNonBlank(error.getMessage(), error.getDetails(), error.getDescription(), error.getCode());
        if (message != null) {
            return message;
        }

        ErrorCode cause = firstCause(error);
        return cause == null ? "" : resolveMessage(cause);
    }

    private static String resolveElaborationMessage(ErrorCode error, String locale) {
        ErrorCodeElaboration messages = firstMessages(error);
        if (messages == null) {
            return null;
        }

        if (isZh(locale)) {
            return firstNonBlank(messages.getMessage_cn(), messages.getMessage_en());
        }
        return firstNonBlank(messages.getMessage_en(), messages.getMessage_cn());
    }

    private static ErrorCodeElaboration firstMessages(ErrorCode error) {
        ErrorCode current = error;
        while (current != null) {
            if (current.getMessages() != null) {
                return current.getMessages();
            }
            if (current instanceof ErrorCodeList) {
                ErrorCode cause = firstCause(current);
                if (cause != null) {
                    ErrorCodeElaboration causeMessages = firstMessages(cause);
                    if (causeMessages != null) {
                        return causeMessages;
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private static ErrorCode firstCause(ErrorCode error) {
        if (error.getCause() != null) {
            return error.getCause();
        }

        if (error instanceof ErrorCodeList && ((ErrorCodeList) error).getCauses() != null &&
                !((ErrorCodeList) error).getCauses().isEmpty()) {
            return ((ErrorCodeList) error).getCauses().get(0);
        }

        return null;
    }

    private static boolean isReviewed(ErrorCode error) {
        Object reviewed = error.getFromOpaque(OPAQUE_REVIEWED);
        if (reviewed instanceof Boolean) {
            return (Boolean) reviewed;
        }
        if (reviewed instanceof String) {
            return Boolean.parseBoolean((String) reviewed);
        }
        return false;
    }

    private static String stringFromOpaque(ErrorCode error, String key) {
        Object value = error.getFromOpaque(key);
        return value == null ? null : value.toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isZh(String locale) {
        if (locale == null) {
            return Locale.getDefault().getLanguage().equals("zh");
        }
        return locale.replace('-', '_').toLowerCase(Locale.ROOT).startsWith("zh");
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
