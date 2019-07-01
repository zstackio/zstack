package org.zstack.appliancevm;


/**
 * Created by shixin on 2019/06/27.
 */
public interface ApplianceVmSyncConfigToHaGroupExtensionPoint {
    void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid);
    void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid);
}
