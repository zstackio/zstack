package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.zstack.core.progress.ProgressReportService.taskProgress;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAttachL3NetworkToNicFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAttachL3NetworkToNicFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        taskProgress("create nics");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<AttachL3NetworkToVmNicMsg> msgs = new ArrayList<>();
        Map<String, String> vmStaticIps = new StaticIpOperator().getStaticIpbyVmUuid(spec.getVmInventory().getUuid());
        /* reload vmnic */
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, spec.getVmInventory().getUuid()).list();
        for (VmNicVO nic : vmNicVOS) {
            List<String> l3Uuids = VmNicHelper.getCanAttachL3List(VmNicInventory.valueOf(nic), spec.getL3Networks());
            for (String uuid : l3Uuids) {
                AttachL3NetworkToVmNicMsg msg = new AttachL3NetworkToVmNicMsg();
                msg.setL3NetworkUuid(uuid);
                msg.setVmNicUuid(nic.getUuid());
                String staticIp = vmStaticIps.get(uuid);
                if (staticIp != null) {
                    msg.setStaticIp(staticIp);
                }
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getUuid());
                msgs.add(msg);
            }
        }

        List<ErrorCode> errorCodeList = new ArrayList<>();
        new While<>(msgs).each((msg, comp) -> bus.send(msg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    comp.done();
                } else {
                    errorCodeList.add(reply.getError());
                    comp.allDone();
                }
            }
        })).run(new NoErrorCompletion(trigger) {
            @Override
            public void done() {
                if (errorCodeList.isEmpty()) {
                    trigger.next();
                } else {
                    trigger.fail(errorCodeList.get(0));
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<DetachIpAddressFromVmNicMsg> msgs = new ArrayList<>();
        /* reload vmnic */
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, spec.getVmInventory().getUuid()).list();
        for (VmNicVO nic : vmNicVOS) {
            List<String> ipUuids = VmNicHelper.getCanDetachL3List(VmNicInventory.valueOf(nic), spec.getL3Networks());
            for (String uuid : ipUuids) {
                DetachIpAddressFromVmNicMsg msg = new DetachIpAddressFromVmNicMsg();
                msg.setVmNicUuid(nic.getUuid());
                msg.setUsedIpUuid(uuid);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getUuid());
                msgs.add(msg);
            }
        }

        new While<>(msgs).each((msg, comp) -> bus.send(msg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                comp.done();
            }
        })).run(new NoErrorCompletion(chain) {
            @Override
            public void done() {
                chain.rollback();
            }
        });
    }
}
