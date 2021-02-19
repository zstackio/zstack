package org.zstack.compute.vm;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by yaoning.li on 2021/2/19.
 */
public class VmCascadeProtectionExtensionPoint implements VmCascadeExtensionPoint {
    @Override
    public ErrorCode preDestroyVm(VmInstanceInventory inv) {
        String status = VmGlobalConfig.CASCADE_ALLOWS_VM_STATUS.value(String.class);
        if (status.equals(VmGlobalConfig.CASCADE_ALLOWS_VM_STATUS.defaultValue(String.class))) {
            return null;
        }

        if (!Arrays.asList(status.split(",")).contains(inv.getState())) {
            return operr("vm[%s] status is not in %s", inv.getUuid(), status);
        }

        return null;
    }
}
