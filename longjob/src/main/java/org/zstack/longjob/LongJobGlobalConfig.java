package org.zstack.longjob;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by kayo on 2018/3/26.
 */
@GlobalConfigDefinition
public class LongJobGlobalConfig {
    public static final String CATEGORY = "longJob";

    @GlobalConfigValidation
    public static GlobalConfig LONG_JOB_DEFAULT_TIMEOUT = new GlobalConfig(CATEGORY, "longJob.api.timeout");

    public static GlobalConfig LONG_JOB_PROGRESS_NOTIFICATION_ENABLED = new GlobalConfig(CATEGORY, "progress.notification.enabled");
    public static GlobalConfig LONG_JOB_NOTIFICATION_TIMELINESS = new GlobalConfig(CATEGORY, "progress.notification.timeliness");
}
