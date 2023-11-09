package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.configuration.DiskOfferingState;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateOtherDiskFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmInstantiateOtherDiskFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;

    public void run(FlowTrigger trigger, Map data) {
        List<APICreateVmInstanceMsg.DiskAO> diskAOs = (List<APICreateVmInstanceMsg.DiskAO>) data.get(APICreateVmInstanceMsg.DiskAO.class.getSimpleName());
        VmInstanceInventory instantiateVm = (VmInstanceInventory) data.get(VmInstanceInventory.class.getSimpleName());
        String vmCreationStrategy = (String) data.get(VmCreationStrategy.class.getSimpleName());
        String vmUuid= instantiateVm.getUuid();
        String hostUuid = instantiateVm.getLastHostUuid();
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(vmUuid);
        boolean isInstantStart = Objects.equals(vmCreationStrategy, VmCreationStrategy.InstantStart.toString());

        setDiskOfferingByUuid(diskAOs);
        setVolumeInventoryByResourceUuid(diskAOs);
        List<Long> dataDiskSizes = diskAOs.stream().map(APICreateVmInstanceMsg.DiskAO::getSize).filter(size -> size > 0).distinct().collect(Collectors.toList());
        Map<Long, DiskOfferingVO> temporaryDiskOfferingBySize = createTemporaryDiskOfferingBySize(dataDiskSizes, vmUuid, accountUuid);
        setDiskOfferingBySize(diskAOs, temporaryDiskOfferingBySize);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("vm-%s-instantiate-other-disk",instantiateVm.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "instantiate-other-disk";

                    final List<String> attachedVolumeUuids = new ArrayList<>();

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {
                        List<ErrorCode> errorCodes = new ArrayList<>();
                        new While<>(diskAOs).each((diskAO, whileCompletion) -> {

                            FlowChain chain = FlowChainBuilder.newShareFlowChain();
                            chain.setName("attach-other-disk");
                            chain.then(new ShareFlow() {
                                VolumeInventory volumeInventory;
                                String allocatedInstallUrl;

                                @Override
                                public void setup() {
                                    if (diskAO.getDiskOfferingUuid() != null) {
                                        setupVolumeFromDiskOfferingUuidFlows();
                                    }

                                    if (diskAO.getTemplateUuid() != null) {
                                        setupVolumeFromTemplateUuidFlows();
                                    }

                                    if (diskAO.getSourceUuid() != null) {
                                        volumeInventory = diskAO.getVolumeInventory();
                                    }

                                    setupAttachVolumeFlows();

                                    done(new FlowDoneHandler(whileCompletion) {
                                        @Override
                                        public void handle(Map data) {
                                            collectCreatedVolume();
                                            whileCompletion.done();
                                        }
                                    });

                                    error(new FlowErrorHandler(whileCompletion) {
                                        @Override
                                        public void handle(ErrorCode errCode, Map data) {
                                            errorCodes.add(errCode);
                                            whileCompletion.allDone();
                                        }
                                    });
                                }

                                private void collectCreatedVolume() {
                                    if (diskAO.getSourceUuid() == null) {
                                        attachedVolumeUuids.add(volumeInventory.getUuid());
                                    }
                                }

                                private void setupVolumeFromDiskOfferingUuidFlows() {
                                    flow(new Flow() {
                                        String __name__ = "allocate-primaryStorage";

                                        boolean isSuccessAllocatePS = false;

                                        @Override
                                        public void run(final FlowTrigger whileTrigger, Map data) {
                                            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                                            amsg.setSize(diskAO.getSize());
                                            amsg.setVmInstanceUuid(vmUuid);
                                            amsg.setRequiredHostUuid(hostUuid);
                                            amsg.setDiskOfferingUuid(diskAO.getDiskOfferingUuid());
                                            amsg.setRequiredPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                                            amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
                                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                                            bus.send(amsg, new CloudBusCallBack(whileTrigger) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (!reply.isSuccess()) {
                                                        whileTrigger.fail(reply.getError());
                                                        return;
                                                    }
                                                    AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                                    allocatedInstallUrl = ar.getAllocatedInstallUrl();
                                                    isSuccessAllocatePS = true;
                                                    whileTrigger.next();
                                                }
                                            });
                                        }

                                        @Override
                                        public void rollback(FlowRollback chain, Map data) {
                                            if (!isSuccessAllocatePS) {
                                                chain.rollback();
                                                return;
                                            }
                                            ReleasePrimaryStorageSpaceMsg msg = new ReleasePrimaryStorageSpaceMsg();
                                            msg.setDiskSize(diskAO.getSize());
                                            msg.setAllocatedInstallUrl(allocatedInstallUrl);
                                            msg.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, diskAO.getPrimaryStorageUuid());
                                            bus.send(msg);
                                            chain.rollback();
                                        }
                                    });

                                    flow(new Flow() {
                                        String __name__ = "create-data-volume";

                                        @Override
                                        public void run(final FlowTrigger whileTrigger, Map data) {
                                            CreateVolumeMsg msg = new CreateVolumeMsg();
                                            msg.setAccountUuid(accountUuid);
                                            msg.setVmInstanceUuid(vmUuid);
                                            msg.setSize(diskAO.getSize());
                                            msg.setSystemTags(diskAO.getSystemTags());
                                            msg.setVolumeType(VolumeType.Data.toString());
                                            msg.setDiskOfferingUuid(diskAO.getDiskOfferingUuid());
                                            msg.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                                            msg.setDescription(String.format("vm-%s-data-volume", vmUuid));
                                            msg.setName(String.format("DATA-for-%s", instantiateVm.getName()));
                                            msg.setFormat(VolumeFormat.getVolumeFormatByMasterHypervisorType(instantiateVm.getHypervisorType()).toString());
                                            bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
                                            bus.send(msg, new CloudBusCallBack(whileTrigger) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (!reply.isSuccess()) {
                                                        whileTrigger.fail(reply.getError());
                                                        return;
                                                    }
                                                    CreateVolumeReply cr = reply.castReply();
                                                    volumeInventory = cr.getInventory();
                                                    whileTrigger.next();
                                                }
                                            });
                                        }

                                        @Override
                                        public void rollback(FlowRollback chain, Map data) {
                                            if (volumeInventory == null) {
                                                chain.rollback();
                                                return;
                                            }
                                            DeleteVolumeMsg msg = new DeleteVolumeMsg();
                                            msg.setDetachBeforeDeleting(false);
                                            msg.setUuid(volumeInventory.getUuid());
                                            msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                                            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeInventory.getUuid());
                                            bus.send(msg, new CloudBusCallBack(chain) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (reply.isSuccess()) {
                                                        chain.rollback();
                                                        return;
                                                    }

                                                    DeleteVolumeGC gc = new DeleteVolumeGC();
                                                    gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                                                    gc.deletionPolicy = VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString();
                                                    gc.volumeUuid = msg.getVolumeUuid();
                                                    gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);

                                                    chain.rollback();
                                                }
                                            });
                                        }
                                    });

                                    flow(new NoRollbackFlow() {
                                        String __name__ = "instantiate-data-volume";

                                        @Override
                                        public void run(final FlowTrigger whileTrigger, Map data) {
                                            InstantiateVolumeMsg imsg = new InstantiateVolumeMsg();
                                            imsg.setHostUuid(hostUuid);
                                            imsg.setSkipIfExisting(true);
                                            imsg.setPrimaryStorageAllocated(true);
                                            imsg.setVolumeUuid(volumeInventory.getUuid());
                                            imsg.setAllocatedInstallUrl(allocatedInstallUrl);
                                            imsg.setPrimaryStorageUuid(volumeInventory.getPrimaryStorageUuid());
                                            bus.makeLocalServiceId(imsg, VolumeConstant.SERVICE_ID);
                                            bus.send(imsg, new CloudBusCallBack(whileTrigger) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (!reply.isSuccess()) {
                                                        whileTrigger.fail(reply.getError());
                                                    }
                                                    whileTrigger.next();
                                                }
                                            });
                                        }
                                    });
                                }

                                private void setupVolumeFromTemplateUuidFlows() {
                                    flow(new Flow() {
                                        String __name__ = String.format("instantiate-data-volume-from-template-%s", diskAO.getTemplateUuid());

                                        @Override
                                        public void run(final FlowTrigger whileTrigger, Map data) {
                                            CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
                                            cmsg.setHostUuid(instantiateVm.getLastHostUuid());
                                            cmsg.setImageUuid(diskAO.getTemplateUuid());
                                            cmsg.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                                            Tuple t = Q.New(ImageVO.class).eq(ImageVO_.uuid, diskAO.getTemplateUuid())
                                                    .select(ImageVO_.name, ImageVO_.description).findTuple();
                                            cmsg.setName("data-volume-" + t.get(0, String.class));
                                            cmsg.setDescription(t.get(1, String.class));
                                            cmsg.setAccountUuid(accountUuid);
                                            cmsg.setSystemTags(diskAO.getSystemTags());
                                            bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                                            bus.send(cmsg, new CloudBusCallBack(whileTrigger) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (!reply.isSuccess()) {
                                                        whileTrigger.fail(reply.getError());
                                                        return;
                                                    }
                                                    VolumeInventory inv = ((CreateDataVolumeFromVolumeTemplateReply) reply).getInventory();
                                                    String volumeUuid = inv.getUuid();
                                                    VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
                                                    vo.setVmInstanceUuid(vmUuid);
                                                    vo.setActualSize(vo.getActualSize() == null ? 0L : vo.getActualSize());
                                                    vo = dbf.updateAndRefresh(vo);
                                                    volumeInventory = VolumeInventory.valueOf(vo);
                                                    attachedVolumeUuids.add(volumeInventory.getUuid());
                                                    whileTrigger.next();
                                                }
                                            });
                                        }

                                        @Override
                                        public void rollback(FlowRollback chain, Map data) {
                                            if (volumeInventory == null) {
                                                chain.rollback();
                                                return;
                                            }
                                            DeleteVolumeMsg msg = new DeleteVolumeMsg();
                                            msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                                            msg.setUuid(volumeInventory.getUuid());
                                            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeInventory.getUuid());
                                            bus.send(msg, new CloudBusCallBack(chain) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (reply.isSuccess()) {
                                                        chain.rollback();
                                                        return;
                                                    }

                                                    DeleteVolumeGC gc = new DeleteVolumeGC();
                                                    gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                                                    gc.deletionPolicy = VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString();
                                                    gc.volumeUuid = msg.getVolumeUuid();
                                                    gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);

                                                    chain.rollback();
                                                }
                                            });
                                        }
                                    });
                                }

                                private void setupAttachVolumeFlows() {
                                    flow(new NoRollbackFlow() {
                                        String __name__ = String.format("attach-volume-to-vm-%s", vmUuid);

                                        @Override
                                        public void run(final FlowTrigger whileTrigger, Map data) {
                                            AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                                            amsg.setVmInstanceUuid(vmUuid);
                                            amsg.setVolume(volumeInventory);
                                            bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                                            bus.send(amsg, new CloudBusCallBack(whileTrigger) {
                                                @Override
                                                public void run(MessageReply reply) {
                                                    if (!reply.isSuccess()) {
                                                        whileTrigger.fail(reply.getError());
                                                        return;
                                                    }
                                                    whileTrigger.next();
                                                }
                                            });
                                        }
                                    });
                                }
                            }).start();

                        }).run(new WhileDoneCompletion(innerTrigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                List<String> temporaryDiskOfferingUuids = temporaryDiskOfferingBySize.values().stream()
                                        .map(DiskOfferingVO::getUuid).collect(Collectors.toList());
                                if (!temporaryDiskOfferingUuids.isEmpty()) {
                                    dbf.removeByPrimaryKeys(temporaryDiskOfferingUuids, DiskOfferingVO.class);
                                }

                                if (!errorCodes.isEmpty()) {
                                    innerTrigger.fail(errorCodes.get(0));
                                    return;
                                }

                                innerTrigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        new While<>(attachedVolumeUuids).each((uuid, whileCompletion) -> {
                            DeleteVolumeMsg msg = new DeleteVolumeMsg();
                            msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
                            msg.setUuid(uuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, uuid);
                            bus.send(msg, new CloudBusCallBack(msg) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        whileCompletion.done();
                                        return;
                                    }
                                    DeleteVolumeGC gc = new DeleteVolumeGC();
                                    gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                                    gc.deletionPolicy = VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString();
                                    gc.volumeUuid = msg.getVolumeUuid();
                                    gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);
                                    whileCompletion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(null) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                chain.rollback();
                            }
                        });
                    }
                });

                if (isInstantStart) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "start-vm";

                        @Override
                        public void run(FlowTrigger innerTrigger, Map data) {
                            StartVmInstanceMsg smsg = new StartVmInstanceMsg();
                            smsg.setVmInstanceUuid(instantiateVm.getUuid());
                            smsg.setHostUuid(instantiateVm.getHostUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, instantiateVm.getUuid());
                            bus.send(smsg, new CloudBusCallBack(innerTrigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        innerTrigger.fail(reply.getError());
                                        return;
                                    }
                                    innerTrigger.next();
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void setVolumeInventoryByResourceUuid(List<APICreateVmInstanceMsg.DiskAO> diskAOs) {
        List<String> volumeUuids = diskAOs.stream()
                .filter(diskAO -> Objects.equals(diskAO.getSourceType(), VolumeVO.class.getSimpleName()))
                .map(APICreateVmInstanceMsg.DiskAO::getSourceUuid).collect(Collectors.toList());
        List<VolumeVO> volumes = Q.New(VolumeVO.class).in(VolumeVO_.uuid, volumeUuids).list();
        volumes.forEach(volumeVO -> diskAOs.stream().filter(diskAO -> Objects.equals(diskAO.getSourceUuid(), volumeVO.getUuid()))
                .forEach(diskAO -> diskAO.setVolumeInventory(VolumeInventory.valueOf(volumeVO))));
    }

    private void setDiskOfferingByUuid(List<APICreateVmInstanceMsg.DiskAO> diskAOs) {
        List<String> diskOfferingUuid = diskAOs.stream().map(APICreateVmInstanceMsg.DiskAO::getDiskOfferingUuid)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<String, DiskOfferingVO> diskOfferingByUuid = getDiskOfferingByUuid(diskOfferingUuid);
        diskAOs.stream().filter(diskAO -> diskAO.getDiskOfferingUuid() != null)
                .forEach(diskAO -> diskAO.setDiskOffering(diskOfferingByUuid.get(diskAO.getDiskOfferingUuid())));
    }

    private void setDiskOfferingBySize(List<APICreateVmInstanceMsg.DiskAO> diskAOs, Map<Long, DiskOfferingVO> temporaryDiskOfferingUuidsBySize) {
        diskAOs.stream().filter(diskAO -> diskAO.getSize() > 0)
                .forEach(diskAO -> {
                    diskAO.setDiskOfferingUuid(temporaryDiskOfferingUuidsBySize.get(diskAO.getSize()).getUuid());
                    diskAO.setDiskOffering(temporaryDiskOfferingUuidsBySize.get(diskAO.getSize()));
                });
    }

    private Map<Long, DiskOfferingVO> createTemporaryDiskOfferingBySize(List<Long> sizes, String vmUuid, String accountUuid) {
        if (CollectionUtils.isEmpty(sizes)) {
            return new HashMap<>();
        }

        List<DiskOfferingVO> diskOfferingVos = new ArrayList<>();
        Map<Long, DiskOfferingVO> diskOfferingBySize = new HashMap<>();
        for (Long size : sizes) {
            DiskOfferingVO dvo = new DiskOfferingVO();
            dvo.setUuid(Platform.getUuid());
            dvo.setAccountUuid(accountUuid);
            dvo.setDiskSize(size);
            dvo.setName(String.format("create-data-volume-for-vm-%s", vmUuid));
            dvo.setType("TemporaryDiskOfferingType");
            dvo.setState(DiskOfferingState.Enabled);
            diskOfferingVos.add(dvo);
            diskOfferingBySize.put(size, dvo);
        }
        dbf.persistCollection(diskOfferingVos);
        return diskOfferingBySize;
    }

    private Map<String, DiskOfferingVO> getDiskOfferingByUuid(List<String> diskOfferingUuids) {
        List<DiskOfferingVO> diskOfferingVOs = Q.New(DiskOfferingVO.class).in(DiskOfferingVO_.uuid, diskOfferingUuids).list();
        Map<String, DiskOfferingVO> diskOfferingByUuid = new HashMap<>();
        if (diskOfferingVOs.isEmpty()) {
            return diskOfferingByUuid;
        }

        diskOfferingVOs.forEach(diskOfferingVO -> diskOfferingByUuid.put(diskOfferingVO.getUuid(), diskOfferingVO));
        return diskOfferingByUuid;
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}

