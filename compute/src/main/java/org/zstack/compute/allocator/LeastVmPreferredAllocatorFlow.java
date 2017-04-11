package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 * This flow returns a list of host candidates sorted by the number of their VMs.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LeastVmPreferredAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(LeastVmPreferredAllocatorFlow.class);
    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    private List<Tuple> findLeastVmHost(List<String> huuids) {
        String sql = "select count(vm) as cnt, host.uuid" +
                " from HostVO host" +
                " Left Join VmInstanceVO vm on host.uuid = vm.hostUuid" +
                " where host.uuid in (:huuids)" +
                " group by host.uuid order by cnt";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("huuids", huuids);
        return q.getResultList();
    }

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        if (spec.isListAllHosts()) {
            next(candidates);
            return;
        }

        List<String> huuids = getHostUuidsFromCandidates();
        List<Tuple> tuples = findLeastVmHost(huuids);

        // no VM running on any candidate host(s)
        if (tuples.isEmpty()) {
            next(candidates);
            return;
        }

        List<HostVO> sorted = new ArrayList<>(candidates.size());

        Map<String, HostVO> dict = new HashMap<>();
        for (HostVO hvo: candidates) {
            dict.put(hvo.getUuid(), hvo);
        }

        if (huuids.size() > tuples.size()) {
            HashSet<String> hostsWithVMs = new HashSet<>();
            for (Tuple t : tuples) {
                String hostUuid = t.get(1, String.class);
                hostsWithVMs.add(hostUuid);
            }

            for (String huuid : huuids) {
                if (!hostsWithVMs.contains(huuid)) {
                    sorted.add(dict.get(huuid));
                }
            }
        }

        // Note: the query result is ordered.
        for (Tuple t : tuples) {
            String hostUuid = t.get(1, String.class);
            sorted.add(dict.get(hostUuid));
        }

        next(sorted);
    }
}
