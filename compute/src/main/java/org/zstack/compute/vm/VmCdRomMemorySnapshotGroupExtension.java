package org.zstack.compute.vm;

import edu.emory.mathcs.backport.java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.group.MemorySnapshotGroupExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.CreateVmCdRomMsg;
import org.zstack.header.vm.CreateVmCdRomReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.cdrom.DeleteVmCdRomMsg;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveVO;
import org.zstack.header.vm.devices.VmInstanceDeviceManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by LiangHanYu on 2022/6/20 11:41
 */
public class VmCdRomMemorySnapshotGroupExtension implements MemorySnapshotGroupExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmCdRomMemorySnapshotGroupExtension.class);

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
        List<VmInstanceDeviceAddressArchiveVO> needToRevertCdRomList = vidm.getAddressArchiveInfoFromArchiveForResourceUuid(snapshotGroup.getVmInstanceUuid(), snapshotGroup.getUuid(), VmCdRomInventory.class.getCanonicalName())
                .stream().sorted(Comparator.comparingInt(needToRevertCdRom -> JSONObjectUtil.toObject(needToRevertCdRom.getMetadata(), VmCdRomInventory.class).getDeviceId()))
                .collect(Collectors.toList());

        List<String> needToDeleteVmCdRomUuidListCurrently = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.uuid)
                .eq(VmCdRomVO_.vmInstanceUuid, snapshotGroup.getVmInstanceUuid())
                .listValues();

        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setName(String.format("revert-vm-%s-cdRom-info", snapshotGroup.getVmInstanceUuid()));
        fchain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-all-cdRoms";

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

                    private boolean isCdRomIsoEnabled(String isoUuid){
                        return Q.New(ImageVO.class).eq(ImageVO_.uuid,isoUuid)
                                .eq(ImageVO_.state, ImageState.Enabled)
                                .eq(ImageVO_.status, ImageStatus.Ready)
                                .isExists();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToRevertCdRomList).each((originalCdRom, whileCompletion) -> {
                            VmCdRomInventory cdRomInventory = JSONObjectUtil.toObject(originalCdRom.getMetadata(), VmCdRomInventory.class);
                            CreateVmCdRomMsg cmsg = new CreateVmCdRomMsg();
                            cmsg.setVmInstanceUuid(snapshotGroup.getVmInstanceUuid());
                            cmsg.setName(cdRomInventory.getName());
                            cmsg.setResourceUuid(cdRomInventory.getUuid());
                            if (isCdRomIsoEnabled(cdRomInventory.getIsoUuid())){
                                cmsg.setIsoUuid(cdRomInventory.getIsoUuid());
                            }
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

                                    vidm.revertRequestedDeviceAddressFromArchive(cdRomInventory.getVmInstanceUuid(),
                                            snapshotGroup.getUuid(),
                                            Collections.singletonList(cdRomInventory.getUuid()));
                                    whileCompletion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
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