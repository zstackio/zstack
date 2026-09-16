package org.zstack.header.network.l3;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Internal entry of the L3Network deletion flow.
 *
 * <p>{@link APIDeleteL3NetworkMsg} converts into this message, so API-only concerns
 * (session, auditing, account checks, API event publishing) stay on the API entry.
 */
public class DeleteL3NetworkMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String uuid;
    private APIDeleteMessage.DeletionMode deletionMode = APIDeleteMessage.DeletionMode.Permissive;
    private String accountUuid;
    private String operationUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    @Override
    public String getL3NetworkUuid() {
        return uuid;
    }
}
