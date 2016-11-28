package org.zstack.header.network.l2;

import java.util.*;

public class L2NetworkType {
    private static Map<String, L2NetworkType> types = Collections.synchronizedMap(new HashMap<String, L2NetworkType>());
    private final String typeName;
    private boolean exposed = true;

    public static boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    public L2NetworkType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public L2NetworkType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public static L2NetworkType valueOf(String typeName) {
        L2NetworkType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("L2NetworkType type: " + typeName + " was not registered by any L2NetworkFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof L2NetworkType)) {
            return false;
        }

        L2NetworkType type = (L2NetworkType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (L2NetworkType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
