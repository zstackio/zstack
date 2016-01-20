package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostCapacityAllocatorFlow extends AbstractHostAllocatorFlow {
	private static final CLogger logger = Utils.getLogger(HostCapacityAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private HostCapacityReserveManager reserveMgr;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;

	@Transactional(readOnly = true)
	private List<HostVO> allocate(long cpu, long memory) {
		String sql = "select c from HostCapacityVO c";
		TypedQuery<HostCapacityVO> query = dbf.getEntityManager().createQuery(sql, HostCapacityVO.class);
        if (usePagination()) {
            query.setFirstResult(paginationInfo.getOffset());
            query.setMaxResults(paginationInfo.getLimit());
        }
		List<HostCapacityVO> caps = query.getResultList();

        List<String> cds = new ArrayList<String>();
        for (HostCapacityVO c : caps) {
            if (c.getAvailableCpu() > cpu && ratioMgr.calculateHostAvailableMemoryByRatio(c.getUuid(), c.getAvailableMemory()) > memory) {
                cds.add(c.getUuid());
            }
        }

        if (cds.isEmpty()) {
            return new ArrayList<HostVO>();
        }

        sql = "select h from HostVO h where h.uuid in (:huuids)";
        TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
        hq.setParameter("huuids", cds);
        return hq.getResultList();
	}

	private List<HostVO> allocate(List<HostVO> vos, long cpu, long memory) {
        List<HostVO> ret = new ArrayList<HostVO>();
        for (HostVO hvo : vos) {
            if (hvo.getCapacity().getAvailableCpu() > cpu
                    && ratioMgr.calculateHostAvailableMemoryByRatio(hvo.getUuid(), hvo.getCapacity().getAvailableMemory()) > memory) {
                ret.add(hvo);
            }
        }

        return ret;
	}

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            candidates = allocate(spec.getCpuCapacity(), spec.getMemoryCapacity());
        } else {
            candidates = allocate(candidates, spec.getCpuCapacity(), spec.getMemoryCapacity());
        }

        candidates = reserveMgr.filterOutHostsByReservedCapacity(candidates, spec.getCpuCapacity(), spec.getMemoryCapacity());

        if (candidates.isEmpty()) {
            fail(String.format("no host having cpu[%s HZ], memory[%s bytes] found",
                    spec.getCpuCapacity(), spec.getMemoryCapacity()));
        } else {
            next(candidates);
        }
    }
}
