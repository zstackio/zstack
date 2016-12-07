package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@RestRequest(
        path = "/primary-storage",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddPrimaryStorageEvent.class
)
public abstract class APIAddPrimaryStorageMsg extends APICreateMessage {
    /**
     * @desc depending on primary storage, formats of url are various. For example, NFS primary storage
     * uses *server_ip:mount_path* as url. Max length of 2048 characters
     */
    @APIParam(maxLength = 2048)
    private String url;
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    /**
     * @ignore
     */
    private String type;
    /**
     * @desc uuid of zone where this primary storage is being created. See :ref:`ZoneInventory`
     */
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    public APIAddPrimaryStorageMsg() {
    }

    public APIAddPrimaryStorageMsg(String url, String type) {
        this.url = url;
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
