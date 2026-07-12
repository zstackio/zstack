package org.zstack.core.errorcode;

import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalErrorCodeI18nServiceImpl implements GlobalErrorCodeI18nService, Component {
    private static final CLogger logger = Utils.getLogger(GlobalErrorCodeI18nServiceImpl.class);

    private static final String I18N_FOLDER = "i18n" + File.separator + "globalErrorCodeMapping";
    private static final String FILE_PREFIX = "global-error-";
    private static final String FILE_SUFFIX = ".json";
    private static final String CHINESE_ELABORATION_PREFIX = "错误信息: ";
    private static final String ENGLISH_ELABORATION_PREFIX = "Error message: ";

    // locale -> (globalErrorCode -> template)
    private final Map<String, Map<String, String>> localeMessages = new ConcurrentHashMap<>();

    @Override
    public boolean start() {
        loadAllJsonFiles();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void loadAllJsonFiles() {
        try {
            List<String> paths = PathUtil.scanFolderOnClassPath(I18N_FOLDER);
            for (String path : paths) {
                if (!path.endsWith(FILE_SUFFIX)) {
                    continue;
                }

                File file = new File(path);
                String fileName = file.getName();
                if (!fileName.startsWith(FILE_PREFIX)) {
                    continue;
                }

                String locale = fileName.substring(FILE_PREFIX.length(),
                        fileName.length() - FILE_SUFFIX.length());

                try {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    @SuppressWarnings("unchecked")
                    Map<String, String> messages = JSONObjectUtil.toObject(content, LinkedHashMap.class);
                    localeMessages.put(locale, Collections.unmodifiableMap(messages));
                    logger.info(String.format("loaded %d i18n error messages for locale [%s]",
                            messages.size(), locale));
                } catch (Exception e) {
                    logger.warn(String.format("failed to load i18n file [%s]: %s", path, e.getMessage()), e);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("failed to scan i18n folder: %s", e.getMessage()));
        }

        logger.info(String.format("GlobalErrorCodeI18nService loaded %d locales: %s",
                localeMessages.size(), localeMessages.keySet()));
    }

    @Override
    public Set<String> getAvailableLocales() {
        return Collections.unmodifiableSet(localeMessages.keySet());
    }

    @Override
    public String getLocalizedMessage(String globalErrorCode, String locale, String[] formatArgs) {
        if (globalErrorCode == null || locale == null) {
            return null;
        }

        String template = getTemplate(globalErrorCode, locale);
        if (template == null) {
            return null;
        }

        return formatTemplate(template, formatArgs);
    }

    private String getTemplate(String globalErrorCode, String locale) {
        Map<String, String> messages = localeMessages.get(locale);
        if (messages != null) {
            String template = messages.get(globalErrorCode);
            if (template != null) {
                return template;
            }
        }

        // fallback to en_US
        if (!"en_US".equals(locale)) {
            Map<String, String> enMessages = localeMessages.get("en_US");
            if (enMessages != null) {
                return enMessages.get(globalErrorCode);
            }
        }

        return null;
    }

    private String formatTemplate(String template, String[] formatArgs) {
        if (formatArgs == null || formatArgs.length == 0) {
            return template;
        }

        try {
            return String.format(template, (Object[]) formatArgs);
        } catch (Exception e) {
            logger.debug(String.format("failed to format i18n template [%s]: %s", template, e.getMessage()));
            return template;
        }
    }

    @Override
    public void localizeErrorCode(ErrorCode error, String locale) {
        localizeErrorCode(error, locale, false);
    }

    @Override
    public ErrorCode localizeErrorCodeDetails(ErrorCode error, String locale) {
        ErrorCode localized = copyErrorCode(error);
        localizeErrorCode(localized, locale, true);
        return localized;
    }

    private LocalizationResult localizeErrorCode(ErrorCode error, String locale, boolean localizeDetails) {
        if (error == null) {
            return LocalizationResult.notLocalized();
        }

        String resolvedLocale = locale != null ? locale : LocaleUtils.DEFAULT_LOCALE;
        LocalizationResult causeResult = localizeErrorCode(error.getCause(), resolvedLocale, localizeDetails);
        List<LocalizationResult> listCauseResults = localizeListCauses(error, resolvedLocale, localizeDetails);

        String message = null;
        boolean localized = false;
        boolean completelyFormatted = false;
        if (error.getGlobalErrorCode() != null) {
            String template = getTemplate(error.getGlobalErrorCode(), resolvedLocale);
            if (template != null) {
                message = formatTemplate(template, error.getFormatArgs());
                localized = true;
                completelyFormatted = isTemplateCompletelyFormatted(template, error.getFormatArgs());
            }
        }

        if (message == null) {
            LocalizationResult nestedResult = getLocalizedCauseResult(causeResult, listCauseResults);
            message = nestedResult.message;
            localized = nestedResult.localized;
            completelyFormatted = nestedResult.completelyFormatted;
        }

        if (message == null) {
            message = error.getDetails() != null ? error.getDetails() : error.getDescription();
        }

        String resolvedMessage = message != null ? message : (error.getCode() != null ? error.getCode() : "");
        error.setMessage(resolvedMessage);
        if (localizeDetails && localized && completelyFormatted) {
            error.setDetails(resolvedMessage);
        }
        if (localizeDetails) {
            localizeElaboration(error, resolvedLocale);
        }
        return new LocalizationResult(resolvedMessage, localized, completelyFormatted);
    }

    private void localizeElaboration(ErrorCode error, String locale) {
        if (!isNotBlank(error.getElaboration())) {
            return;
        }

        ErrorCodeElaboration messages = error.getMessages();
        if (messages == null) {
            return;
        }

        boolean simplifiedChinese = Locale.SIMPLIFIED_CHINESE.toString().equals(locale);
        String message = simplifiedChinese ? messages.getMessage_cn() : messages.getMessage_en();
        if (!isNotBlank(message)) {
            return;
        }

        String targetPrefix = simplifiedChinese ? CHINESE_ELABORATION_PREFIX : ENGLISH_ELABORATION_PREFIX;
        if (isTemplateCompletelyFormatted(message, null)) {
            error.setElaboration(targetPrefix + message);
            return;
        }

        String elaboration = error.getElaboration();
        if (elaboration.startsWith(CHINESE_ELABORATION_PREFIX)) {
            error.setElaboration(targetPrefix + elaboration.substring(CHINESE_ELABORATION_PREFIX.length()));
        } else if (elaboration.startsWith(ENGLISH_ELABORATION_PREFIX)) {
            error.setElaboration(targetPrefix + elaboration.substring(ENGLISH_ELABORATION_PREFIX.length()));
        }
    }

    private List<LocalizationResult> localizeListCauses(ErrorCode error, String locale, boolean localizeDetails) {
        List<LocalizationResult> results = new ArrayList<>();
        if (error instanceof ErrorCodeList) {
            List<ErrorCode> causes = ((ErrorCodeList) error).getCauses();
            if (causes != null) {
                for (ErrorCode cause : causes) {
                    results.add(localizeErrorCode(cause, locale, localizeDetails));
                }
            }
        }
        return results;
    }

    private LocalizationResult getLocalizedCauseResult(LocalizationResult causeResult,
                                                        List<LocalizationResult> listCauseResults) {
        if (causeResult.localized && isNotBlank(causeResult.message)) {
            return causeResult;
        }

        for (LocalizationResult result : listCauseResults) {
            if (result.localized && isNotBlank(result.message)) {
                return result;
            }
        }

        return LocalizationResult.notLocalized();
    }

    private boolean isTemplateCompletelyFormatted(String template, String[] formatArgs) {
        try {
            Object[] args = formatArgs == null ? new Object[0] : formatArgs;
            String.format(template, args);
            return true;
        } catch (IllegalFormatException e) {
            return false;
        }
    }

    private ErrorCode copyErrorCode(ErrorCode error) {
        if (error == null) {
            return null;
        }

        ErrorCode copy = error.copy();
        copy.setCause(copyErrorCode(error.getCause()));

        if (error instanceof ErrorCodeList) {
            List<ErrorCode> causes = ((ErrorCodeList) error).getCauses();
            if (causes == null) {
                ((ErrorCodeList) copy).setCauses(null);
            } else {
                List<ErrorCode> copiedCauses = new ArrayList<>(causes.size());
                for (ErrorCode cause : causes) {
                    copiedCauses.add(copyErrorCode(cause));
                }
                ((ErrorCodeList) copy).setCauses(copiedCauses);
            }
        }

        return copy;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class LocalizationResult {
        private final String message;
        private final boolean localized;
        private final boolean completelyFormatted;

        private LocalizationResult(String message, boolean localized, boolean completelyFormatted) {
            this.message = message;
            this.localized = localized;
            this.completelyFormatted = completelyFormatted;
        }

        private static LocalizationResult notLocalized() {
            return new LocalizationResult(null, false, false);
        }
    }
}
