package org.zstack.core.keystore;

import org.zstack.header.core.keystore.KeystoreVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by miao on 16-8-15.
 */
@Action(category = KeystoreConstant.ACTION_CATEGORY)
public class APIDeleteKeystoreMsg extends APIDeleteMessage {
    @APIParam(resourceType = KeystoreVO.class)
    private String uuid;

    public APIDeleteKeystoreMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
