package org.zstack.compute.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by yaoning.li on 2021/2/19.
 */
public interface VmCascadeExtensionPoint {
    ErrorCode preDestroyVm(VmInstanceInventory inv);
}
