package org.zstack.header.storage.primary;

import org.zstack.header.storage.backup.BackupStorageType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PrimaryStorageType {
    private static Map<String, PrimaryStorageType> types = Collections.synchronizedMap(new HashMap<>());
    private final String typeName;
    private boolean exposed = true;
    private boolean supportHeartbeatFile;
    protected boolean supportVmLiveMigration = true;
    protected Function<Boolean, Boolean> supportVmLiveMigrationFunction = null;
    private boolean supportVolumeMigration;
    private boolean supportVolumeMigrationInCurrentPrimaryStorage;
    private boolean supportVolumeMigrationToOtherPrimaryStorage;
    private boolean supportSharedVolume;
    private int order;
    private PrimaryStorageFindBackupStorage primaryStorageFindBackupStorage;
    private List<BackupStorageType> relatedBackupStorageTypes;
    
    public static PrimaryStorageType createIfAbsent(String typeName) {
        PrimaryStorageType type;
        synchronized (PrimaryStorageType.class) {
            type = types.get(typeName);
            if (type != null) {
                return type;
            }
            types.put(typeName, type = new PrimaryStorageType(typeName));
        }
        return type;
    }
    
    public boolean isSupportSharedVolume() {
        return supportSharedVolume;
    }

    public void setSupportSharedVolume(boolean supportSharedVolume) {
        this.supportSharedVolume = supportSharedVolume;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    private PrimaryStorageType(String typeName) {
        this.typeName = typeName;
    }

    public static PrimaryStorageType valueOf(String typeName) throws IllegalArgumentException {
        PrimaryStorageType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("PrimaryStorageType type: " + typeName + " was not registered");
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
        if (!(t instanceof PrimaryStorageType)) {
            return false;
        }

        PrimaryStorageType type = (PrimaryStorageType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    private static List<PrimaryStorageType> getExposedTypes() {
        return types.values().stream()
            .filter(PrimaryStorageType::isExposed)
            .sorted(Comparator.comparingInt(PrimaryStorageType::getOrder))
            .collect(Collectors.toList());
    }

    public static List<String> getSupportSharedVolumePSTypeNames() {
        List<PrimaryStorageType> exposedTypes = getExposedTypes();
        return exposedTypes.stream()
                .filter(PrimaryStorageType::isSupportSharedVolume)
                .map(PrimaryStorageType::toString)
                .collect(Collectors.toList());
    }

    public static List<String> getAllTypeNames() {
        return getExposedTypes().stream()
            .map(PrimaryStorageType::toString)
            .collect(Collectors.toList());
    }

    public static List<PrimaryStorageType> getAllTypes() {
        return getExposedTypes();
    }

    public boolean isSupportHeartbeatFile() {
        return supportHeartbeatFile;
    }

    public void setSupportHeartbeatFile(boolean supportHeartbeatFile) {
        this.supportHeartbeatFile = supportHeartbeatFile;
    }

    public boolean isSupportVmLiveMigration() {
        if (supportVmLiveMigrationFunction != null) {
            return supportVmLiveMigrationFunction.apply(supportVmLiveMigration);
        }
        return supportVmLiveMigration;
    }

    public void setSupportVmLiveMigration(boolean supportVmLiveMigration) {
        this.supportVmLiveMigration = supportVmLiveMigration;
    }

    public boolean isSupportVolumeMigration() {
        return supportVolumeMigration;
    }

    public void setSupportVolumeMigration(boolean supportVolumeMigration) {
        this.supportVolumeMigration = supportVolumeMigration;
    }
    
    public void setSupportVmLiveMigrationFunction(Function<Boolean, Boolean> supportVmLiveMigrationFunction) {
        this.supportVmLiveMigrationFunction = supportVmLiveMigrationFunction;
    }
    
    public boolean isSupportVolumeMigrationInCurrentPrimaryStorage() {
        return supportVolumeMigrationInCurrentPrimaryStorage;
    }

    public void setSupportVolumeMigrationInCurrentPrimaryStorage(boolean supportVolumeMigrationInCurrentPrimaryStorage) {
        this.supportVolumeMigrationInCurrentPrimaryStorage = supportVolumeMigrationInCurrentPrimaryStorage;
    }

    public boolean isSupportVolumeMigrationToOtherPrimaryStorage() {
        return supportVolumeMigrationToOtherPrimaryStorage;
    }

    public void setSupportVolumeMigrationToOtherPrimaryStorage(boolean supportVolumeMigrationToOtherPrimaryStorage) {
        this.supportVolumeMigrationToOtherPrimaryStorage = supportVolumeMigrationToOtherPrimaryStorage;
    }

    public PrimaryStorageFindBackupStorage getPrimaryStorageFindBackupStorage() {
        return primaryStorageFindBackupStorage;
    }

    public void setPrimaryStorageFindBackupStorage(PrimaryStorageFindBackupStorage primaryStorageFindBackupStorage) {
        this.primaryStorageFindBackupStorage = primaryStorageFindBackupStorage;
    }

    public List<String> findBackupStorage(String psUuid) {
        return primaryStorageFindBackupStorage.findBackupStorage(psUuid);
    }
    
    public void setRelatedBackupStorageTypes(List<BackupStorageType> relatedBackupStorageTypes) {
        this.relatedBackupStorageTypes = relatedBackupStorageTypes;
    }
    
    public List<BackupStorageType> getRelatedBackupStorageTypes() {
        return relatedBackupStorageTypes;
    }
    
    public static List<String> findRelatedBackupStorageTypes(String primaryStorageType) throws IllegalArgumentException {
        return PrimaryStorageType.valueOf(primaryStorageType)
            .getRelatedBackupStorageTypes()
            .stream()
            .map(BackupStorageType::toString)
            .collect(Collectors.toList());
    }
}
