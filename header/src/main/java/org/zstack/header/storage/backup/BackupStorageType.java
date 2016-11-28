package org.zstack.header.storage.backup;

import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.*;


public class BackupStorageType {
    private static Map<String, BackupStorageType> types = Collections.synchronizedMap(new HashMap<String, BackupStorageType>());
    private final String typeName;
    private final Set<String> supportedSchemes;
    private boolean exposed = true;
    private int order;
    private BackupStorageFindRelatedPrimaryStorage primaryStorageFinder;

    public BackupStorageType(String typeName, String... protocols) {
        this.typeName = typeName;
        supportedSchemes = new HashSet<String>(protocols.length);
        Collections.addAll(supportedSchemes, protocols);
        types.put(typeName, this);
    }

    public BackupStorageType(String typeName, boolean expose, String... protocols) {
        this(typeName);
        this.exposed = expose;
    }

    public Set<String> getSupportedSchemes() {
        return supportedSchemes;
    }

    public static BackupStorageType valueOf(String typeName) {
        BackupStorageType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("BackupStorageType type: " + typeName + " was not registered by any BackupStorageFactory");
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
        if (t == null || !(t instanceof BackupStorageType)) {
            return false;
        }

        BackupStorageType type = (BackupStorageType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static List<String> getAllTypeNames() {
        List<BackupStorageType> exposedTypes = new ArrayList<BackupStorageType>();
        for (BackupStorageType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type);
            }
        }

        Collections.sort(exposedTypes, new Comparator<BackupStorageType>() {
            @Override
            public int compare(BackupStorageType o1, BackupStorageType o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });

        return CollectionUtils.transformToList(exposedTypes, new Function<String, BackupStorageType>() {
            @Override
            public String call(BackupStorageType arg) {
                return arg.toString();
            }
        });
    }

    public List<String> findRelatedPrimaryStorage(String bsUuid) {
        return primaryStorageFinder == null ? null : primaryStorageFinder.findRelatedPrimaryStorage(bsUuid);
    }

    public BackupStorageFindRelatedPrimaryStorage getPrimaryStorageFinder() {
        return primaryStorageFinder;
    }

    public void setPrimaryStorageFinder(BackupStorageFindRelatedPrimaryStorage primaryStorageFinder) {
        this.primaryStorageFinder = primaryStorageFinder;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
