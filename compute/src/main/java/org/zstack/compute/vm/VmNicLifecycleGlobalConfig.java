package org.zstack.compute.vm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class VmNicLifecycleGlobalConfig {
    public static final String CATEGORY = "vmNicLifecycle";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig RECONCILE_TIMEOUT =
            new GlobalConfig(CATEGORY, "reconcileOnHost.timeout");
}
