package org.zstack.core.errorcode;

import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalErrorCodeI18nServiceImpl implements GlobalErrorCodeI18nService, Component {
    private static final CLogger logger = Utils.getLogger(GlobalErrorCodeI18nServiceImpl.class);

    private static final String I18N_FOLDER = "i18n" + File.separator + "globalErrorCodeMapping";
    private static final String FILE_PREFIX = "global-error-";
    private static final String FILE_SUFFIX = ".json";

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
                    localeMessages.put(locale, messages);
                    logger.info(String.format("loaded %d i18n error messages for locale [%s]",
                            messages.size(), locale));
                } catch (Exception e) {
                    logger.warn(String.format("failed to load i18n file [%s]: %s", path, e.getMessage()), e);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("failed to scan i18n folder: %s", e.getMessage()), e);
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
        if (error == null || locale == null) {
            return;
        }

        if (error.getGlobalErrorCode() != null) {
            String message = getLocalizedMessage(error.getGlobalErrorCode(), locale, error.getFormatArgs());
            if (message != null) {
                error.setMessage(message);
            }
        }

        if (error.getCause() != null) {
            localizeErrorCode(error.getCause(), locale);
        }

        if (error instanceof ErrorCodeList) {
            List<ErrorCode> causes = ((ErrorCodeList) error).getCauses();
            if (causes != null) {
                for (ErrorCode cause : causes) {
                    localizeErrorCode(cause, locale);
                }
            }
        }
    }
}
