package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
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
        String sql = "select host.uuid, count(vm.uuid) as cnt" +
                " from HostVO host" +
                " Left Join VmInstanceVO vm on host.uuid = vm.hostUuid" +
                " where host.uuid in (:huuids)" +
                " group by host.uuid order by cnt";

        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("huuids", huuids).list();
        long minVm = tuples.get(0).get(1, Long.class);

        /* count vm numbers of each host, save to  hostMap */
        Map<String, Long> hostMap = tuples.stream().collect(Collectors.toMap(tuple ->
                    tuple.get(0, String.class), tuple -> tuple.get(1, Long.class)));

        /* find the best hosts which have the minimum number of vms  */
        List<String> preferHost = hostMap.entrySet().stream().filter(host -> host.getValue() == minVm)
                .map(host->host.getKey()).collect(Collectors.toList());

        /* put the best hosts at the head of result, but important thing is:
         * !!! keep the best host order unchanged, or LeastVmPreferredSortFlow will overwrite the previous shuffle result
         * Testcase for this change is  TestConcurrentAllocationCase under test-premium */
        List<String> result = huuids.stream().filter(host -> preferHost.contains(host)).collect(Collectors.toList());
        result.addAll(huuids.stream().filter(host -> !preferHost.contains(host)).collect(Collectors.toList()));

        return result;
    }

    @Override
    public void sort() {
        Map<String, HostInventory> hosts = candidates.stream().collect(Collectors.toMap(HostInventory::getUuid, (candidate) -> candidate));
        List<String> sortedHostUuids = findLeastVmHost(candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()));

        candidates.clear();
        sortedHostUuids.forEach(huuid -> candidates.add(hosts.get(huuid)));
        logger.debug(String.format("Sorted by LeastVmPreferred the hosts %s", candidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));

    }
}
