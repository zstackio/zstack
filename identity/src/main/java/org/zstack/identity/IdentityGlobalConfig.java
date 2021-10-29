package org.zstack.identity;

import org.zstack.core.config.*;
import org.zstack.header.core.encrypt.AppointColumn;
import org.zstack.header.core.encrypt.SignedText;

/**
 */
@GlobalConfigDefinition
public class IdentityGlobalConfig {
    public static final String CATEGORY = "identity";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig MAX_CONCURRENT_SESSION = new GlobalConfig(CATEGORY, "session.maxConcurrent");
    @SignedText(tableName = "GlobalConfigVO", primaryKey = "id", signedColumnName = {"value", "defaultValue"},
            appointColumnName = {@AppointColumn(column = "category", vaule = CATEGORY),
                    @AppointColumn(column = "name", vaule = "session.timeout")})
    @GlobalConfigValidation(numberGreaterThan = 0,numberLessThan = 31536000)
    public static GlobalConfig SESSION_TIMEOUT = new GlobalConfig(CATEGORY, "session.timeout");
    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig SESSION_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "session.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig SHOW_ALL_RESOURCE_TO_ADMIN = new GlobalConfig(CATEGORY, "admin.showAllResource");
    @GlobalConfigValidation(notEmpty = false)
    public static GlobalConfig ACCOUNT_API_CONTROL = new GlobalConfig(CATEGORY, "account.api.control");
    @SignedText(tableName = "GlobalConfigVO", primaryKey = "id", signedColumnName = {"value", "defaultValue"},
            appointColumnName = {@AppointColumn(column = "category", vaule = CATEGORY),
                    @AppointColumn(column = "name", vaule = "enable.unique.session")})
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_UNIQUE_SESSION = new GlobalConfig(CATEGORY, "enable.unique.session");
}
