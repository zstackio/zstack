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
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmBootFromNewNodeMsg;
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

        if (spec.getRequiredHostUuid() == null || spec.getRequiredHostUuid().equals(spec.getVmInventory().getLastHostUuid())) {
            chain.next();
        } else {
            VmBootFromNewNodeMsg msg = new VmBootFromNewNodeMsg();
            msg.setUuid(spec.getRequiredHostUuid());
            msg.setPlatformId(CoreGlobalProperty.PLATFORM_ID);
            msg.setVmUuid(spec.getVmInventory().getUuid());
            msg.setPrvSocId(HostSystemTags.HOST_SSCARDID.getTag(spec.getVmInventory().getLastHostUuid(), HostVO.class));
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
