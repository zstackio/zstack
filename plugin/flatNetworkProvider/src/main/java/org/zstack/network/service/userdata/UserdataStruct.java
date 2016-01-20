package org.zstack.network.service.userdata;

import org.zstack.header.vm.VmInstanceSpec;

/**
 * Created by frank on 10/13/2015.
 */
public class UserdataStruct {
    private VmInstanceSpec vmSpec;
    private String userdata;
    private String l3NetworkUuid;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public VmInstanceSpec getVmSpec() {
        return vmSpec;
    }

    public void setVmSpec(VmInstanceSpec vmSpec) {
        this.vmSpec = vmSpec;
    }

    public String getUserdata() {
        return userdata;
    }

    public void setUserdata(String userdata) {
        this.userdata = userdata;
    }
}
