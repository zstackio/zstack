package org.zstack.storage.primary.iscsi;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.VmReleaseResourceExtensionPoint;

/**
 * Created by frank on 6/9/2015.
 */
public class IscsiFileSystemPrimaryStorageTargetExtension implements VmReleaseResourceExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {

    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {

    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {

    }
}
