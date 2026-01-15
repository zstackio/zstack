package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

@RestResponse(fieldsTo = {"all"})
public class APITakeoverPrimaryStorageEvent extends APIEvent {
    private PrimaryStorageInventory inventory;

    private ReconnectResult reconnectResult;

    private String reconnectError;

    public APITakeoverPrimaryStorageEvent() {
    }

    public APITakeoverPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public ReconnectResult getReconnectResult() {
        return reconnectResult;
    }

    public void setReconnectResult(ReconnectResult reconnectResult) {
        this.reconnectResult = reconnectResult;
    }

    public String getReconnectError() {
        return reconnectError;
    }

    public void setReconnectError(String reconnectError) {
        this.reconnectError = reconnectError;
    }

    public static APITakeoverPrimaryStorageEvent __example__() {
        APITakeoverPrimaryStorageEvent event = new APITakeoverPrimaryStorageEvent();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("SharedBlock");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));

        event.setInventory(ps);
        event.setReconnectResult(ReconnectResult.SUCCESS);
        return event;
    }
}
