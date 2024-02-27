
package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAfterInstantiateVolumeInAttachingVolumeFlow extends NoRollbackFlow {
    CLogger logger = Utils.getLogger(VmAfterInstantiateVolumeInAttachingVolumeFlow.class);

    @Autowired
    ErrorFacade errf;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    @Override
    public void run(FlowTrigger chain, Map ctx) {
        final VolumeInventory volume = (VolumeInventory) ctx.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        extEmitter.afterInstantiateVolume(spec.getVmInventory(), volume, new NoErrorCompletion() {
            @Override
            public void done() {
                chain.next();
            }
        });
    }
}
