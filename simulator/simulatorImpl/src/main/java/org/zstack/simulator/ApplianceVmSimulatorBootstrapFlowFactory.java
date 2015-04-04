package org.zstack.simulator;

import org.zstack.appliancevm.ApplianceVmBootstrapFlowFactory;
import org.zstack.core.workflow.Flow;
import org.zstack.core.workflow.FlowTrigger;
import org.zstack.header.simulator.SimulatorConstant;

import java.util.Map;

/**
 */
public class ApplianceVmSimulatorBootstrapFlowFactory implements ApplianceVmBootstrapFlowFactory {
    @Override
    public String getHypervisorTypeForApplianceVmBootstrapFlow() {
        return SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE;
    }

    @Override
    public Flow createApplianceVmBootstrapInfoFlow() {
        return new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                trigger.next();
            }

            @Override
            public void rollback(FlowTrigger trigger, Map data) {
                trigger.rollback();
            }
        };
    }
}
