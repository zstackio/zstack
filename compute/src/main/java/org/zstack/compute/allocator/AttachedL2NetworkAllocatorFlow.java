package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedL2NetworkAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(AttachedL2NetworkAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

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
            Set<String> l2s = l2ClusterMap.get(ref.getClusterUuid());
            if (l2s == null) {
                l2s = new HashSet<>();
                l2ClusterMap.put(ref.getClusterUuid(), l2s);
            }
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

        if (hostUuids.isEmpty()) {
            sql = "select h from HostVO h where h.clusterUuid in (:cuuids)";
            TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
            hq.setParameter("cuuids", clusterUuids);

            if (usePagination()) {
                hq.setFirstResult(paginationInfo.getOffset());
                hq.setMaxResults(paginationInfo.getLimit());
            }

            return hq.getResultList();
        } else {
            sql = "select h from HostVO h where h.clusterUuid in (:cuuids) and h.uuid in (:huuids)";
            TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
            hq.setParameter("cuuids", clusterUuids);
            hq.setParameter("huuids", hostUuids);

            if (usePagination()) {
                hq.setFirstResult(paginationInfo.getOffset());
                hq.setMaxResults(paginationInfo.getLimit());
            }

            return hq.getResultList();
        }
    }

    @Override
    public void allocate() {
        if (spec.getL3NetworkUuids().isEmpty()) {
            if (CoreGlobalProperty.UNIT_TEST_ON || spec.isAllowNoL3Networks()) {
                skip();
                return;
            } else {
                throw new CloudRuntimeException("l3Network uuids can not be empty AttachedL2NetworkAllocatorFlow");
            }
        }

        if (amITheFirstFlow()) {
            candidates = allocate(spec.getL3NetworkUuids(), new ArrayList<>());
        } else {
            candidates = allocate(spec.getL3NetworkUuids(), getHostUuidsFromCandidates());
        }

        if (candidates.isEmpty()) {
            fail(String.format("no host found in clusters that has attached to L2Networks which have L3Networks%s", spec.getL3NetworkUuids()));
        } else {
            next(candidates);
        }
    }
}
