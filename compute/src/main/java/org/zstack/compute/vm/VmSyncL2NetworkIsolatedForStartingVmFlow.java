package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkIsolatedAttachOnHostMsg;
import org.zstack.header.network.l2.L2NetworkIsolatedDetachOnHostMsg;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;

import java.util.*;

/**
 * Created by boce.wang on 11/01/2023.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSyncL2NetworkIsolatedForStartingVmFlow implements Flow {

    @Autowired
    protected CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final Map<String, List<String>> vmNicIsolated = new HashMap<>();

        for (VmNicInventory nic : spec.getDestNics()) {
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).find();
            if (l3NetworkVO.getIsolated()) {
                vmNicIsolated.compute(l3NetworkVO.getL2NetworkUuid(), (key, macList) -> {
                    if (macList == null) {
                        return new ArrayList<>(Collections.singletonList(nic.getMac()));
                    }
                    macList.add(nic.getMac());
                    return macList;
                });
            }
        }

        if (vmNicIsolated.keySet().isEmpty()) {
            trigger.next();
            return;
        }

        List<NeedReplyMessage> hmsgList = new ArrayList<>();
        if (spec.getVmInventory().getLastHostUuid() != null &&
                !Objects.equals(spec.getVmInventory().getLastHostUuid(), spec.getDestHost().getUuid())) {
            L2NetworkIsolatedDetachOnHostMsg dmsg = new L2NetworkIsolatedDetachOnHostMsg();
            dmsg.setHostUuid(spec.getDestHost().getUuid());
            dmsg.setMigrateHostUuid(spec.getVmInventory().getLastHostUuid());
            dmsg.setIsolatedL2NetworkMacMap(vmNicIsolated);
            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.L2_PRIVATE_VLAN_SERVICE_ID, spec.getDestHost().getUuid());
            hmsgList.add(dmsg);
        }
        if (spec.getVmInventory().getLastHostUuid() == null || spec.getVmInventory().getLastHostUuid().isEmpty()) {
            L2NetworkIsolatedAttachOnHostMsg amsg = new L2NetworkIsolatedAttachOnHostMsg();
            amsg.setHostUuid(spec.getDestHost().getUuid());
            amsg.setIsolatedL2NetworkMacMap(vmNicIsolated);
            bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.L2_PRIVATE_VLAN_SERVICE_ID, spec.getDestHost().getUuid());
            hmsgList.add(amsg);
        }
        if (hmsgList.isEmpty()) {
            trigger.next();
            return;
        }
        List<ErrorCode> errs = new ArrayList<>();
        new While<>(hmsgList).each((msg, wcompl) -> {
            bus.send(msg, new CloudBusCallBack(wcompl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                        wcompl.allDone();
                    }
                    wcompl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.isEmpty()) {
                    trigger.fail(errs.get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
