package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:30 2019/9/24
 */
public class VmPriorityUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(VmPriorityUpgradeExtension.class);

    @Autowired
    private EventFacade evtf;

    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        initRunningVmPriority();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void updateVmPriorityOnHost(HostInventory inv) {
        List<VmInstanceVO> vos = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.hostUuid, inv.getUuid())
                .list();

        List<String> vmUuids = vos.stream().filter(v -> v.getState().equals(VmInstanceState.Running))
                .map(VmInstanceVO::getUuid)
                .collect(Collectors.toList());

        List<String> updatedVms = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid)
                .like(SystemTagVO_.tag, "vmPriority::%")
                .in(SystemTagVO_.resourceUuid, vmUuids)
                .listValues();

        vmUuids.removeAll(updatedVms);

        if (vmUuids.isEmpty()) {
            return;
        }

        new VmPriorityOperator().batchSetVmPriority(vmUuids, VmPriorityLevel.Normal);

        Map<String, VmPriorityLevel> vmLevelMap = new HashMap<>();
        vmUuids.forEach(v-> {
            vmLevelMap.put(v, VmPriorityLevel.Normal);
        });
        UpdateVmPriorityMsg msg = new UpdateVmPriorityMsg();
        msg.setHostUuid(inv.getUuid());
        msg.setVmlevelMap(vmLevelMap);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, inv.getUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                UpdateVmPriorityReply r = new UpdateVmPriorityReply();
                if (!reply.isSuccess()) {
                    logger.warn(String.format("update vms priority failed on host[%s],because %s",
                            inv.getUuid(), reply.getError()));
                }
            }
        });
    }

    private void initRunningVmPriority() {
        if (!VmPriorityGlobalProperty.initRunningVmPriority) {
            return;
        }

        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!d.getNewStatus().equals(HostStatus.Connected.toString())
                    || !d.getInventory().getHypervisorType().equals("KVM")) {
                    return;
                }

                updateVmPriorityOnHost(d.getInventory());
            }
        });
    }
}
