package org.zstack.identity;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class IdentityGlobalConfig {
    public static final String CATEGORY = "identity";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig MAX_CONCURRENT_SESSION = new GlobalConfig(CATEGORY, "session.maxConcurrent");
    @GlobalConfigValidation(numberGreaterThan = 0,numberLessThan = 31536000)
    public static GlobalConfig SESSION_TIMEOUT = new GlobalConfig(CATEGORY, "session.timeout");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SESSION_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "session.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig SHOW_ALL_RESOURCE_TO_ADMIN = new GlobalConfig(CATEGORY, "admin.showAllResource");
    @GlobalConfigValidation(notEmpty = false)
    public static GlobalConfig ACCOUNT_API_CONTROL = new GlobalConfig(CATEGORY, "account.api.control");
}
