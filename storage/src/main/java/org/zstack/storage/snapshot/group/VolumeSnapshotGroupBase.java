package org.zstack.storage.snapshot.group;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.vm.RestoreVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveVO;
import org.zstack.header.vm.devices.VmInstanceDeviceManager;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.snapshot.VolumeSnapshotMessageRouter.getResourceIdToRouteMsg;

/**
 * Created by MaJin on 2019/7/9.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotGroupBase implements VolumeSnapshotGroup {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotGroupBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceDeviceManager vidm;

    public VolumeSnapshotGroupBase(VolumeSnapshotGroupVO self) {
        this.self = self;
        this.id = "volumeSnapshotGroup-" + self.getUuid();
    }

    protected VolumeSnapshotGroupVO self;

    protected final String id;

    protected VolumeSnapshotGroupInventory getSelfInventory() {
        return VolumeSnapshotGroupInventory.valueOf(self);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteVolumeSnapshotGroupInnerMsg) {
            handle((DeleteVolumeSnapshotGroupInnerMsg) msg);
        } else if (msg instanceof RevertVmFromSnapshotGroupInnerMsg) {
            handle((RevertVmFromSnapshotGroupInnerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUngroupVolumeSnapshotGroupMsg) {
            handle((APIUngroupVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotGroupMsg) {
            handle((APIDeleteVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIRevertVmFromSnapshotGroupMsg) {
            handle((APIRevertVmFromSnapshotGroupMsg) msg);
        } else if (msg instanceof APIUpdateVolumeSnapshotGroupMsg) {
            handle((APIUpdateVolumeSnapshotGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVolumeSnapshotGroupMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }

        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }

        if (update) {
            dbf.updateAndRefresh(self);
        }

        APIUpdateVolumeSnapshotGroupEvent event = new APIUpdateVolumeSnapshotGroupEvent(msg.getId());
        event.setInventory(getSelfInventory());
        bus.publish(event);
    }

    private void handle(APIUngroupVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUngroupVolumeSnapshotGroupEvent evt = new APIUngroupVolumeSnapshotGroupEvent(msg.getId());
                dbf.remove(self);
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "ungroup-snapshot-group";
            }
        });
    }

    private void handle(APIDeleteVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleDelete(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-snapshot-group";
            }
        });

    }

    private void handleDelete(APIDeleteVolumeSnapshotGroupMsg msg, NoErrorCompletion completion) {
        APIDeleteVolumeSnapshotGroupEvent event = new APIDeleteVolumeSnapshotGroupEvent(msg.getId());
        DeleteVolumeSnapshotGroupInnerMsg imsg = new DeleteVolumeSnapshotGroupInnerMsg();
        imsg.setUuid(msg.getUuid());
        imsg.setDeletionMode(msg.getDeletionMode());
        bus.makeTargetServiceIdByResourceUuid(imsg, VolumeSnapshotConstant.SERVICE_ID, msg.getUuid());
        overlaySend(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }

                if (reply instanceof DeleteVolumeSnapshotGroupInnerReply) {
                    event.setResults(((DeleteVolumeSnapshotGroupInnerReply) reply).getResults());
                }

                bus.publish(event);
                completion.done();
            }
        });
    }

    private void handle(DeleteVolumeSnapshotGroupInnerMsg msg) {
        DeleteVolumeSnapshotGroupInnerReply reply = new DeleteVolumeSnapshotGroupInnerReply();
        List<VolumeSnapshotVO> snapshots = getEffectiveSnapshots();
        if (snapshots.size() < self.getSnapshotCount()) {
            logger.debug(String.format("skip snapshots not belong to origin vm[uuid:%s]", self.getVmInstanceUuid()));
        }

        new While<>(snapshots).all((snapshot, compl) -> {
            DeleteVolumeSnapshotMsg rmsg = new DeleteVolumeSnapshotMsg();
            rmsg.setSnapshotUuid(snapshot.getUuid());
            rmsg.setVolumeUuid(snapshot.getVolumeUuid());
            rmsg.setTreeUuid(snapshot.getTreeUuid());
            rmsg.setDeletionMode(msg.getDeletionMode());

            bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeSnapshotConstant.SERVICE_ID, getResourceIdToRouteMsg(snapshot));
            bus.send(rmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply r) {
                    reply.addResult(new DeleteSnapshotGroupResult(rmsg.getSnapshotUuid(), rmsg.getVolumeUuid(), r.getError()));
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIRevertVmFromSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleRevert(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "revert-snapshot-group";
            }
        });
    }

    private void
    handleRevert(APIRevertVmFromSnapshotGroupMsg msg, NoErrorCompletion completion) {
        APIRevertVmFromSnapshotGroupEvent event = new APIRevertVmFromSnapshotGroupEvent(msg.getId());

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("revert-vm-%s-from-snapshot-group-%s", self.getVmInstanceUuid(), msg.getGroupUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "revert-vm-devices-info-before-restore-VmInstance";

            @Override
            public boolean skip(Map data) {
                return self.getVolumeSnapshotRefs().stream().filter(sp -> sp.getVolumeType().equals(VolumeType.Memory.toString())).collect(Collectors.toList()).isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(MemorySnapshotGroupExtensionPoint.class)).each((ext, compl) -> {
                    List<VmInstanceDeviceAddressArchiveVO> archiveVmInfos = vidm.getAddressArchiveInfoFromArchiveForResourceUuid(getSelfInventory().getVmInstanceUuid(), getSelfInventory().getUuid(), ext.getArchiveBundleCanonicalName());
                    List<Object> bundles = archiveVmInfos.stream().map(archiveVmInfo -> (Object) JSONObjectUtil.toObject(archiveVmInfo.getMetadata(), ext.getArchiveBundleClass())).collect(Collectors.toList());
                    if (bundles.isEmpty()) {
                        compl.addError(operr("no bundle found for type:%s", ext.getArchiveBundleCanonicalName()));
                        compl.allDone();
                        return;
                    }
                    ext.beforeRevertMemorySnapshotGroup(getSelfInventory(), bundles, new Completion(compl) {
                        @Override
                        public void success() {
                            compl.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            compl.addError(errorCode);
                            compl.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(errorCodeList.getCauses().get(0));
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "revert-volume-snapshots";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                RevertVmFromSnapshotGroupInnerMsg imsg = new RevertVmFromSnapshotGroupInnerMsg();
                imsg.setUuid(msg.getUuid());
                imsg.setSession(msg.getSession());
                bus.makeTargetServiceIdByResourceUuid(imsg, VolumeSnapshotConstant.SERVICE_ID, msg.getUuid());
                overlaySend(imsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        if (reply instanceof RevertVmFromSnapshotGroupInnerReply) {
                            event.setResults(((RevertVmFromSnapshotGroupInnerReply) reply).getResults());
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, self.getVmInstanceUuid()).eq(VolumeVO_.type, VolumeType.Memory).find();
                if (volume == null) {
                    trigger.next();
                    return;
                }

                Optional opt = self.getVolumeSnapshotRefs().stream().filter(sp -> sp.getVolumeUuid().equals(volume.getUuid())).findFirst();
                if (!opt.isPresent()) {
                    trigger.next();
                    return;
                }

                VolumeSnapshotGroupRefVO ref = (VolumeSnapshotGroupRefVO) opt.get();

                RestoreVmInstanceMsg rmsg = new RestoreVmInstanceMsg();
                rmsg.setVmInstanceUuid(self.getVmInstanceUuid());
                rmsg.setMemorySnapshotUuid(ref.getVolumeSnapshotUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, rmsg.getVmInstanceUuid());
                bus.send(rmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        trigger.next();
                    }
                });

            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
                completion.done();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                bus.publish(event);
                completion.done();
            }
        }).start();
    }

    private void handle(RevertVmFromSnapshotGroupInnerMsg msg) {
        RevertVmFromSnapshotGroupInnerReply reply = new RevertVmFromSnapshotGroupInnerReply();
        List<VolumeSnapshotVO> snapshots = getEffectiveSnapshots();
        String vmUuid = self.getVmInstanceUuid();

        VolumeSnapshotGroupAvailability availability = VolumeSnapshotGroupChecker.getAvailability(self);
        if (!availability.isAvailable()) {
            reply.setError(operr(availability.getReason()));
            bus.reply(msg, reply);
            return;
        }

        VolumeSnapshotGroupVO newGroup = null;
        if (VolumeSnapshotGlobalConfig.SNAPSHOT_BEFORE_REVERTVOLUME.value(Boolean.class)) {
            newGroup = new VolumeSnapshotGroupVO();
            newGroup.setUuid(Platform.getUuid());
            newGroup.setName(String.format("revert-vm-point-%s-%s", vmUuid, TimeUtils.getCurrentTimeStamp("yyyyMMddHHmmss")));
            newGroup.setDescription(String.format("save snapshot for revert vm [uuid:%s]", vmUuid));
            newGroup.setSnapshotCount(snapshots.size());
            newGroup.setVmInstanceUuid(vmUuid);
            newGroup.setAccountUuid(msg.getSession().getAccountUuid());
            dbf.persist(newGroup);
        }

        final String finalNewGroupUuid = newGroup == null ? null : newGroup.getUuid();
        new While<>(snapshots).each((snapshot, compl) -> {
            if (Q.New(VolumeVO.class).eq(VolumeVO_.uuid, snapshot.getVolumeUuid()).eq(VolumeVO_.type, VolumeType.Memory).isExists()) {
                compl.done();
                return;
            }

            RevertVolumeFromSnapshotGroupMsg rmsg = new RevertVolumeFromSnapshotGroupMsg();
            rmsg.setSnapshotUuid(snapshot.getUuid());
            rmsg.setVolumeUuid(snapshot.getVolumeUuid());
            rmsg.setTreeUuid(snapshot.getTreeUuid());
            rmsg.setSession(msg.getSession());
            rmsg.setNewSnapshotGroupUuid(finalNewGroupUuid);

            bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeSnapshotConstant.SERVICE_ID, getResourceIdToRouteMsg(snapshot));
            bus.send(rmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply r) {
                    reply.addResult(new RevertSnapshotGroupResult(rmsg.getSnapshotUuid(), rmsg.getVolumeUuid(), r.getError()));
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    public List<VolumeSnapshotVO> getSnapshots() {
        return SQL.New("select snap from VolumeSnapshotGroupRefVO ref, VolumeSnapshotVO snap" +
                " where ref.volumeSnapshotGroupUuid = :groupUuid" +
                " and snap.uuid = ref.volumeSnapshotUuid", VolumeSnapshotVO.class)
                .param("groupUuid", self.getUuid())
                .list();
    }

    @Transactional(readOnly = true)
    public List<VolumeSnapshotVO> getEffectiveSnapshots() {
        List<VolumeSnapshotVO> snapshots = getSnapshots();
        Set<String> attachedVolUuids = new HashSet<>(Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, self.getVmInstanceUuid())
                .select(VolumeVO_.uuid).listValues());
        snapshots.removeIf(it -> !attachedVolUuids.contains(it.getVolumeUuid()));
        return snapshots;
    }

    private void overlaySend(VolumeSnapshotGroupMessage imsg, CloudBusCallBack callBack) {
        VolumeSnapshotGroupOverlayMsg omsg = new VolumeSnapshotGroupOverlayMsg();
        omsg.setVmInstanceUuid(self.getVmInstanceUuid());
        omsg.setMessage((NeedReplyMessage) imsg);
        bus.makeTargetServiceIdByResourceUuid(omsg, VmInstanceConstant.SERVICE_ID, self.getVmInstanceUuid());
        bus.send(omsg, callBack);
    }
}
