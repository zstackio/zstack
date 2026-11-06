package org.zstack.rest;

import org.zstack.header.message.APIEvent;

import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/12/8.
 */
public class AsyncRestQueryResult {
    private String uuid;
    private AsyncRestState state;
    private APIEvent result;
    private String ownerAccountUuid;
    private String ownerUserUuid;

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public void setOwnerAccountUuid(String ownerAccountUuid) {
        this.ownerAccountUuid = ownerAccountUuid;
    }

    public String getOwnerUserUuid() {
        return ownerUserUuid;
    }

    public void setOwnerUserUuid(String ownerUserUuid) {
        this.ownerUserUuid = ownerUserUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public AsyncRestState getState() {
        return state;
    }

    public void setState(AsyncRestState state) {
        this.state = state;
    }

    public APIEvent getResult() {
        return result;
    }

    public void setResult(APIEvent result) {
        this.result = result;
    }
}
