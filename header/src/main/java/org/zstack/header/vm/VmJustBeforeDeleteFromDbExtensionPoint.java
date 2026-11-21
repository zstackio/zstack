package org.zstack.header.vm;

import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;

/**
 * Created by xing5 on 2017/6/26.
 */
public interface VmJustBeforeDeleteFromDbExtensionPoint {
    void vmJustBeforeDeleteFromDb(VmInstanceInventory inv);

    /**
     * Receives the effective deletion policy before database removal.
     * By default, all policies including DBOnly delegate to the single-argument callback.
     *
     * @param inv the VM being removed from the database
     * @param deletionPolicy the policy controlling external resource deletion
     */
    default void vmJustBeforeDeleteFromDb(VmInstanceInventory inv, VmInstanceDeletionPolicy deletionPolicy) {
        vmJustBeforeDeleteFromDb(inv);
    }
}
