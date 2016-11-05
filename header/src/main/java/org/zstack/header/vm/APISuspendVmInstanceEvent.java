package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by luchkun on 10/29/16.
 */
public class APISuspendVmInstanceEvent extends APIEvent{

    private VmInstanceInventory inventory;

    public APISuspendVmInstanceEvent() {
        super(null);
    }

    public APISuspendVmInstanceEvent(String apiId){
        super(apiId);
    }

    public VmInstanceInventory getInventory(){
        return inventory;
    }
    public void setInventory(VmInstanceInventory inventory){
        this.inventory = inventory;
    }
}
