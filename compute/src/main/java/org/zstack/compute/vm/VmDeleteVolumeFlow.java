package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDeleteVolumeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmDeleteVolumeFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private CascadeFacade casf;


    /* root volume is detached as well, as it is going to be deleted*/
    private void detachAllVolumes(VmInstanceSpec spec) {
        List<String> volumeUuids = CollectionUtils.transformToList(spec.getVmInventory().getAllVolumes(), new Function<String, VolumeInventory>() {
            @Override
            public String call(VolumeInventory arg) {
                return arg.getUuid();
            }
        });
        SimpleQuery<VolumeVO> query = dbf.createQuery(VolumeVO.class);
        query.add(VolumeVO_.uuid, Op.IN, volumeUuids);
        /* if we failed in half way, the data volume may have been detached from the original vm and attached to another one,
         * we check vm uuid here to avoid detaching a data disk from a healthy vm
         */
        query.add(VolumeVO_.vmInstanceUuid, Op.EQ, spec.getVmInventory().getUuid());
        List<VolumeVO> vos = query.list();
        for (VolumeVO vo : vos) {
            vo.setVmInstanceUuid(null);
            dbf.update(vo);
        }
    }

    private List<VolumeInventory> getVolumesToExpunge(VmInstanceSpec spec, boolean expungeDataDisk) {
        List<VolumeInventory> volumesToExpunge = new ArrayList<VolumeInventory>(spec.getVmInventory().getAllVolumes().size());
        if (expungeDataDisk) {
            volumesToExpunge.addAll(spec.getVmInventory().getAllVolumes());
        } else {
            for (VolumeInventory v : spec.getVmInventory().getAllVolumes()) {
                if (v.getUuid().equals(spec.getVmInventory().getRootVolumeUuid())) {
                    volumesToExpunge.add(v);
                    break;
                }
            }
        }
        return volumesToExpunge;
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        boolean expungeDataDisk = VmGlobalConfig.DELETE_DATA_VOLUME_ON_VM_DESTROY.value(Boolean.class);

        /* data volume must be detached anyway no matter if it is going to be deleted */
        if (!spec.getVmInventory().getAllVolumes().isEmpty()) {
            detachAllVolumes(spec);
        }

        List<VolumeInventory> ctx = getVolumesToExpunge(spec, expungeDataDisk);
        final String issuer = VolumeVO.class.getSimpleName();

        casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }
}
