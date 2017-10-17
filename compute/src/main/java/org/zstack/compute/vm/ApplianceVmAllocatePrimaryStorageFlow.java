package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.asyncbatch.AsyncLoop;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.WorkFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmAllocatePrimaryStorageFlow implements Flow {
    protected static final CLogger logger = Utils.getLogger(ApplianceVmAllocatePrimaryStorageFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final List<AllocatePrimaryStorageMsg> msgs = new ArrayList<>();
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory destHost = spec.getDestHost();
        final ImageInventory iminv = spec.getImageSpec().getInventory();

        // get ps types from bs
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        String bsType = q.findValue();
        List<String> primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);
        DebugUtils.Assert(primaryStorageTypes != null, "why primaryStorageTypes is null");

        DebugUtils.Assert(spec.getDataDiskOfferings().size() == 0, "create appliance vm can not with data volume");

        for (PrimaryStorageAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyExtensionPoint.class)) {
            String allocatorStrategyType = ext.getAllocatorStrategy();

            AllocatePrimaryStorageMsg rmsg = new AllocatePrimaryStorageMsg();
            rmsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
            rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
            rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
            rmsg.setSize(iminv.getSize());
            rmsg.setRequiredHostUuid(destHost.getUuid());
            rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());
            rmsg.setRequiredPrimaryStorageTypes(primaryStorageTypes);
            rmsg.setAllocationStrategy(allocatorStrategyType);

            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(rmsg);
        }

        new AsyncLoop<AllocatePrimaryStorageMsg>(trigger) {
            boolean isAllocateSuccess = false;

            @Override
            protected Collection<AllocatePrimaryStorageMsg> collectionForLoop() {
                return msgs;
            }

            @Override
            protected void run(AllocatePrimaryStorageMsg msg, Completion completion) {
                if(isAllocateSuccess){
                    completion.success();
                    return;
                }

                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {

                        if (!reply.isSuccess() && (msgs.indexOf(msg) + 1 < msgs.size())) {
                            // After failing, continue until all operations fail
                            logger.warn(String.format("Try to allocate failed, allocate primary storage msg: %s", JSONObjectUtil.toJsonString(msg)));
                            completion.success();
                            return;
                        }else if(!reply.isSuccess()){
                            completion.fail(reply.getError());
                            return;
                        }

                        isAllocateSuccess = true;

                        VolumeSpec volumeSpec = new VolumeSpec();
                        AllocatePrimaryStorageReply ar = reply.castReply();
                        volumeSpec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                        volumeSpec.setSize(ar.getSize());
                        volumeSpec.setRoot(msg.getImageUuid() != null);
                        spec.getVolumeSpecs().add(volumeSpec);

                        completion.success();
                    }
                });
            }

            @Override
            protected void done() {
                trigger.next();
            }

            @Override
            protected void error(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        }.start();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        for (VolumeSpec vspec : spec.getVolumeSpecs()) {
            if (vspec.isVolumeCreated()) {
                // don't return capacity as it has been returned when the volume is deleted
                continue;
            }

            IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
            msg.setDiskSize(vspec.getSize());
            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vspec.getPrimaryStorageInventory().getUuid());
            bus.send(msg);
        }

        chain.rollback();
    }
}
