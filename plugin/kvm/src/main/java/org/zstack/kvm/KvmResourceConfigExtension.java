package org.zstack.kvm;

import org.zstack.header.vm.ArchiveResourceConfigBundle;
import org.zstack.header.vm.ResourceConfigMemorySnapshotExtensionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.ArrayList;
import java.util.List;

public class KvmResourceConfigExtension implements ResourceConfigMemorySnapshotExtensionPoint {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public List<ArchiveResourceConfigBundle.ResourceConfigBundle> getNeedToArchiveResourceConfig(String resourceUuid) {
        List<ArchiveResourceConfigBundle.ResourceConfigBundle> bundleList = new ArrayList<>();
        ArchiveResourceConfigBundle.ResourceConfigBundle bundle = new ArchiveResourceConfigBundle.ResourceConfigBundle();
        bundle.setResourceUuid(resourceUuid);
        bundle.setIdentity(KVMGlobalConfig.NESTED_VIRTUALIZATION.getIdentity());
        bundle.setValue(rcf.getResourceConfigValue(KVMGlobalConfig.NESTED_VIRTUALIZATION, resourceUuid, String.class));
        bundleList.add(bundle);
        return bundleList;
    }
}
