package org.zstack.header.vm;

import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.VSwitchType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VmNicType {
    private static Map<String, VmNicType> types = Collections.synchronizedMap(new HashMap<>());
//    private static Map<VSwitchType, VmNicType> typesByVSwitchType = Collections.synchronizedMap(new HashMap<>());
//    private static Map<Boolean, VmNicType> typesByEnableSriov = Collections.synchronizedMap(new HashMap<>());

    private final String typeName;
//    private final VSwitchType vSwitchType;
//    private final Boolean enableSriov;

    public VmNicType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public VmNicType(Boolean enableSriov, VSwitchType vSwitchType) {
//        this.vSwitchType = vSwitchType;
//        this.enableSriov = enableSriov;
        String typeName = "VNIC";
        if (enableSriov) {
            typeName = "VF";
        } else if (vSwitchType.equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            typeName = "vDPA";
        }
        this.typeName = typeName;
        types.put(typeName, this);
//        typesByVSwitchType.put(vSwitchType, this);
//        typesByVSwitchType.put(enableSriov, this);
    }

    public static VmNicType valueOf(String typeName) {
        VmNicType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VmNicType type: " + typeName + " was not registered by any VmNicFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof VmNicType)) {
            return false;
        }

        VmNicType type = (VmNicType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
