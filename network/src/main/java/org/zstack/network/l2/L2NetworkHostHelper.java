package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.network.l2.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2NetworkHostHelper {
    @Autowired
    DatabaseFacade dbf;

    public void attachL2NetworkToHost(String l2NetworkUuid, String hostUuid,
                                      String providerType,
                                      L2NetworkAttachStatus status) {
        L2NetworkHostRefVO ref = Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid).find();
        if (ref != null) {
            ref.setL2ProviderType(providerType);
            ref.setAttachStatus(status);
            dbf.updateAndRefresh(ref);
        }
    }

    public void attachL2NetworksToHost(List<L2NetworkClusterRefVO> l2NetworkClusterRefs, String hostUuid,
                                       L2NetworkAttachStatus status) {
        List<L2NetworkHostRefVO> newAdded = new ArrayList<>();
        List<L2NetworkHostRefVO> updated = Q.New(L2NetworkHostRefVO.class)
                .in(L2NetworkHostRefVO_.l2NetworkClusterRefId,
                        l2NetworkClusterRefs.stream().map(L2NetworkClusterRefVO::getId).collect(Collectors.toList()))
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid).list();
        updated.forEach(ref -> ref.setAttachStatus(status));

        List<L2NetworkClusterRefVO> newAddedRefs = l2NetworkClusterRefs.stream().filter(ref ->
                        !updated.stream().map(L2NetworkHostRefVO::getL2NetworkClusterRefId).collect(Collectors.toList()).contains(ref.getId()))
                .collect(Collectors.toList());


        newAddedRefs.forEach(ref -> {
            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(hostUuid);
            vo.setL2NetworkUuid(ref.getL2NetworkUuid());
            vo.setL2NetworkClusterRefId(ref.getId());
            vo.setL2ProviderType(ref.getL2ProviderType());
            vo.setAttachStatus(status);
            newAdded.add(vo);
        });

        dbf.persistCollection(newAdded);
        dbf.updateCollection(updated);
    }

    public void attachL2NetworkToHosts(String l2NetworkUuid, List<String> hostUuids,
                                       L2NetworkAttachStatus status) {
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .set(L2NetworkHostRefVO_.attachStatus, status)
                .update();
    }

    public void attachL2NetworkToHosts(String l2NetworkUuid, List<String> hostUuids,
                                       long l2NetworkClusterRefId, String providerType,
                                       L2NetworkAttachStatus status) {
        List<L2NetworkHostRefVO> refs = new ArrayList<>();

        hostUuids.forEach(uuid -> {
            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(uuid);
            vo.setL2NetworkUuid(l2NetworkUuid);
            vo.setL2NetworkClusterRefId(l2NetworkClusterRefId);
            vo.setL2ProviderType(providerType);
            vo.setAttachStatus(status);
            refs.add(vo);
        });

        dbf.persistCollection(refs);
    }

    public L2NetworkHostRefInventory getL2NetworkHostRef(String l2NetworkUuid, String hostUuid) {
        L2NetworkHostRefVO ref = Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid).find();
        if (ref == null) {
            return null;
        } else {
            return L2NetworkHostRefInventory.valueOf(ref);
        }
    }

    public L2NetworkHostRefInventory getL2NetworkHostRef(String l2NetworkUuid, String hostUuid, L2NetworkAttachStatus status) {
        L2NetworkHostRefVO ref = Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.attachStatus, status).find();
        if (ref == null) {
            return null;
        } else {
            return L2NetworkHostRefInventory.valueOf(ref);
        }
    }

    public List<L2NetworkHostRefInventory> getL2NetworkHostRefs(String l2NetworkUuid, List<String> hostUuids, L2NetworkAttachStatus status) {
        return L2NetworkHostRefInventory.valueOf(Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .eq(L2NetworkHostRefVO_.attachStatus, status)
                .list());
    }

    public List<L2NetworkHostRefInventory> getL2NetworkHostRefs(List<String> l2NetworkUuids, String hostUuid, L2NetworkAttachStatus status) {
        return L2NetworkHostRefInventory.valueOf(Q.New(L2NetworkHostRefVO.class)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.attachStatus, status)
                .list());
    }

    public List<L2NetworkHostRefInventory> getL2NetworkHostRefs(String l2NetworkUuid, L2NetworkAttachStatus status) {
        return L2NetworkHostRefInventory.valueOf(Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.attachStatus, status)
                .list());
    }

    public List<L2NetworkHostRefInventory> getL2NetworkHostRefs(Long l2NetworkClusterRefId, L2NetworkAttachStatus status) {
        return L2NetworkHostRefInventory.valueOf(Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkClusterRefId, l2NetworkClusterRefId)
                .eq(L2NetworkHostRefVO_.attachStatus, status)
                .list());
    }
}
