package org.zstack.header.physicalserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PhysicalServerRoleType {
    private static final Map<String, PhysicalServerRoleType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;

    public PhysicalServerRoleType(String typeName) {
        if (types.containsKey(typeName)) {
            throw new IllegalArgumentException(String.format("duplicate PhysicalServerRoleType[%s]", typeName));
        }
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static PhysicalServerRoleType valueOf(String typeName) {
        PhysicalServerRoleType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException(String.format("PhysicalServerRoleType[%s] is not registered", typeName));
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhysicalServerRoleType)) {
            return false;
        }
        return typeName.equals(other.toString());
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
