package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
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

    private boolean isNoEnabledHost() {
        return !candidates.stream().anyMatch(vo -> HostState.Enabled == vo.getState());
    }

    private boolean isNoConnectedHost() {
        return !candidates.stream().anyMatch(vo -> HostStatus.Connected == vo.getStatus());
    }

    private boolean isNoHypervisor(String hyType) {
        return !candidates.stream().anyMatch(vo -> hyType.equals(vo.getHypervisorType()));
    }

    @Override
    public void allocate() {
        List<HostVO> ret;
        if (amITheFirstFlow()) {
            ret = allocate(spec.getHypervisorType());
        } else {
            ret = allocate(candidates, spec.getHypervisorType());
        }

        if (ret.isEmpty()) {

            String error;
            if (isNoConnectedHost()) {
                error = String.format("no Connected hosts found in the [%s] candidate hosts", candidates.size());
            } else if (isNoEnabledHost()) {
                error = String.format("no Enabled hosts found in the [%s] candidate hosts", candidates.size());
            } else if (isNoHypervisor(spec.getHypervisorType())) {
                error = String.format("no Enabled hosts found in the [%s] candidate hosts having the hypervisor type [%s]",
                        candidates.size(), spec.getHypervisorType());
            } else {
                StringBuilder sb = new StringBuilder("no host having");
                sb.append(String.format(" state=Enabled"));
                sb.append(String.format(" status=Connected"));
                if (spec.getHypervisorType() != null) {
                    sb.append(String.format(" hypervisorType=%s", spec.getHypervisorType()));
                }
                sb.append(" found");
                error = sb.toString();
            }

            fail(error);
        } else {
            logger.info(String.format("found [%s] hosts with hypervisor type [%s] are Enabled and Connected",
                    ret.size(), spec.getHypervisorType()));
            next(ret);
        }
    }
}
