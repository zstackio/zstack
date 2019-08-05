package org.zstack.header.storage.primary;

import java.util.*;

public class PrimaryStorageVendor {
    private static Map<String, PrimaryStorageVendor> types = Collections.synchronizedMap(new HashMap<String, PrimaryStorageVendor>());
    private final String typeName;

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public PrimaryStorageVendor(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static PrimaryStorageVendor valueOf(String typeName) {
        PrimaryStorageVendor type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("PrimaryStorageVendor: " + typeName + " was not registered by any PrimaryStorageLicenseInfoFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof PrimaryStorageVendor)) {
            return false;
        }

        PrimaryStorageVendor type = (PrimaryStorageVendor) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}