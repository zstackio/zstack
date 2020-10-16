package org.zstack.header.storage.backup;

import org.zstack.header.storage.primary.PrimaryStorageType;

import java.util.*;
import java.util.stream.Collectors;


public class BackupStorageType {
    private static Map<String, BackupStorageType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;
    private Set<String> supportedSchemes;
    private boolean exposed = true;
    private int order;
    private BackupStorageFindRelatedPrimaryStorage primaryStorageFinder;
    private List<PrimaryStorageType> relatedPrimaryStorageTypes;
    
    public static BackupStorageType createIfAbsent(String typeName) {
        BackupStorageType type;
        synchronized (BackupStorageType.class) {
            type = types.get(typeName);
            if (type != null) {
                return type;
            }
            types.put(typeName, type = new BackupStorageType(typeName));
        }
        return type;
    }

    private BackupStorageType(String typeName) {
        this.typeName = typeName;
    }

    public Set<String> getSupportedSchemes() {
        return supportedSchemes;
    }
    
    public void setSupportedSchemes(Set<String> supportedSchemes) {
        this.supportedSchemes = supportedSchemes;
    }
    
    public static BackupStorageType valueOf(String typeName) throws IllegalArgumentException {
        BackupStorageType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("BackupStorageType type: " + typeName + " was not registered");
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
        if (!(t instanceof BackupStorageType)) {
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
        return types.values().stream()
            .filter(BackupStorageType::isExposed)
            .sorted(Comparator.comparingInt(BackupStorageType::getOrder))
            .map(BackupStorageType::toString)
            .collect(Collectors.toList());
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
    
    public void setRelatedPrimaryStorageTypes(List<PrimaryStorageType> relatedPrimaryStorageTypes) {
        this.relatedPrimaryStorageTypes = relatedPrimaryStorageTypes;
    }
    
    public List<PrimaryStorageType> getRelatedPrimaryStorageTypes() {
        return relatedPrimaryStorageTypes;
    }
    
    public static List<String> findRelatedPrimaryStorageTypes(String backupStorageType) throws IllegalArgumentException {
        return BackupStorageType.valueOf(backupStorageType)
            .getRelatedPrimaryStorageTypes()
            .stream()
            .map(PrimaryStorageType::toString)
            .collect(Collectors.toList());
    }
}
