package org.zstack.header.vm;

import org.zstack.header.vm.devices.VmDeviceMigrationCapability;

import java.util.*;

public class VmNicType {
    public enum VmNicSubType {
        NONE,
        SRIOV,
        VHOSTUSER,
    }
    private static Map<String, VmNicType> types = Collections.synchronizedMap(new HashMap<>());

    private final String typeName;
    private final VmDeviceMigrationCapability migrationCapability;
    private boolean hasAddon = false;
    private boolean useSRIOV = false;

    public VmNicType(String typeName, VmDeviceMigrationCapability migrationCapability) {
        this.typeName = typeName;
        this.migrationCapability = Objects.requireNonNull(migrationCapability, "NIC migration capability is required");
        types.put(typeName, this);
    }

    public VmDeviceMigrationCapability getMigrationCapability() {
        return migrationCapability;
    }

    public static VmNicType valueOf(String typeName) {
        VmNicType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VmNicType type: " + typeName + " was not registered by any VmNicFactory");
        }
        return type;
    }

    public boolean isHasAddon() {
        return hasAddon;
    }

    public void setHasAddon(boolean hasAddon) {
        this.hasAddon = hasAddon;
    }

    public boolean isUseSRIOV() {
        return useSRIOV;
    }

    public void setUseSRIOV(boolean useSRIOV) {
        this.useSRIOV = useSRIOV;
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
