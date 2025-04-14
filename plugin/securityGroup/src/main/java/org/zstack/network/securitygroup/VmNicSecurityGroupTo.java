package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.List;

public class VmNicSecurityGroupTo {
    List<VmNicSecurityTO> vmNics = new ArrayList<>();
    List<SecurityGroupTo> groups = new ArrayList<>();

    public List<VmNicSecurityTO> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<VmNicSecurityTO> vmNics) {
        this.vmNics = vmNics;
    }

    public List<SecurityGroupTo> getGroups() {
        return groups;
    }

    public void setGroups(List<SecurityGroupTo> groups) {
        this.groups = groups;
    }
}
