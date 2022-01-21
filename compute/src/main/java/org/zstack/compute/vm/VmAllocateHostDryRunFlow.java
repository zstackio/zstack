package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.allocator.*;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.CollectionUtils;
import java.util.Collections;
import java.util.Map;

import static org.zstack.header.host.HostConstant.SORTED_CLUSTERS;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostDryRunFlow extends VmAllocateHostFlow {
    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        AllocateHostMsg amsg = prepareMsg(spec);
        amsg.setListAllHosts(true);
        amsg.setDryRun(true);
        bus.send(amsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                AllocateHostDryRunReply r = reply.castReply();
                if (r.getHosts().size() == 0) {
                    data.put(SORTED_CLUSTERS, Collections.emptyList());
                    trigger.next();
                    return;
                }

                data.put(SORTED_CLUSTERS, CollectionUtils.transformToList(r.getHosts(), HostInventory::getClusterUuid));
                trigger.next();
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
