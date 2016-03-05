package org.zstack.header.console;

import org.zstack.header.message.APIEvent;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIRequestConsoleAccessEvent extends APIEvent {
    private ConsoleInventory inventory;

    public APIRequestConsoleAccessEvent() {
        super(null);
    }

    public APIRequestConsoleAccessEvent(String apiId) {
        super(apiId);
    }

    public ConsoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(ConsoleInventory inventory) {
        this.inventory = inventory;
    }
}
