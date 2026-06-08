package org.zstack.kvm.hypervisor;

import org.zstack.kvm.hypervisor.datatype.HostOsCategoryVO;

import java.util.List;

import static org.zstack.kvm.hypervisor.HypervisorMetadataCollector.HypervisorMetadataDefinition;

public interface KvmHypervisorMetadataStore {
    boolean refresh(List<HypervisorMetadataDefinition> definitions);

    HostExpectedHypervisorMetadata find(String architecture, String osReleaseVersion, String hypervisor);

    List<HostOsCategoryVO> listForCompatibility();

    boolean isAvailable();
}
