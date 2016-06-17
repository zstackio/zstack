package org.zstack.core.logging;

import org.apache.commons.lang.LocaleUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Locale;

/**
 * Created by xing5 on 2016/5/30.
 */
public class Log4jBackend implements LogBackend {
    private static final CLogger logger = Utils.getLogger(Log4jBackend.class);

    @Override
    public void writeLog(Log log) {
        if (LogGlobalProperty.LOG4j_BACKEND_ON) {
            logger.debug(JSONObjectUtil.toJsonString(log.getContent()));
        }
    }

    @Override
    public Locale getCurrentLocale() {
        return LocaleUtils.toLocale(CoreGlobalProperty.LOCALE);
    }
}
