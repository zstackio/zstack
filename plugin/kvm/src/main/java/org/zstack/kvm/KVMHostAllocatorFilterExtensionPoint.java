package org.zstack.kvm;

import org.zstack.core.Platform;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class KVMHostAllocatorFilterExtensionPoint implements HostAllocatorFilterExtensionPoint {
    private CLogger logger = Utils.getLogger(KVMHostAllocatorFilterExtensionPoint.class);

    @Override
    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec) {
        if (spec.getHypervisorType() == null || !spec.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return candidates;
        }

        if (!VmInstanceConstant.VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            return candidates;
        }

        // vm can only live migrate to hosts that have the same version of qemu/libvirt
        List<HostVO> result = new ArrayList<>();
        String srcHostUuid = spec.getVmInstance().getHostUuid();
        String srcQemuVer = KVMSystemTags.QEMU_IMG_VERSION.getTokenByResourceUuid(srcHostUuid, KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
        String srcLibvirtVer = KVMSystemTags.LIBVIRT_VERSION.getTokenByResourceUuid(srcHostUuid, KVMSystemTags.LIBVIRT_VERSION_TOKEN);
        String srcCpuModelName = KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(srcHostUuid, KVMSystemTags.CPU_MODEL_NAME_TOKEN);

        // nothing to check
        if (srcQemuVer == null && srcLibvirtVer == null && srcCpuModelName == null) {
            return candidates;
        }

        for (HostVO host : candidates) {
            String dstQemuVer = KVMSystemTags.QEMU_IMG_VERSION.getTokenByResourceUuid(host.getUuid(), KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
            String dstLibvirtVer = KVMSystemTags.LIBVIRT_VERSION.getTokenByResourceUuid(host.getUuid(), KVMSystemTags.LIBVIRT_VERSION_TOKEN);
            String dstCpuModelName = KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(host.getUuid(), KVMSystemTags.CPU_MODEL_NAME_TOKEN);
            if ((srcQemuVer == null || srcQemuVer.equals(dstQemuVer))
                    && (srcLibvirtVer == null || srcLibvirtVer.equals(dstLibvirtVer))) {
                result.add(host);
            } else {
                logger.debug(String.format("cannot migrate vm[uuid:%s] to host[uuid:%s] because qemu/libvirt version not match",
                        spec.getVmInstance().getUuid(), host.getUuid()));
            }

            if (KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME.value(Boolean.class) && srcCpuModelName != null && !srcCpuModelName.equals(dstCpuModelName)) {
                result.remove(host);
                logger.debug(String.format("cannot migrate vm[uuid:%s] to host[uuid:%s] because cpu model name not match",
                        spec.getVmInstance().getUuid(), host.getUuid()));
            }
        }

        return result;
    }

    @Override
    public String filterErrorReason() {
        return Platform.i18n("cannot adapt version for the bellow rpm: livirt / qemu / cpumodel");
    }
}
