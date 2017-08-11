package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.VolumeDeletionStruct;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class VolumeSnapshotCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotCascadeExtension.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = VolumeSnapshotVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(VolumeSnapshotVO.class);
        completion.success();
    }

    private NeedReplyMessage makeMsg(final String suuid, boolean volumeDeletion) {
        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
        sq.select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid);
        sq.add(VolumeSnapshotVO_.uuid, Op.EQ, suuid);
        Tuple t = sq.findTuple();
        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);

        VolumeSnapshotDeletionMsg msg = new VolumeSnapshotDeletionMsg();
        msg.setSnapshotUuid(suuid);
        msg.setTreeUuid(treeUuid);
        msg.setVolumeUuid(volumeUuid);
        msg.setVolumeDeletion(volumeDeletion);
        String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);

        if (volumeUuid == null) {
            return msg;
        }

        // To delete a volume snapshot, we need first to be synchronized in the volume queue.
        VolumeSnapshotOverlayMsg omsg = new VolumeSnapshotOverlayMsg();
        omsg.setMessage(msg);
        omsg.setVolumeUuid(volumeUuid);
        bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, volumeUuid);

        return omsg;
    }


    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<NeedReplyMessage> msgs = new ArrayList<>();
        if (VolumeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VolumeDeletionStruct> vols = action.getParentIssuerContext();
            for (VolumeDeletionStruct vol : vols) {
                msgs.addAll(handleVolumeDeletion(vol));
            }
        } else if (VolumeSnapshotVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VolumeSnapshotInventory> sinvs = action.getParentIssuerContext();
            for (VolumeSnapshotInventory sinv : sinvs) {
               msgs.add(handleSnapshotDeletion(sinv));
            }
        }

        if (msgs.isEmpty()) {
            completion.success();
            return;
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                completion.success();
            }
        });
    }

    private NeedReplyMessage handleSnapshotDeletion(VolumeSnapshotInventory sinv) {
        return makeMsg(sinv.getUuid(), false);
    }

    private List<NeedReplyMessage> handleVolumeDeletion(VolumeDeletionStruct vol) {
        if (!VolumeDeletionPolicy.Direct.toString().equals(vol.getDeletionPolicy())) {
            return new ArrayList<>();
        }

        List<NeedReplyMessage> ret = new ArrayList<>();
        SimpleQuery<VolumeSnapshotTreeVO> cq = dbf.createQuery(VolumeSnapshotTreeVO.class);
        cq.select(VolumeSnapshotTreeVO_.uuid);
        cq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getInventory().getUuid());
        List<String> cuuids = cq.listValue();
        for (String cuuid : cuuids) {
            // deleting full snapshot of chain will cause whole chain to be deleted
            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.select(VolumeSnapshotVO_.uuid);
            q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, cuuid);
            q.add(VolumeSnapshotVO_.parentUuid, Op.NULL);
            String suuid = q.findValue();

            if (suuid == null) {
                // this is a storage snapshot, don't delete it on primary storage
                continue;
            }

            ret.add(makeMsg(suuid, true));
        }

        return ret;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VolumeVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<VolumeSnapshotInventory> fromAction(CascadeAction action) {
        List<VolumeSnapshotInventory> ret = null;
        if (VolumeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VolumeDeletionStruct> vols = action.getParentIssuerContext();
            List<String> volUuids = CollectionUtils.transformToList(vols, new Function<String, VolumeDeletionStruct>() {
                @Override
                public String call(VolumeDeletionStruct arg) {
                    return arg.getInventory().getUuid();
                }
            });

            if (volUuids.isEmpty()) {
                return null;
            }

            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.add(VolumeSnapshotVO_.volumeUuid, Op.IN, volUuids);
            List<VolumeSnapshotVO> vos = q.list();
            if (!vos.isEmpty()) {
                ret = VolumeSnapshotInventory.valueOf(vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VolumeSnapshotInventory> invs = fromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }

        return null;
    }
}
