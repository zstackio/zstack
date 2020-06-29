package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicReturnIpFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmNicReturnIpFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmNicInventory nic = (VmNicInventory) data.get(VmInstanceConstant.Params.VmNicInventory.toString());
        final UsedIpInventory ipInv = (UsedIpInventory) data.get(VmInstanceConstant.Params.UsedIPInventory.toString());

        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(ipInv.getL3NetworkUuid());
        msg.setUsedIpUuid(ipInv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ipInv.getL3NetworkUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                    ext.afterDelIpAddress(nic.getUuid(), ipInv.getUuid());
                }

                trigger.next();
            }
        });
    }
}
