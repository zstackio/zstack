package org.zstack.sdnController;

import java.util.*;

public class SdnControllerType {
    private static Map<String, SdnControllerType> types = Collections.synchronizedMap(new HashMap<String, SdnControllerType>());
    private final String typeName;
    private boolean exposed = true;

    public SdnControllerType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public SdnControllerType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static SdnControllerType valueOf(String typeName) {
        SdnControllerType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("SdnControllerType type: " + typeName + " was not registered by any SdnControllerFactory");
        }
        return type;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof SdnControllerType)) {
            return false;
        }

        SdnControllerType type = (SdnControllerType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (SdnControllerType type : types.values()) {
            if (type.exposed) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
