package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmLabels;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.logging.Log;
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
                Log log = new Log(spec.getVmInstance().getUuid());
                log.log(VmLabels.VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_FAILURE_NO_CONNECTED_HOST, candidates.size());
                error = log.toString();
            } else if (isNoEnabledHost()) {
                Log log = new Log(spec.getVmInstance().getUuid());
                log.log(VmLabels.VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_FAILURE_NO_ENABLED_HOST, candidates.size());
                error = log.toString();
            } else if (isNoHypervisor(spec.getHypervisorType())) {
                Log log = new Log(spec.getVmInstance().getUuid());
                log.log(VmLabels.VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_FAILURE_NO_HYPERVISOR, candidates.size(), spec.getHypervisorType());
                error = log.toString();
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
            new Log(spec.getVmInstance().getUuid())
                    .log(VmLabels.VM_START_ALLOCATE_HOST_STATE_HYPERVISOR_SUCCESS, ret.size(),
                            spec.getHypervisorType());
            next(ret);
        }
    }
}
