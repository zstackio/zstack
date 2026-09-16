package org.zstack.header.network.l3;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Internal entry of the IP range deletion flow.
 *
 * <p>{@link APIDeleteIpRangeMsg} converts into this message, so API-only concerns
 * (session, auditing, account checks, API event publishing) stay on the API entry.
 */
public class DeleteIpRangeMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String uuid;
    private String l3NetworkUuid;
    private APIDeleteMessage.DeletionMode deletionMode = APIDeleteMessage.DeletionMode.Permissive;
    private String accountUuid;
    private String operationUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIpRangeUuid() {
        return uuid;
    }

    public APIDeleteMessage.DeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(APIDeleteMessage.DeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public void setOperationUuid(String operationUuid) {
        this.operationUuid = operationUuid;
    }
}
