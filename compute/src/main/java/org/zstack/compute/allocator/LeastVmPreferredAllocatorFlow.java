package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LeastVmPreferredAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;

    class VmNumHost {
        long vmNum;
        String hostUuid;
    }

    @Transactional(readOnly = true)
    private List<Tuple> findLeastVmHost(List<String> huuids) {
        String sql = "select count(vm), host.uuid from VmInstanceVO vm, HostVO host where vm.hostUuid = host.uuid and host.uuid in (:huuids) group by host.uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("huuids", huuids);
        return q.getResultList();
    }

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        List<String> huuids = getHostUuidsFromCandidates();
        List<Tuple> tuples = findLeastVmHost(huuids);

        if (spec.isListAllHosts()) {
            next(candidates);
            return;
        }

        // no VM running on host
        if (tuples.isEmpty()) {
            next(candidates);
            return;
        }


        // for host not having vm running, put vm number to zero
        Map<String, VmNumHost> mp = new HashMap<String, VmNumHost>();
        for (Tuple t : tuples) {
            long num = t.get(0, Long.class);
            String hostUuid = t.get(1, String.class);
            VmNumHost vh = new VmNumHost();
            vh.vmNum = num;
            vh.hostUuid = hostUuid;
            mp.put(hostUuid, vh);
        }

        for (String huuid : huuids) {
            VmNumHost vh = mp.get(huuid);
            if (vh == null) {
                vh = new VmNumHost();
                vh.hostUuid = huuid;
                vh.vmNum = 0;
                mp.put(huuid, vh);
            }
        }

        long max = Integer.MAX_VALUE;
        String huuid = null;
        for (VmNumHost vh : mp.values()) {
            if (vh.vmNum < max) {
                huuid = vh.hostUuid;
                max = vh.vmNum;
            }
        }

        final String finalHuuid = huuid;
        HostVO target = CollectionUtils.find(candidates, new Function<HostVO, HostVO>() {
            @Override
            public HostVO call(HostVO arg) {
                return arg.getUuid().equals(finalHuuid) ? arg : null;
            }
        });

        next(CollectionDSL.list(target));
    }
}
