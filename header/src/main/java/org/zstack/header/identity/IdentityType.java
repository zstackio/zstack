package org.zstack.header.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IdentityType {
    private static Map<String, IdentityType> types = Collections.synchronizedMap(new HashMap<String, IdentityType>());
    private final String typeName;

    public IdentityType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static IdentityType valueOf(String typeName) {
        IdentityType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Identity type: " + typeName + " was not registered by any HypervisorFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof IdentityType)) {
            return false;
        }

        IdentityType type = (IdentityType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
