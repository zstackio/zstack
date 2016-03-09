package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.host.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostStateAndHypervisorAllocatorFlow extends AbstractHostAllocatorFlow {
	private static final CLogger logger = Utils.getLogger(HostStateAndHypervisorAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

	private List<HostVO> allocate(String hypervisorType) {
		SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
		query.add(HostVO_.state, Op.EQ, HostState.Enabled);
		query.add(HostVO_.status, Op.EQ, HostStatus.Connected);
	    if (hypervisorType != null) {
	        query.add(HostVO_.hypervisorType, Op.EQ, hypervisorType);
	    }

        if (usePagination()) {
            query.setStart(paginationInfo.getOffset());
            query.setLimit(paginationInfo.getLimit());
        }

		return query.list();
	}

	private List<HostVO> allocate(List<HostVO> vos, String hypervisorType) {
		List<HostVO> lst = new ArrayList<HostVO>(vos.size());
		for (HostVO vo : vos) {
		    if (hypervisorType != null && !hypervisorType.equals(vo.getHypervisorType())) {
		        continue;
		    }
		    
			if (vo.getState() == HostState.Enabled && vo.getStatus() == HostStatus.Connected) {
			    lst.add(vo);
			}
		}
		return lst;
	}

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            candidates = allocate(spec.getHypervisorType());
        } else {
            candidates = allocate(candidates, spec.getHypervisorType());
        }

        if (candidates.isEmpty()) {
            StringBuilder sb = new StringBuilder("no host having");
            sb.append(String.format(" state=Enabled"));
            sb.append(String.format(" status=Connected"));
            if (spec.getHypervisorType() != null) {
                sb.append(String.format(" hypervisorType=%s", spec.getHypervisorType()));
            }
            sb.append(" found");
            fail(sb.toString());
        } else {
            next(candidates);
        }
    }
}
