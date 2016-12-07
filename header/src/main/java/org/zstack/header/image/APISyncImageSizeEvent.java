package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/5/6.
 */
@RestResponse(allTo = "inventory")
public class APISyncImageSizeEvent extends APIEvent {
    private ImageInventory inventory;

    public APISyncImageSizeEvent() {
    }

    public APISyncImageSizeEvent(String apiId) {
        super(apiId);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
