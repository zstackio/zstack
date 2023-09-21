package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l3.L3NetworkInventory;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import org.zstack.utils.logging.CLogger;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.core.db.Q;
import org.zstack.utils.Utils;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.host.*;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedL2NetworkAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(AttachedL2NetworkAllocatorFlow.class);

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
            if (spec.isAllowNoL3Networks()) {
                skip();
                return;
            } else {
                spec.setAllowNoL3Networks(true);

                String sql;
                Set<String> clusterUuids = new HashSet<>();
                clusterUuids.add(spec.getVmInstance().getClusterUuid());
                List<String> hostUuids = null;
                if (!amITheFirstFlow()) {
                    hostUuids = candidates.stream().map(HostVO::getUuid).collect(Collectors.toList());
                }

                if (hostUuids == null || hostUuids.isEmpty()) {
                    sql = "select h from HostVO h where h.clusterUuid in (:cuuids)";
                    TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
                    hq.setParameter("cuuids", clusterUuids);
                    if (usePagination()) {
                        hq.setFirstResult(paginationInfo.getOffset());
                        hq.setMaxResults(paginationInfo.getLimit());
                    }
                    candidates = hq.getResultList();
                } else {
                    sql = "select h from HostVO h where h.clusterUuid in (:cuuids) and h.uuid in (:huuids)";
                    TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
                    hq.setParameter("cuuids", clusterUuids);
                    hq.setParameter("huuids", hostUuids);

                    if (usePagination()) {
                        hq.setFirstResult(paginationInfo.getOffset());
                        hq.setMaxResults(paginationInfo.getLimit());
                    }
                    candidates = hq.getResultList();
               }

                next(candidates);
                logger.debug(String.format("vm clusteruuid:%s, candidates size:%s", spec.getVmInstance().getClusterUuid(), candidates.size()));
                return;
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
