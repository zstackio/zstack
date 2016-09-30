package org.zstack.appliancevm;

import org.zstack.header.vm.StartNewCreatedVmInstanceMsg;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class StartNewCreatedApplianceVmMsg extends StartNewCreatedVmInstanceMsg {
    private ApplianceVmSpec applianceVmSpec;

    public ApplianceVmSpec getApplianceVmSpec() {
        return applianceVmSpec;
    }

    public void setApplianceVmSpec(ApplianceVmSpec applianceVmSpec) {
        this.applianceVmSpec = applianceVmSpec;
    }
}
