package org.zstack.header.storage.primary;

import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.*;
import java.util.stream.Collectors;


public class PrimaryStorageType {
    private static Map<String, PrimaryStorageType> types = Collections.synchronizedMap(new HashMap<String, PrimaryStorageType>());
    private final String typeName;
    private boolean exposed = true;
    private boolean supportHeartbeatFile;
    protected boolean supportVmLiveMigration = true;
    private boolean supportVolumeMigration;
    private boolean supportVolumeMigrationInCurrentPrimaryStorage;
    private boolean supportVolumeMigrationToOtherPrimaryStorage;
    private boolean supportConfigVolumeProvisionStrategy;
    private boolean supportSharedVolume;
    private int order;
    private PrimaryStorageFindBackupStorage primaryStorageFindBackupStorage;
    private  boolean supportCreateVolumeSnapshotCheckCapacity = true;

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

    public PrimaryStorageType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public PrimaryStorageType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public static PrimaryStorageType valueOf(String typeName) {
        PrimaryStorageType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("PrimaryStorageType type: " + typeName + " was not registered by any PrimaryStorageFactory");
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
        if (t == null || !(t instanceof PrimaryStorageType)) {
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
        List<PrimaryStorageType> exposedTypes = new ArrayList<PrimaryStorageType>();
        for (PrimaryStorageType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type);
            }
        }

        Collections.sort(exposedTypes, new Comparator<PrimaryStorageType>() {
            @Override
            public int compare(PrimaryStorageType o1, PrimaryStorageType o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });

        return exposedTypes;
    }

    public static List<String> getSupportSharedVolumePSTypeNames() {
        List<PrimaryStorageType> exposedTypes = getExposedTypes();
        return exposedTypes.stream()
                .filter(PrimaryStorageType::isSupportSharedVolume)
                .map(PrimaryStorageType::toString)
                .collect(Collectors.toList());
    }

    public static List<String> getAllTypeNames() {
        List<PrimaryStorageType> exposedTypes = getExposedTypes();

        return CollectionUtils.transformToList(exposedTypes, new Function<String, PrimaryStorageType>() {
            @Override
            public String call(PrimaryStorageType arg) {
                return arg.toString();
            }
        });
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

    public boolean isSupportConfigVolumeProvisionStrategy() {
        return supportConfigVolumeProvisionStrategy;
    }

    public void setSupportConfigVolumeProvisionStrategy(boolean supportConfigVolumeProvisionStrategy) {
        this.supportConfigVolumeProvisionStrategy = supportConfigVolumeProvisionStrategy;
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

    public boolean isSupportCreateVolumeSnapshotCheckCapacity() {
        return supportCreateVolumeSnapshotCheckCapacity;
    }

    public void setSupportCreateVolumeSnapshotCheckCapacity(boolean supportCreateVolumeSnapshotCheckCapacity) {
        this.supportCreateVolumeSnapshotCheckCapacity = supportCreateVolumeSnapshotCheckCapacity;
    }
}
