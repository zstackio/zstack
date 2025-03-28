package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeInventory;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class VmVolumeSnapshotChainUtil {
    static public Map<String, List<String>> getVmVolumesAliveSnapshotChain(List<VolumeInventory> vmVolumes) {
        List<String> volumeUuids = vmVolumes.stream().map(VolumeInventory::getUuid).collect(Collectors.toList());
        Map<String, String> volumeInstallPathByUuid = vmVolumes.stream().collect(Collectors.toMap(VolumeInventory::getUuid, VolumeInventory::getInstallPath));

        List<String> currentTreeUuids = Q.New(VolumeSnapshotTreeVO.class)
                .eq(VolumeSnapshotTreeVO_.current, true)
                .eq(VolumeSnapshotTreeVO_.status, VolumeSnapshotTreeStatus.Completed)
                .in(VolumeSnapshotVO_.volumeUuid, volumeUuids)
                .select(VolumeSnapshotTreeVO_.uuid).listValues();

        if (currentTreeUuids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<VolumeSnapshotVO> aliveChainVolumeSnapshots = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.status, VolumeSnapshotStatus.Ready)
                .in(VolumeSnapshotVO_.volumeUuid, volumeUuids)
                .in(VolumeSnapshotVO_.treeUuid, currentTreeUuids)
                .list();

        Map<String, List<VolumeSnapshotVO>> aliveChainVolumeSnapshotsByVolumeUuid = new HashMap<>();
        volumeUuids.forEach(volumeUuid -> aliveChainVolumeSnapshots.forEach(volumeSnapshotVO -> {
            if (Objects.equals(volumeSnapshotVO.getVolumeUuid(), volumeUuid)) {
                if (!aliveChainVolumeSnapshotsByVolumeUuid.containsKey(volumeUuid)) {
                    aliveChainVolumeSnapshotsByVolumeUuid.put(volumeUuid, new ArrayList<>());
                }
                aliveChainVolumeSnapshotsByVolumeUuid.get(volumeUuid).add(volumeSnapshotVO);
            }
        }));

        Map<String, List<String>> volumesSnapshotChain = new HashMap<>();
        aliveChainVolumeSnapshotsByVolumeUuid.forEach((volumeUuid, vos) -> {
            String latestSnapshotUuid = vos.stream().filter(VolumeSnapshotAO::isLatest).map(VolumeSnapshotVO::getUuid).findFirst().orElse(null);
            if (latestSnapshotUuid == null) {
                throw new OperationFailureException(operr("no latest snapshot found for volume[uuid:%s] on tree[%s ]", volumeUuid, vos.get(0).getTreeUuid()));
            }
            List<String> aliveChainInstallPath = new ArrayList<>();
            aliveChainInstallPath.add(volumeInstallPathByUuid.get(volumeUuid));
            aliveChainInstallPath.addAll(VolumeSnapshotTree.fromVOs(vos).getAliveChainInstallPath(latestSnapshotUuid));
            volumesSnapshotChain.put(volumeUuid, aliveChainInstallPath);
        });
        return volumesSnapshotChain;
    }
}
