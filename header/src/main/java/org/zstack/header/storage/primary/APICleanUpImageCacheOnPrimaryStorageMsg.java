package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/7/21.
 */
@RestRequest(
        path = "/primary-storage/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICleanUpImageCacheOnPrimaryStorageEvent.class
)
public class APICleanUpImageCacheOnPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
}
