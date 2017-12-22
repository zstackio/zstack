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
    private List<String> findLeastVmHost(List<String> huuids) {
        String sql = "select host.uuid" +
                " from HostVO host" +
                " Left Join VmInstanceVO vm on host.uuid = vm.hostUuid" +
                " where host.uuid in (:huuids)" +
                " group by host.uuid order by count(vm)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("huuids", huuids);
        return q.getResultList();
    }

    @Override
    public void sort() {
        Map<String, HostInventory> hosts = candidates.stream().collect(Collectors.toMap(HostInventory::getUuid, (candidate) -> candidate));
        List<String> sortedHostUuids = findLeastVmHost(candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()));

        candidates.clear();
        sortedHostUuids.forEach(huuid -> candidates.add(hosts.get(huuid)));
    }
}
