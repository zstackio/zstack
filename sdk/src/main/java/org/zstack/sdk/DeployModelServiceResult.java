package org.zstack.sdk;

import org.zstack.sdk.ModelServiceInstanceGroupInventory;

public class DeployModelServiceResult {
    public ModelServiceInstanceGroupInventory inventory;
    public void setInventory(ModelServiceInstanceGroupInventory inventory) {
        this.inventory = inventory;
    }
    public ModelServiceInstanceGroupInventory getInventory() {
        return this.inventory;
    }

}
