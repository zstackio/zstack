package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkAttachStatus;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.network.l2.L2NetworkHostRefVO_;
import org.zstack.header.network.l3.L3NetworkInventory;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedL2NetworkAllocatorFlow extends AbstractHostAllocatorFlow {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Transactional(readOnly = true)
    private List<HostVO> allocate(Collection<String> l3NetworkUuids, Collection<String> hostUuids) {
        String sql = "select l3.l2NetworkUuid from L3NetworkVO l3 where l3.uuid in (:l3uuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("l3uuids", l3NetworkUuids);
        List<String> l2uuids = q.getResultList();
        if (l2uuids.isEmpty()) {
            return new ArrayList<>();
        }

        sql = "select ref from L2NetworkClusterRefVO ref where ref.l2NetworkUuid in (:l2uuids)";
        TypedQuery<L2NetworkClusterRefVO> rq = dbf.getEntityManager().createQuery(sql, L2NetworkClusterRefVO.class);
        rq.setParameter("l2uuids", l2uuids);
        List<L2NetworkClusterRefVO> refs = rq.getResultList();
        if (refs.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Set<String>> l2ClusterMap = new HashMap<>();
        for (L2NetworkClusterRefVO ref : refs) {
            Set<String> l2s = l2ClusterMap.computeIfAbsent(ref.getClusterUuid(), k -> new HashSet<>());
            l2s.add(ref.getL2NetworkUuid());
        }

        Set<String> clusterUuids = new HashSet<>();
        for (Map.Entry<String, Set<String>> e : l2ClusterMap.entrySet()) {
            if (e.getValue().containsAll(l2uuids)) {
                clusterUuids.add(e.getKey());
            }
        }

        if (clusterUuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> retHostUuids;
        if (hostUuids.isEmpty()) {
            retHostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                    .in(HostVO_.clusterUuid, clusterUuids).listValues();
        } else {
            retHostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                    .in(HostVO_.clusterUuid, clusterUuids)
                    .in(HostVO_.uuid, hostUuids).listValues();
        }

        if (retHostUuids.isEmpty()){
            return new ArrayList<>();
        }

        /* in normal case, there is no L2NetworkHostRefVO  */
        List<String> excludeHostUuids = Q.New(L2NetworkHostRefVO.class).in(L2NetworkHostRefVO_.l2NetworkUuid, l2uuids)
                .notEq(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Attached)
                .select(L2NetworkHostRefVO_.hostUuid).listValues();
        retHostUuids.removeAll(excludeHostUuids);
        if (retHostUuids.isEmpty()){
            return new ArrayList<>();
        }

        sql = "select h from HostVO h where h.uuid in (:huuids)";
        TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
        hq.setParameter("huuids", retHostUuids);

        if (usePagination()) {
            hq.setFirstResult(paginationInfo.getOffset());
            hq.setMaxResults(paginationInfo.getLimit());
        }

        return hq.getResultList();
    }

    @Override
    public void allocate() {
        if (spec.getL3NetworkUuids().isEmpty()) {
            if (spec.isAllowNoL3Networks()) {
                skip();
                return;
            } else {
                throw new CloudRuntimeException("l3Network uuids can not be empty AttachedL2NetworkAllocatorFlow");
            }
        }

        List<String> l3Uuids = spec.getL3NetworkUuids();
        List<L3NetworkInventory> serviceL3s = new ArrayList<>();
        for (GetL3NetworkForVmNetworkService extp : pluginRgty.getExtensionList(GetL3NetworkForVmNetworkService.class)) {
            serviceL3s.addAll(extp.getL3NetworkForVmNetworkService(spec.getVmInstance()));
        }
        if (!serviceL3s.isEmpty()) {
            l3Uuids.addAll(serviceL3s.stream().map(L3NetworkInventory::getUuid).distinct().collect(Collectors.toList()));
        }


        if (amITheFirstFlow()) {
            candidates = allocate(l3Uuids, new ArrayList<>());
        } else {
            candidates = allocate(l3Uuids, getHostUuidsFromCandidates());
        }

        if (candidates.isEmpty()) {
            fail(Platform.operr("no host found in clusters that has attached to L2Networks which have L3Networks%s", spec.getL3NetworkUuids()));
        } else {
            next(candidates);
        }
    }
}
