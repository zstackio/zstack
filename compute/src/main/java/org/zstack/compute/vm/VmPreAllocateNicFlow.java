package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicSpec;
import org.zstack.header.vm.VmPreAttachL3NetworkExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmPreAllocateNicFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmPreAllocateNicFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicSpec> l3Networks = spec.getL3Networks();
        for (VmNicSpec l3Network : l3Networks) {
            for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                for (L3NetworkInventory l3Inv : l3Network.getL3Invs()) {
                    ext.vmPreAttachL3Network(spec.getVmInventory(), l3Inv);
                }
            }
        }
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
