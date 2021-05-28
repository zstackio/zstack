package org.zstack.header.storage.cdp;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;

@RestRequest(
        path = "/cdp-backup-storage/policy",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteCdpPolicyMsg.class,
        parameterName = "params"
)
@TagResourceType(BackupStorageVO.class)
public class APIDeleteCdpPolicyMsg extends APIDeleteMessage {

    private String uuid;

    public APIDeleteCdpPolicyMsg() {
    }

    public APIDeleteCdpPolicyMsg(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBackupStorageUuid() {
        return getUuid();
    }

    public static APIDeleteCdpPolicyMsg __example__() {
        APIDeleteCdpPolicyMsg msg = new APIDeleteCdpPolicyMsg();

        msg.setUuid(uuid());

        return msg;
    }

}