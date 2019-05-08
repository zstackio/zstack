package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.CheckVolumeSnapshotsMsg;
import org.zstack.header.volume.CheckVolumeSnapshotsReply;
import org.zstack.header.volume.VolumeConstant;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;

/**
 * Created by mingjian.deng on 2019/4/8.
 */
public class VolumeSnapshotExtension implements VolumeSnapshotCheckExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void checkBeforeDeleteSnapshot(VolumeSnapshotInventory snapshot, Completion completion) {
        if (!snapshot.isLatest()) {
            // not the latest snapshot, skip all bellow
            completion.success();
            return;
        }
        VolumeSnapshotTreeInventory currentTree = VolumeSnapshotManagerImpl.getCurrentTree(snapshot.getVolumeUuid());
        if (currentTree == null) {
            // no current tree, skip all bellow
            completion.success();
            return;
        }
        if (!currentTree.getUuid().equals(snapshot.getTreeUuid())) {
            // the snapshot is not on the current tree, skip all bellow
            completion.success();
            return;
        }

        List<VolumeSnapshotTreeVO> trees = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.volumeUuid, snapshot.getVolumeUuid()).eq(VolumeSnapshotTreeVO_.status, VolumeSnapshotTreeStatus.Creating).list();
        judgeAndMergeTrees(trees, currentTree, completion);
    }

    private ErrorCode correctVolumeSnapshotTree(String treeUuid, List<VolumeSnapshotTreeVO> trees, boolean completed) {
        ErrorCode error = null;

        if (completed){
            /** create snapshot succeed, we continue to complete the bellow:
             * 1. mark the new tree
             * 2. update the current tree
             */
            SQL.New("update VolumeSnapshotTreeVO tree" +
                    " set tree.current = false" +
                    " where tree.current = true" +
                    " and tree.volumeUuid = :volUuid").param("volUuid", trees.get(0).getVolumeUuid()).execute();

            VolumeSnapshotTreeVO tree = dbf.findByUuid(treeUuid, VolumeSnapshotTreeVO.class);
            tree.setStatus(VolumeSnapshotTreeStatus.Completed);
            tree.setCurrent(true);
            dbf.persistAndRefresh(tree);
        } else {
            /**
             * create snapshot failed, we rollback it with the bellow step:
             * mark the new tree
             */
            VolumeSnapshotTreeVO tree = dbf.findByUuid(treeUuid, VolumeSnapshotTreeVO.class);
            tree.setStatus(VolumeSnapshotTreeStatus.Failed);
            tree.setCurrent(false);
            dbf.persistAndRefresh(tree);
        }
        return error;
    }

    private void judgeAndMergeTrees(List<VolumeSnapshotTreeVO> trees, VolumeSnapshotTreeInventory currentTree, Completion completion) {
        if (trees.isEmpty()) {
            completion.success();
            return;
        }
        List<VolumeSnapshotVO> snapshots = new ArrayList<>();

        for (VolumeSnapshotTreeVO tree: trees) {
            VolumeSnapshotVO vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, tree.getUuid()).eq(VolumeSnapshotVO_.status, VolumeSnapshotStatus.Creating).find();
            snapshots.add(vo);
        }

        if (snapshots.isEmpty()) {
            completion.success();
            return;
        }

        if (!VolumeSnapshotGlobalConfig.SNAPSHOT_CORRECT_AFTER_RESTART_MANAGEMENT.value(Boolean.class)) {
            completion.fail(err(VolumeSnapshotErrors.FULL_SNAPSHOT_ERROR, "full snapshot on volume [%s] is creating and not finished correctly, " +
                    "please check the trees [%s] and their snapshots manually", currentTree.getVolumeUuid(), trees.toString()));
            return;
        }

        CheckVolumeSnapshotsMsg gmsg = new CheckVolumeSnapshotsMsg();
        String currentTreeSnapshotInstallPath = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.primaryStorageInstallPath).eq(VolumeSnapshotVO_.treeUuid, currentTree.getUuid()).findValue();
        gmsg.setSnapshots(VolumeSnapshotInventory.valueOf(snapshots));

        gmsg.setVolumeUuid(currentTree.getVolumeUuid());
        gmsg.setCurrentTreeSnapshotInstallPath(currentTreeSnapshotInstallPath);
        bus.makeLocalServiceId(gmsg, VolumeConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CheckVolumeSnapshotsReply r = reply.castReply();
                    if (r.getSnapshotUuid() == null) {
                        completion.success();
                        return;
                    }
                    VolumeSnapshotVO snapshot = dbf.findByUuid(r.getSnapshotUuid(), VolumeSnapshotVO.class);
                    ErrorCode error = correctVolumeSnapshotTree(snapshot.getTreeUuid(), trees, r.isCompleted());
                    if (error != null) {
                        completion.fail(error);
                    } else {
                        completion.success();
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
