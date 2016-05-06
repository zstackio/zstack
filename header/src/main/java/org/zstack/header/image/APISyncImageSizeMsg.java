package org.zstack.header.image;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by xing5 on 2016/5/6.
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
public class APISyncImageSizeMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
}
