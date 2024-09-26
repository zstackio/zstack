package org.zstack.sdk;

import org.zstack.sdk.ModelServiceInstanceGroupInventory;
import org.zstack.sdk.ApplicationDevelopmentServiceInventory;

public class DeployAppDevelopmentServiceResult {
    public ModelServiceInstanceGroupInventory inventory;
    public void setInventory(ModelServiceInstanceGroupInventory inventory) {
        this.inventory = inventory;
    }
    public ModelServiceInstanceGroupInventory getInventory() {
        return this.inventory;
    }

    public ApplicationDevelopmentServiceInventory app;
    public void setApp(ApplicationDevelopmentServiceInventory app) {
        this.app = app;
    }
    public ApplicationDevelopmentServiceInventory getApp() {
        return this.app;
    }

}
