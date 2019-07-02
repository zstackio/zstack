package org.zstack.appliancevm;

/**
 * Created by shixin.ruan on 2019/06/11
 */
public interface ApplianceVmHaExtensionPoint {
    void attachVirtualRouterToHaGroup(String vrUuid, String haGroupUuid);
    void detachVirtualRouterFromHaGroup(String vrUuid, String haGroupUuid);
    void createApplianceVmHaSpec(ApplianceVmSpec aspec, String offeringUuid, String haUuid);
}
