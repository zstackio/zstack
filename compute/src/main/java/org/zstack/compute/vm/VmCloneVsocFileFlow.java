package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.CoreGlobalProperty.PLATFORM_ID;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCloneVsocFileFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmCloneVsocFileFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private List<VmBeforeCreateOnHypervisorExtensionPoint> exts;

    @Override
    public void run(FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        VmCloneVsocFileMsg msg = new VmCloneVsocFileMsg();
        msg.setDestVmUuid(spec.getVmInventory().getUuid());
        msg.setSrcVmUuid(spec.getSrcVmUuid());
        msg.setDestSocId(null);
        msg.setResource(VmInstanceConstant.NORESOURCE);
        msg.setType(VmInstanceConstant.OFFLINE);
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setPlatformId(CoreGlobalProperty.PLATFORM_ID);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        DeleteVmVsocFileMsg msgDelete = new DeleteVmVsocFileMsg();
        msgDelete.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msgDelete.setHostUuid(spec.getDestHost().getUuid());
        msgDelete.setPlatformId(PLATFORM_ID);
        bus.makeTargetServiceIdByResourceUuid(msgDelete, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msgDelete, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug("SUCESSS: call vsoc_delete");
                } else {
                    logger.error(String.format("FAIL: call vsoc_delete, because: %s", reply.getError().toString()));
                }
            }
        });
        chain.rollback();
    }
}
