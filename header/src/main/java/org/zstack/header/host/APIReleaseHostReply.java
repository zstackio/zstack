package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"success=actualSuccess", "inventory", "error=actualError"})
public class APIReleaseHostReply extends APIReply {
    private HostInventory inventory;
    private Boolean actualSuccess;
    private ErrorCode actualError;

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    public Boolean getActualSuccess() {
        return super.isSuccess();
    }

    public void setActualSuccess(Boolean actualSuccess) {
        this.actualSuccess = actualSuccess;
    }

    public ErrorCode getActualError() {
        return super.getError();
    }

    public void setActualError(ErrorCode actualError) {
        this.actualError = actualError;
    }

    public static APIReleaseHostReply __example__() {
        APIReleaseHostReply reply = new APIReleaseHostReply();
        HostInventory inv = new HostInventory();
        inv.setName("host-1");
        inv.setUuid(uuid());
        inv.setState(HostState.Enabled.toString());
        inv.setStatus(HostStatus.Connected.toString());
        reply.setInventory(inv);
        return reply;
    }
}