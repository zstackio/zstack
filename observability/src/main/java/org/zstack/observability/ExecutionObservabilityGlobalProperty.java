package org.zstack.observability;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ExecutionObservabilityGlobalProperty {
    @GlobalProperty(name = "ExecutionObservability.enabled", defaultValue = "false")
    public static boolean ENABLED;
}
