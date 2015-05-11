package org.zstack.appliancevm;

import org.zstack.core.workflow.Flow;

/**
 */
public interface ApplianceVmBootstrapFlowFactory {
    String getHypervisorTypeForApplianceVmBootstrapFlow();

    Flow createApplianceVmBootstrapInfoFlow();
}
