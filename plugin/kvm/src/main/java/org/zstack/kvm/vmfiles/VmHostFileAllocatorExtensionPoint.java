package org.zstack.kvm.vmfiles;

import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.db.Q;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;

public class VmHostFileAllocatorExtensionPoint implements HostAllocatorFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmHostFileAllocatorExtensionPoint.class);

    @Override
    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec) {
        if (spec == null) {
            return candidates;
        }

        String vmOperation = spec.getVmOperation();
        if (vmOperation == null || !VmInstanceConstant.VmOperation.Start.toString().equals(vmOperation)) {
            return candidates;
        }

        if (spec.getVmInstance() == null) {
            return candidates;
        }

        String vmUuid = spec.getVmInstance().getUuid();
        String lastHostUuid = spec.getVmInstance().getLastHostUuid();

        List<VmHostFileType> files = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, lastHostUuid)
                .select(VmHostFileVO_.type)
                .notNull(VmHostFileVO_.changeDate)
                .listValues();
        if (files.isEmpty()) {
            return candidates;
        }

        String types = files.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        logger.debug(String.format("only allow VM[uuid:%s] start on last host[uuid:%s], unsynchronized %s VM host files exist",
                vmUuid, lastHostUuid, types));
        return candidates.stream()
                .filter(c -> Objects.equals(lastHostUuid, c.getUuid()))
                .collect(Collectors.toList());
    }

    @Override
    public String filterErrorReason() {
        return i18n("only allowed start on last host: unsynchronized VM host files exist");
    }
}
