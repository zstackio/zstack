package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.APIUpdateInstanceOfferingMsg;
import org.zstack.header.identity.Action;

/**
 * Created by frank on 7/31/2015.
 */
@Action(category = VirtualRouterConstant.ACTION_CATEGORY)
public class APIUpdateVirtualRouterOfferingMsg extends APIUpdateInstanceOfferingMsg {
    private Boolean isDefault;

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
