package org.zstack.sdk;

import org.zstack.sdk.ModelEvalServiceInstanceGroupInventory;

public class DeployModelEvalServiceResult {
    public ModelEvalServiceInstanceGroupInventory inventory;
    public void setInventory(ModelEvalServiceInstanceGroupInventory inventory) {
        this.inventory = inventory;
    }
    public ModelEvalServiceInstanceGroupInventory getInventory() {
        return this.inventory;
    }

    public java.util.List tasks;
    public void setTasks(java.util.List tasks) {
        this.tasks = tasks;
    }
    public java.util.List getTasks() {
        return this.tasks;
    }

}
