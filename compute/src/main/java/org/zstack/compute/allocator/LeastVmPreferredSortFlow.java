package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This flow returns a list of host candidates sorted by the number of their VMs.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LeastVmPreferredSortFlow extends AbstractHostSortorFlow {
    private static final CLogger logger = Utils.getLogger(LeastVmPreferredSortFlow.class);
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
    public void sort() {
        if (spec.isListAllHosts()) {
            return;
        }

        Map<String, HostInventory> hosts = candidates.stream().
                collect(Collectors.toMap(HostInventory::getUuid, (candidate) -> candidate));
        List<String> tmp = CollectionUtils.transformToList(
                findLeastVmHost(hosts.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList())), new Function<String, Tuple>() {
                    @Override
                    public String call(Tuple arg) {
                        return arg.get(1, String.class);
                    }
                });

        List<HostInventory> sorted = new ArrayList<>(candidates.size());

        candidates.forEach(candidate -> {
            if (!tmp.contains(candidate.getUuid())) {
                sorted.add(candidate);
            }
        });

        tmp.forEach(huuid -> {
            sorted.add(hosts.get(huuid));
        });
        candidates.clear();
        candidates.addAll(sorted);
    }
}
