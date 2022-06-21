package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.group.MemorySnapshotGroupExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.CreateVmCdRomMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.cdrom.DeleteVmCdRomMsg;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.vm.devices.*;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by LiangHanYu on 2022/6/20 11:41
 */
public class VmCdRomMemorySnapshotGroupExtension implements MemorySnapshotGroupExtensionPoint {
    @Autowired
    private CloudBus bus;

    @Autowired
    private VmInstanceDeviceManager vidm;

    @Override
    public void afterCreateMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion) {
        List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class).eq(VmCdRomVO_.vmInstanceUuid, snapshotGroup.getVmInstanceUuid()).list();

        for (VmCdRomVO cdRomVO : cdRomVOS) {
            vidm.createOrUpdateVmDeviceAddress(cdRomVO.getUuid(),
                    null,
                    cdRomVO.getVmInstanceUuid(),
                    JSONObjectUtil.toJsonString(cdRomVO),
                    VmCdRomInventory.class.getCanonicalName());
        }
        completion.success();
    }

    @Override
    public void beforeRevertMemorySnapshotGroup(VolumeSnapshotGroupInventory snapshotGroup, Completion completion) {
        List<VmInstanceDeviceAddressArchiveVO> needToRevertCdRomList = vidm.getAddressArchiveInfoFromArchiveForResourceUuid(snapshotGroup.getVmInstanceUuid(), snapshotGroup.getUuid(), VmCdRomInventory.class.getCanonicalName());
        List<String> needToDeleteVmCdRomUuidListCurrently = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.uuid)
                .eq(VmCdRomVO_.vmInstanceUuid, snapshotGroup.getVmInstanceUuid())
                .listValues();
        List<VmInstanceDeviceAddressArchiveVO> intersection = needToRevertCdRomList.stream().filter(originalCdRom -> needToDeleteVmCdRomUuidListCurrently.contains(originalCdRom.getResourceUuid())).collect(Collectors.toList());
        needToDeleteVmCdRomUuidListCurrently.removeAll(intersection.stream().map(VmInstanceDeviceAddressArchiveVO::getResourceUuid).collect(Collectors.toList()));
        needToRevertCdRomList.removeAll(intersection);

        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setName(String.format("revert-vm-%s-cdRom-info", snapshotGroup.getVmInstanceUuid()));
        fchain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-extra-cdRoms";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToDeleteVmCdRomUuidListCurrently).step((cdRomUuid, whileCompletion) -> {
                            DeleteVmCdRomMsg dmsg = new DeleteVmCdRomMsg();
                            dmsg.setVmInstanceUuid(snapshotGroup.getVmInstanceUuid());
                            dmsg.setCdRomUuid(cdRomUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, dmsg.getVmInstanceUuid());
                            bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-cdRoms-saved-by-memory-snapshot-group";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToRevertCdRomList).step((originalCdRom, whileCompletion) -> {
                            VmCdRomInventory cdRomInventory = JSONObjectUtil.toObject(originalCdRom.getMetadata(), VmCdRomInventory.class);
                            CreateVmCdRomMsg cmsg = new CreateVmCdRomMsg();
                            cmsg.setVmInstanceUuid(snapshotGroup.getVmInstanceUuid());
                            cmsg.setName(cdRomInventory.getName());
                            cmsg.setResourceUuid(cdRomInventory.getUuid());
                            cmsg.setIsoUuid(cdRomInventory.getIsoUuid());
                            cmsg.setDescription(cdRomInventory.getDescription());
                            bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
                            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }
}