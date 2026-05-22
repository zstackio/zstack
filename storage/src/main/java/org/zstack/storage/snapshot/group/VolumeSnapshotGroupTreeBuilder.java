package org.zstack.storage.snapshot.group;

import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupTreeInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupTreeRefInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VolumeSnapshotGroupTreeBuilder {
    public List<VolumeSnapshotGroupTreeInventory> buildForVm(String vmInstanceUuid) {
        List<VolumeSnapshotGroupVO> groupVOs = Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmInstanceUuid)
                .list();
        if (groupVOs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> groupUuids = groupVOs.stream()
                .map(VolumeSnapshotGroupVO::getUuid)
                .collect(Collectors.toList());

        List<VolumeSnapshotGroupRefVO> refs = Q.New(VolumeSnapshotGroupRefVO.class)
                .in(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuids)
                .list();

        Map<String, VolumeSnapshotVO> liveSnapVOs = loadSnapshotVOsWithAncestors(refs);
        Map<String, String> parentMap = buildParentMap(liveSnapVOs);
        Map<String, String> snapToGroup = buildSnapToGroupMap(refs);

        Map<String, List<VolumeSnapshotGroupRefVO>> refsByGroup = refs.stream()
                .collect(Collectors.groupingBy(VolumeSnapshotGroupRefVO::getVolumeSnapshotGroupUuid));

        Map<String, VolumeSnapshotGroupTreeInventory> groupNodeMap =
                buildGroupNodes(groupVOs, refsByGroup, liveSnapVOs);

        Map<String, Date> groupCreateDate = groupVOs.stream()
                .collect(HashMap::new,
                        (m, g) -> m.put(g.getUuid(), g.getCreateDate()),
                        HashMap::putAll);

        linkParents(groupNodeMap, refsByGroup, parentMap, snapToGroup, groupCreateDate);

        return assembleForest(groupNodeMap);
    }

    private Map<String, VolumeSnapshotVO> loadSnapshotVOsWithAncestors(List<VolumeSnapshotGroupRefVO> refs) {
        Set<String> pendingSnapshotUuids = refs.stream()
                .filter(r -> !r.isSnapshotDeleted())
                .map(VolumeSnapshotGroupRefVO::getVolumeSnapshotUuid)
                .collect(Collectors.toSet());
        Map<String, VolumeSnapshotVO> snapshotVOs = new HashMap<>();
        Set<String> queriedSnapshotUuids = new HashSet<>();

        while (!pendingSnapshotUuids.isEmpty()) {
            Set<String> batch = pendingSnapshotUuids.stream()
                    .filter(queriedSnapshotUuids::add)
                    .collect(Collectors.toSet());
            if (batch.isEmpty()) {
                break;
            }

            List<VolumeSnapshotVO> snapshots = Q.New(VolumeSnapshotVO.class)
                    .in(VolumeSnapshotVO_.uuid, batch)
                    .list();
            snapshots.forEach(snapshot -> snapshotVOs.put(snapshot.getUuid(), snapshot));
            pendingSnapshotUuids = snapshots.stream()
                    .map(VolumeSnapshotVO::getParentUuid)
                    .filter(parentUuid -> parentUuid != null && !queriedSnapshotUuids.contains(parentUuid))
                    .collect(Collectors.toSet());
        }

        return snapshotVOs;
    }

    private Map<String, String> buildParentMap(Map<String, VolumeSnapshotVO> snapVOs) {
        Map<String, String> m = new HashMap<>();
        for (VolumeSnapshotVO v : snapVOs.values()) {
            m.put(v.getUuid(), v.getParentUuid());
        }
        return m;
    }

    private Map<String, String> buildSnapToGroupMap(List<VolumeSnapshotGroupRefVO> refs) {
        Map<String, String> m = new HashMap<>();
        for (VolumeSnapshotGroupRefVO r : refs) {
            m.put(r.getVolumeSnapshotUuid(), r.getVolumeSnapshotGroupUuid());
        }
        return m;
    }

    private Map<String, VolumeSnapshotGroupTreeInventory> buildGroupNodes(
            List<VolumeSnapshotGroupVO> groupVOs,
            Map<String, List<VolumeSnapshotGroupRefVO>> refsByGroup,
            Map<String, VolumeSnapshotVO> snapVOs) {
        Map<String, VolumeSnapshotGroupTreeInventory> groupNodeMap = new HashMap<>();
        for (VolumeSnapshotGroupVO g : groupVOs) {
            VolumeSnapshotGroupTreeInventory node = new VolumeSnapshotGroupTreeInventory();
            node.setUuid(g.getUuid());
            node.setName(g.getName());
            node.setDescription(g.getDescription());
            node.setVmInstanceUuid(g.getVmInstanceUuid());
            node.setCreateDate(g.getCreateDate());
            node.setLastOpDate(g.getLastOpDate());

            List<VolumeSnapshotGroupRefVO> groupRefs = refsByGroup.getOrDefault(g.getUuid(), Collections.emptyList());
            boolean hasDeletedRef = groupRefs.stream().anyMatch(VolumeSnapshotGroupRefVO::isSnapshotDeleted);
            boolean hasMissingLiveSnapshot = groupRefs.stream()
                    .filter(r -> !r.isSnapshotDeleted())
                    .anyMatch(r -> !snapVOs.containsKey(r.getVolumeSnapshotUuid()));
            node.setIncomplete(groupRefs.size() != g.getSnapshotCount()
                    || hasDeletedRef
                    || hasMissingLiveSnapshot);

            node.setRefs(buildRefInventories(groupRefs, snapVOs));
            groupNodeMap.put(g.getUuid(), node);
        }
        return groupNodeMap;
    }

    private List<VolumeSnapshotGroupTreeRefInventory> buildRefInventories(
            List<VolumeSnapshotGroupRefVO> groupRefs,
            Map<String, VolumeSnapshotVO> snapVOs) {
        List<VolumeSnapshotGroupTreeRefInventory> refInvs = new ArrayList<>();
        for (VolumeSnapshotGroupRefVO r : groupRefs) {
            VolumeSnapshotGroupTreeRefInventory refInv = new VolumeSnapshotGroupTreeRefInventory();
            refInv.setVolumeUuid(r.getVolumeUuid());
            refInv.setVolumeName(r.getVolumeName());
            refInv.setVolumeType(r.getVolumeType());
            refInv.setVolumeSnapshotUuid(r.getVolumeSnapshotUuid());
            refInv.setSnapshotDeleted(r.isSnapshotDeleted());
            if (r.isSnapshotDeleted()) {
                refInv.setSnapshot(null);
            } else {
                VolumeSnapshotVO svo = snapVOs.get(r.getVolumeSnapshotUuid());
                refInv.setSnapshot(svo == null ? null : VolumeSnapshotInventory.valueOf(svo));
            }
            refInvs.add(refInv);
        }
        return refInvs;
    }

    private void linkParents(Map<String, VolumeSnapshotGroupTreeInventory> groupNodeMap,
                             Map<String, List<VolumeSnapshotGroupRefVO>> refsByGroup,
                             Map<String, String> parentMap,
                             Map<String, String> snapToGroup,
                             Map<String, Date> groupCreateDate) {
        for (VolumeSnapshotGroupTreeInventory node : groupNodeMap.values()) {
            List<String> parentGroupUuids = resolveParentGroupUuids(node.getUuid(),
                    refsByGroup.getOrDefault(node.getUuid(), Collections.emptyList()),
                    parentMap, snapToGroup, groupCreateDate);
            parentGroupUuids = parentGroupUuids.stream()
                    .filter(groupNodeMap::containsKey)
                    .collect(Collectors.toList());
            node.setParentGroupUuids(parentGroupUuids);
            if (!parentGroupUuids.isEmpty()) {
                node.setParentGroupUuid(parentGroupUuids.get(parentGroupUuids.size() - 1));
            }
        }
    }

    private List<String> resolveParentGroupUuids(String selfGroupUuid,
                                                 List<VolumeSnapshotGroupRefVO> selfRefs,
                                                 Map<String, String> parentMap,
                                                 Map<String, String> snapToGroup,
                                                 Map<String, Date> groupCreateDate) {
        Set<String> parentGroupUuids = new HashSet<>();
        for (VolumeSnapshotGroupRefVO r : selfRefs) {
            if (r.isSnapshotDeleted()) {
                continue;
            }
            String cur = parentMap.get(r.getVolumeSnapshotUuid());
            Set<String> visited = new HashSet<>();
            visited.add(r.getVolumeSnapshotUuid());
            while (cur != null && !visited.contains(cur)) {
                visited.add(cur);
                String g = snapToGroup.get(cur);
                if (g != null && !g.equals(selfGroupUuid)) {
                    parentGroupUuids.add(g);
                    break;
                }
                cur = parentMap.get(cur);
            }
        }
        if (parentGroupUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ranked = new ArrayList<>(parentGroupUuids);
        ranked.sort((a, b) -> {
            Date da = groupCreateDate.get(a);
            Date db = groupCreateDate.get(b);
            int cmp = Comparator.nullsLast(Date::compareTo).compare(da, db);
            if (cmp != 0) {
                return cmp;
            }
            return a.compareTo(b);
        });
        return ranked;
    }

    private List<VolumeSnapshotGroupTreeInventory> assembleForest(Map<String, VolumeSnapshotGroupTreeInventory> groupNodeMap) {
        List<VolumeSnapshotGroupTreeInventory> forest = new ArrayList<>();
        for (VolumeSnapshotGroupTreeInventory node : groupNodeMap.values()) {
            if (node.getParentGroupUuid() == null) {
                forest.add(node);
            } else {
                groupNodeMap.get(node.getParentGroupUuid()).getChildren().add(node);
            }
        }

        Comparator<VolumeSnapshotGroupTreeInventory> byCreateDateAsc =
                Comparator.comparing(VolumeSnapshotGroupTreeInventory::getCreateDate,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
        forest.sort(byCreateDateAsc);
        for (VolumeSnapshotGroupTreeInventory node : groupNodeMap.values()) {
            node.getChildren().sort(byCreateDateAsc);
        }

        markCurrent(groupNodeMap);
        return forest;
    }

    private void markCurrent(Map<String, VolumeSnapshotGroupTreeInventory> groupNodeMap) {
        VolumeSnapshotGroupTreeInventory current = null;
        for (VolumeSnapshotGroupTreeInventory node : groupNodeMap.values()) {
            if (!node.getChildren().isEmpty()) {
                continue;
            }
            current = pickNewer(current, node);
        }
        if (current == null) {
            for (VolumeSnapshotGroupTreeInventory node : groupNodeMap.values()) {
                current = pickNewer(current, node);
            }
        }
        if (current != null) {
            current.setCurrent(true);
        }
    }

    private VolumeSnapshotGroupTreeInventory pickNewer(VolumeSnapshotGroupTreeInventory current,
                                                       VolumeSnapshotGroupTreeInventory candidate) {
        if (current == null) {
            return candidate;
        }
        Date candidateDate = candidate.getCreateDate();
        Date currentDate = current.getCreateDate();
        int cmp = Comparator.nullsFirst(Date::compareTo).compare(candidateDate, currentDate);
        if (cmp > 0) {
            return candidate;
        }
        if (cmp == 0 && candidate.getUuid().compareTo(current.getUuid()) > 0) {
            return candidate;
        }
        return current;
    }
}
