package org.zstack.appliancevm;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Appliance VM cascade deletion filter extension point - Factory pattern
 * Each implementation handles cascade deletion logic for specific appliance VM type
 * 
 * Created by weiwang on 22/10/2017
 */
public interface ApvmCascadeFilterExtensionPoint {
    /**
     * Filter appliance VMs that need to be cascade deleted
     * 
     * @param applianceVmVOS appliance VM list to process
     * @param action cascade action
     * @param parentIssuer parent resource type
     * @param parentIssuerUuids parent resource UUID list
     * @param toDeleteNics nics to be deleted (output parameter)
     * @param toDeleteIps IPs to be deleted (output parameter)
     * @return filtered VM list to delete
     */
    List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> applianceVmVOS, CascadeAction action,
                                                 String parentIssuer, List<String> parentIssuerUuids,
                                                 List<VmNicInventory> toDeleteNics,
                                                 List<UsedIpInventory> toDeleteIps);
    
    /**
     * Check if this factory supports the specified appliance VM type
     * @return true if supported, false otherwise
     */
    ApplianceVmType getApplianceVmType();
}
