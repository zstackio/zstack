package org.zstack.header.observabilityServer;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by boce.wang on 11/25/2024.
 */
public interface ObservabilityServerConstant {
    @PythonClass
    public static final String OBSERVABILITY_SERVER_VM_TYPE = "ObservabilityServer";
    @PythonClass
    public static final String OBSERVABILITY_SERVER_OFFERING_TYPE = "ObservabilityServer";
    @PythonClass
    public static final String SERVICE_ID = "observabilityServer";
    public static final String ACTION_CATEGORY = "observabilityServer";

}
