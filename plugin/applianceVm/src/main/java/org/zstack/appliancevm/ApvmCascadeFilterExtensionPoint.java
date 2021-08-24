package org.zstack.appliancevm;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Created by weiwang on 22/10/2017
 */
public interface ApvmCascadeFilterExtensionPoint {
    List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> applianceVmVOS, CascadeAction action, String parentIssuer, List<String> parentIssuerUuids, List<VmNicInventory> toDeleteNics);
}
