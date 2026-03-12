package org.zstack.header.network.l2;

import org.zstack.header.vm.VmNicType;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VSwitchType {
    private static final CLogger logger = Utils.getLogger(VSwitchType.class);
    private static Map<String, VSwitchType> types = Collections.synchronizedMap(new HashMap<String, VSwitchType>());
    private final String typeName;
    private boolean exposed = true;
    private boolean attachToCluster = true;
    private String sdnControllerType = null;
    private boolean useDpdk = false;
    private Map<VmNicType.VmNicSubType, VmNicType> nicTypes = Collections.synchronizedMap(new HashMap<>());

    public VSwitchType(String typeName) {
        this.typeName = typeName;
        if (!types.containsKey(typeName)) {
            types.put(typeName, this);
        }
    }

    public VSwitchType(String typeName, boolean exposed) {
        this(typeName);
        if (!types.containsKey(typeName)) {
            types.put(typeName, this);
        }
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }


    public boolean isAttachToCluster() {
        return attachToCluster;
    }

    public void setAttachToCluster(boolean attachToCluster) {
        this.attachToCluster = attachToCluster;
    }

    public String getSdnControllerType() {
        return sdnControllerType;
    }

    public void setSdnControllerType(String sdnControllerType) {
        this.sdnControllerType = sdnControllerType;
    }

    public boolean isUseDpdk() {
        return useDpdk;
    }

    public void setUseDpdk(boolean useDpdk) {
        this.useDpdk = useDpdk;
    }

    public void addVmNicType(VmNicType.VmNicSubType subType, VmNicType nicType) {
        VmNicType oldNicType = nicTypes.get(subType);
        if (oldNicType != null) {
            if (!oldNicType.toString().equals(nicType.toString())) {
                throw new IllegalArgumentException("duplicated nic type: " + nicType +
                        " subtype " + subType + " for vSwitchType " + typeName + " " +
                        JSONObjectUtil.toJsonString(nicTypes));
            } else {
                /* call addVmNicType duplicated */
                return;
            }
        }
        logger.debug("addVmNicType nic type: " + nicType +
                " subtype " + subType + " for vSwitchType " + typeName);
        nicTypes.put(subType, nicType);
    }

    public VmNicType getVmNicType(VmNicType.VmNicSubType subType) {
        VmNicType nicType = nicTypes.get(subType);
        if (nicType == null) {
            /* for case, enableVHostUser is enabled, but vswitch type is linux bridge  */
            nicType = nicTypes.get(VmNicType.VmNicSubType.NONE);
            if (nicType == null) {
                throw new IllegalArgumentException("unsupported nicSubType " + subType + " for vswitch type " + typeName);
            }
        }

        return nicType;
    }


    public static VSwitchType valueOf(String typeName) {
        VSwitchType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("VSwitchType type: " + typeName + " was not registered by any L2NetworkFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof VSwitchType)) {
            return false;
        }

        VSwitchType type = (VSwitchType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (VSwitchType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
