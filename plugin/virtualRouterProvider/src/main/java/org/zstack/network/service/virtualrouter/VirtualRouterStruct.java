package org.zstack.network.service.virtualrouter;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

/**
 * Created by frank on 8/11/2015.
 */
public class VirtualRouterStruct {
    private L3NetworkInventory l3Network;
    private VirtualRouterOfferingValidator offeringValidator;
    private VirtualRouterVmSelector virtualRouterVmSelector;
    private List<String> inherentSystemTags;
    private List<String> nonInherentSystemTags;
    private boolean notGatewayForGuestL3Network;

    public boolean isNotGatewayForGuestL3Network() {
        return notGatewayForGuestL3Network;
    }

    public void setNotGatewayForGuestL3Network(boolean notGatewayForGuestL3Network) {
        this.notGatewayForGuestL3Network = notGatewayForGuestL3Network;
    }

    public List<String> getNonInherentSystemTags() {
        return nonInherentSystemTags;
    }

    public void setNonInherentSystemTags(List<String> nonInherentSystemTags) {
        this.nonInherentSystemTags = nonInherentSystemTags;
    }

    public List<String> getInherentSystemTags() {
        return inherentSystemTags;
    }

    public void setInherentSystemTags(List<String> inherentSystemTags) {
        this.inherentSystemTags = inherentSystemTags;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }

    public VirtualRouterOfferingValidator getOfferingValidator() {
        return offeringValidator;
    }

    public void setOfferingValidator(VirtualRouterOfferingValidator offeringValidator) {
        this.offeringValidator = offeringValidator;
    }

    public VirtualRouterVmSelector getVirtualRouterVmSelector() {
        return virtualRouterVmSelector;
    }

    public void setVirtualRouterVmSelector(VirtualRouterVmSelector virtualRouterVmSelector) {
        this.virtualRouterVmSelector = virtualRouterVmSelector;
    }
}
