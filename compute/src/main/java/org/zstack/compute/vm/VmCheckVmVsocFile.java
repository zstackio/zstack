package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmVsocBootFromNewNodeMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCheckVmVsocFile implements Flow {
    private static CLogger logger = Utils.getLogger(VmCheckVmVsocFile.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (spec.getDestHost().getUuid().equals(spec.getVmInventory().getLastHostUuid())) {
            chain.next();
        } else {
            VmVsocBootFromNewNodeMsg msg = new VmVsocBootFromNewNodeMsg();
            msg.setHostUuid(spec.getDestHost().getUuid());
            msg.setPlatformId(CoreGlobalProperty.PLATFORM_ID);
            msg.setVmUuid(spec.getVmInventory().getUuid());
            msg.setPrvSocId(HostSystemTags.HOST_SSCARDID.getTokenByResourceUuid(spec.getVmInventory().getLastHostUuid(), HostSystemTags.HOST_SSCARDID_TOKEN));
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
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
