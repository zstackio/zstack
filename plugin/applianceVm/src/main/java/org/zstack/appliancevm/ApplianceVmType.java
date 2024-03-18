package org.zstack.appliancevm;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmType implements Serializable {
    private static Map<String, ApplianceVmType> types = Collections.synchronizedMap(new HashMap<String, ApplianceVmType>());
    private final String typeName;
    private  boolean needOverlay = false;

    public ApplianceVmType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public ApplianceVmType(String typeName, boolean needOverlay) {
        this.typeName = typeName;
        this.needOverlay = needOverlay;
        types.put(typeName, this);
    }

    public boolean isNeedOverlay() {
        return needOverlay;
    }

    public static ApplianceVmType valueOf(String typeName) {
        ApplianceVmType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("ApplianceVmType type: " + typeName + " was not registered by any one component");
        }
        return type;
    }

    public static List<ApplianceVmType> values() {
        return new ArrayList<>(types.values());
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof ApplianceVmType)) {
            return false;
        }

        ApplianceVmType type = (ApplianceVmType)t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
