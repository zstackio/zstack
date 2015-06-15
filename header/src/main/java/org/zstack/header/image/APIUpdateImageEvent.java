package org.zstack.header.image;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/14/2015.
 */
public class APIUpdateImageEvent extends APIEvent {
    private ImageInventory inventory;

    public APIUpdateImageEvent() {
    }

    public APIUpdateImageEvent(String apiId) {
        super(apiId);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
