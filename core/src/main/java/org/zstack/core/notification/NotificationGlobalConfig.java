package org.zstack.core.notification;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by xing5 on 2017/5/2.
 */
@GlobalConfigDefinition
public class NotificationGlobalConfig {
    public static final String CATEGORY = "notification";

    @GlobalConfigValidation(notNull = false)
    public static GlobalConfig WEBHOOK_URL = new GlobalConfig(CATEGORY, "webhook.url");
}
