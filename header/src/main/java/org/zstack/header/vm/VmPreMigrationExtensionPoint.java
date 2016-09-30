package org.zstack.header.vm;

/**
 * Created by frank on 1/2/2016.
 */
public interface VmPreMigrationExtensionPoint {
    void preVmMigration(VmInstanceInventory vm);
}
