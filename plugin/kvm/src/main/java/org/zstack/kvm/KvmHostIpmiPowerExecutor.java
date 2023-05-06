package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostIpmiPowerExecutor;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostIpmiVO;
import org.zstack.header.host.HostVO;

import java.util.Map;

/**
 * @Author : jingwang
 * @create 2023/4/13 2:25 PM
 */
public class KvmHostIpmiPowerExecutor extends HostIpmiPowerExecutor implements KVMHostConnectExtensionPoint {
    @Autowired
    DatabaseFacade dbf;

    int IPMI_DEFAULT_PORT = 623;
    String IPMI_NONE_VALUE = "None";

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        final String hostUuid = context.getInventory().getUuid();
        String currentIpmiAddress = HostSystemTags.IPMI_ADDRESS.getTokenByResourceUuid(hostUuid, HostSystemTags.IPMI_ADDRESS_TOKEN);
        if (IPMI_NONE_VALUE.equals(currentIpmiAddress)) {
            currentIpmiAddress = null;
        }
        HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
        HostIpmiVO ipmi = host.getIpmi();
        if (ipmi != null) {
            ipmi.setIpmiAddress(currentIpmiAddress);
            ipmi.setIpmiPowerStatus(getPowerStatus(ipmi));
            dbf.update(ipmi);
        } else {
            final HostIpmiVO hostIpmiVO = new HostIpmiVO();
            hostIpmiVO.setUuid(hostUuid);
            hostIpmiVO.setIpmiAddress(currentIpmiAddress);
            hostIpmiVO.setIpmiPort(IPMI_DEFAULT_PORT);
            hostIpmiVO.setIpmiPowerStatus(getPowerStatus(hostIpmiVO));
            dbf.persist(hostIpmiVO);
        }

        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                trigger.next();
            }
        };
    }
}
