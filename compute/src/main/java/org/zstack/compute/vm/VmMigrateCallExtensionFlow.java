package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.ForEachFunction;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmMigrateCallExtensionFlow implements Flow {
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        final HostInventory destHost = spec.getDestHost();
        for (VmInstanceMigrateExtensionPoint ext : pluginRgty.getExtensionList(VmInstanceMigrateExtensionPoint.class)) {
            ext.preMigrateVm(spec.getVmInventory(), destHost.getUuid());
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmInstanceMigrateExtensionPoint.class), new ForEachFunction<VmInstanceMigrateExtensionPoint>() {
            @Override
            public void run(VmInstanceMigrateExtensionPoint ext) {
                ext.beforeMigrateVm(spec.getVmInventory(), destHost.getUuid());
            }
        });

        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
