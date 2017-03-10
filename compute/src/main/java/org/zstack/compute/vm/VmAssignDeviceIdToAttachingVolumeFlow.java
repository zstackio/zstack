
package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAssignDeviceIdToAttachingVolumeFlow implements Flow {
    CLogger logger = Utils.getLogger(VmAssignDeviceIdToAttachingVolumeFlow.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    ErrorFacade errf;

    private int getNextVolumeDeviceId(String vmUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.deviceId);
        q.add(VolumeVO_.vmInstanceUuid, Op.EQ, vmUuid);
        q.add(VolumeVO_.deviceId, Op.NOT_NULL);
        q.orderBy(VolumeVO_.deviceId, Od.ASC);
        List<Integer> devIds = q.listValue();

        BitSet full = new BitSet(devIds.size() + 1);
        devIds.forEach(full::set);
        return full.nextClearBit(0);
    }

    @Override
    public void run(FlowTrigger chain, Map ctx) {
        final VolumeInventory volume = (VolumeInventory) ctx.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        VolumeVO dvol = dbf.findByUuid(volume.getUuid(), VolumeVO.class);
        List<GetNextVolumeDeviceIdExtensionPoint> exts = pluginRegistry.getExtensionList(
                GetNextVolumeDeviceIdExtensionPoint.class);
        if (exts == null || exts.isEmpty()) {
            dvol.setDeviceId(getNextVolumeDeviceId(spec.getVmInventory().getUuid()));
        } else if (exts.size() == 1) {
            dvol.setDeviceId(exts.get(0).getNextVolumeDeviceId(spec.getVmInventory().getUuid()));
        } else {
            throw new OperationFailureException(err(SysErrors.INTERNAL,
                    "should not be more than one GetNextVolumeDeviceIdExtensionPoint implementation"));
        }
        dvol = dbf.updateAndRefresh(dvol);
        ctx.put(VmInstanceConstant.Params.AttachingVolumeInventory.toString(), VolumeInventory.valueOf(dvol));
        chain.next();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        final VolumeInventory volume = (VolumeInventory) data.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        VolumeVO dvol = dbf.findByUuid(volume.getUuid(), VolumeVO.class);
        dvol.setDeviceId(null);
        dbf.update(dvol);
        chain.rollback();
    }
}
