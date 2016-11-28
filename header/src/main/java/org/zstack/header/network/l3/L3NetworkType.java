package org.zstack.header.network.l3;

import java.util.*;

public class L3NetworkType {
    private static Map<String, L3NetworkType> types = Collections.synchronizedMap(new HashMap<String, L3NetworkType>());
    private final String typeName;
    private boolean exposed = true;

    public L3NetworkType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public L3NetworkType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public static L3NetworkType valueOf(String typeName) {
        L3NetworkType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("L3NetworkType type: " + typeName + " was not registered by any L3NetworkFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof L3NetworkType)) {
            return false;
        }

        L3NetworkType type = (L3NetworkType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (L3NetworkType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
