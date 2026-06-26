package org.zstack.core.scim;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class ScimGlobalConfig {
    public static final String CATEGORY = "scim";

    @GlobalConfigDef(type = Boolean.class, defaultValue = "false",
            description = "Whether the SCIM receiver accepts incoming requests. Disabling it removes SCIM-synchronized resources.")
    public static GlobalConfig RECEIVER_ENABLED = new GlobalConfig(CATEGORY, "receiver.enabled");

    @GlobalConfigValidation(notEmpty = false)
    @GlobalConfigDef(defaultValue = "", description = "Bearer token accepted by the SCIM receiver")
    public static GlobalConfig RECEIVER_TOKEN = new GlobalConfig(CATEGORY, "receiver.token");

    @GlobalConfigValidation(notEmpty = false)
    @GlobalConfigDef(defaultValue = "", description = "Secret used to verify SCIM request signatures")
    public static GlobalConfig RECEIVER_SIGNATURE_SECRET = new GlobalConfig(CATEGORY, "receiver.signatureSecret");
}
