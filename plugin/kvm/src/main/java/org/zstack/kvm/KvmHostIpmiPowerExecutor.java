package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostIpmiPowerExecutor;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.data.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * @Author : jingwang
 * @create 2023/4/13 2:25 PM
 */
public class KvmHostIpmiPowerExecutor extends HostIpmiPowerExecutor implements KVMHostConnectExtensionPoint, Component {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    EventFacade evtf;
    @Autowired
    ThreadFacade thdf;
    @Autowired
    CloudBus bus;

    int IPMI_DEFAULT_PORT = 623;
    String IPMI_NONE_VALUE = "None";

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String __name__ = "update-host-ipmi-info";

                final KVMHostInventory inventory = context.getInventory();
                String hostUuid = inventory.getUuid();
                String currentIpmiAddress = HostSystemTags.IPMI_ADDRESS.getTokenByResourceUuid(hostUuid, HostSystemTags.IPMI_ADDRESS_TOKEN);
                HostPowerStatus status = HostPowerStatus.POWER_ON;
                if (IPMI_NONE_VALUE.equals(currentIpmiAddress)) {
                    currentIpmiAddress = null;
                }

                HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
                HostIpmiVO ipmi = host.getIpmi();
                if (isIpmiUnConfigured(ipmi)) {
                    status = HostPowerStatus.UN_CONFIGURED;
                }

                if (ipmi != null) {
                    ipmi.setIpmiAddress(currentIpmiAddress);
                    ipmi.setIpmiPowerStatus(status);
                    dbf.update(ipmi);
                } else {
                    final HostIpmiVO hostIpmiVO = new HostIpmiVO();
                    hostIpmiVO.setUuid(hostUuid);
                    hostIpmiVO.setIpmiAddress(currentIpmiAddress);
                    hostIpmiVO.setIpmiPort(IPMI_DEFAULT_PORT);
                    hostIpmiVO.setIpmiPowerStatus(status);
                    dbf.persist(hostIpmiVO);
                }

                trigger.next();
            }
        };
    }

    @Override
    public boolean start() {


        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
