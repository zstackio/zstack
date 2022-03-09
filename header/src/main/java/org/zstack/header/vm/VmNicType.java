package org.zstack.header.vm;

import org.zstack.header.network.l2.VSwitchType;

import java.util.*;

public class VmNicType {
    private static Map<String, VmNicType> types = Collections.synchronizedMap(new HashMap<>());
    private static Map<String, String> vSwitchAndSriovTypes = Collections.synchronizedMap(new HashMap<>());

    private final String typeName;

    public VmNicType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public VmNicType(String typeName, String vSwitchType) {
        this.typeName = typeName;
        types.put(typeName, this);
        vSwitchAndSriovTypes.put(vSwitchType, typeName);
    }

    public VmNicType(String typeName, String vSwitchType, Boolean enableSriov) {
        this.typeName = typeName;
        types.put(typeName, this);
        if (!enableSriov) {
            vSwitchAndSriovTypes.put(vSwitchType, typeName);
        }else{
            vSwitchType += "sriov";
            vSwitchAndSriovTypes.put(vSwitchType, typeName);

        }
    }

    public static VmNicType valueOf(String typeName) {
        VmNicType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VmNicType type: " + typeName + " was not registered by any VmNicFactory");
        }
        return type;
    }

    public static VmNicType valueOf(VSwitchType vSwitchType) {
        String typeName = vSwitchAndSriovTypes.get(vSwitchType.toString());
        return valueOf(typeName);
    }

    public static VmNicType valueOf(VSwitchType vSwitchType, Boolean enableSriov) {
        String typeName = vSwitchAndSriovTypes.get(vSwitchType.toString());
        if (enableSriov) {
            typeName = vSwitchAndSriovTypes.get(vSwitchType + "sriov");
        }
        return valueOf(typeName);
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
