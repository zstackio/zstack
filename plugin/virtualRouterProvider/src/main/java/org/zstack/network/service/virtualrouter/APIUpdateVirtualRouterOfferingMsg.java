package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.APIUpdateInstanceOfferingMsg;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 7/31/2015.
 */
@Action(category = VirtualRouterConstant.ACTION_CATEGORY)
public class APIUpdateVirtualRouterOfferingMsg extends APIUpdateInstanceOfferingMsg {
    private Boolean isDefault;
    @APIParam(resourceType = ImageVO.class, checkAccount = true, required = false)
    private String imageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
