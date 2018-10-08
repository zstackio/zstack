package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicAllocateIpFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmNicAllocateIpFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmNicInventory nic = (VmNicInventory) data.get(VmInstanceConstant.Params.VmNicInventory.toString());
        final L3NetworkInventory l3 = (L3NetworkInventory) data.get(VmInstanceConstant.Params.L3NetworkInventory.toString());

        VmNicVO vmNicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.getUuid()).find();

        Map<String, String> vmStaticIps = new StaticIpOperator().getStaticIpbyVmUuid(vmNicVO.getVmInstanceUuid());
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3.getUuid());
        String staticIp = vmStaticIps.get(l3.getUuid());
        if (staticIp != null) {
            msg.setRequiredIp(staticIp);
        } else if (l3.getIpVersion() == IPv6Constants.IPv6){
            l3nm.updateIpAllocationMsg(msg, nic.getMac());
        }
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3.getUuid());

        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateIpReply areply = reply.castReply();
                    UsedIpInventory ipInventory = areply.getIpInventory();
                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                        ext.afterAddIpAddress(vmNicVO.getUuid(), ipInventory.getUuid());
                    }

                    data.put(VmInstanceConstant.Params.UsedIPInventory.toString(), ipInventory);
                    trigger.next();
                } else {
                    trigger.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        UsedIpInventory ipInventory = (UsedIpInventory) data.get(VmInstanceConstant.Params.UsedIPInventory.toString());
        if (ipInventory == null) {
            chain.rollback();
            return;
        }

        final L3NetworkInventory l3 = (L3NetworkInventory) data.get(VmInstanceConstant.Params.L3NetworkInventory.toString());
        final VmNicInventory nic = (VmNicInventory) data.get(VmInstanceConstant.Params.VmNicInventory.toString());
        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(l3.getUuid());
        msg.setUsedIpUuid(ipInventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                    ext.afterDelIpAddress(nic.getUuid(), ipInventory.getUuid());
                }
                chain.rollback();
            }
        });
    }
}
