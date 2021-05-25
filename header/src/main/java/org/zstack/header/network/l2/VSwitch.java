package org.zstack.header.network.l2;

import java.util.ArrayList;
import java.util.List;

public interface VSwitch {

    VSwitchType linuxBridge = new VSwitchType(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);
    VSwitchType ovsDpdk = new VSwitchType(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK);
    VSwitchType ovsKernel = new VSwitchType(L2NetworkConstant.VSWITCH_TYPE_OVS_KERNEL);

    default List<VSwitchType> getVSwitchTypes() {
        List<VSwitchType> vSwitchTypes = new ArrayList<VSwitchType>();
        vSwitchTypes.add(linuxBridge);
        //vSwitchTypes.add(ovsDpdk);
        //vSwitchTypes.add(ovsKernel);
        return vSwitchTypes;
    }
}
