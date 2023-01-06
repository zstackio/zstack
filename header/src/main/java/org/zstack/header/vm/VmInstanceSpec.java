package org.zstack.header.vm;

import org.zstack.header.allocator.AllocationScene;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.Message;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.JsonWrapper;

import java.io.Serializable;
import java.util.*;


public class VmInstanceSpec implements Serializable {

    public List<String> getDataVolumeTemplateUuids() {
        return dataVolumeTemplateUuids;
    }

    public void setDataVolumeTemplateUuids(List<String> dataVolumeTemplateUuids) {
        this.dataVolumeTemplateUuids = dataVolumeTemplateUuids;
    }

    public Map<String, List<String>> getDataVolumeFromTemplateSystemTags() {
        return dataVolumeFromTemplateSystemTags;
    }

    public void setDataVolumeFromTemplateSystemTags(Map<String, List<String>> dataVolumeFromTemplateSystemTags) {
        this.dataVolumeFromTemplateSystemTags = dataVolumeFromTemplateSystemTags;
    }

    public static class VolumeSpec {
        private PrimaryStorageInventory primaryStorageInventory;
        private String type;
        private long size;
        private String diskOfferingUuid;
        private boolean isVolumeCreated;
        private List<String> tags;
        private String allocatedInstallUrl;
        private String associatedVolumeUuid;

        public String getAssociatedVolumeUuid() {
            return associatedVolumeUuid;
        }

        public void setAssociatedVolumeUuid(String associatedVolumeUuid) {
            this.associatedVolumeUuid = associatedVolumeUuid;
        }

        public String getAllocatedInstallUrl() {
            return allocatedInstallUrl;
        }

        public void setAllocatedInstallUrl(String allocatedInstallUrl) {
            this.allocatedInstallUrl = allocatedInstallUrl;
        }

        public boolean isVolumeCreated() {
            return isVolumeCreated;
        }

        public void setIsVolumeCreated(boolean isVolumeCreated) {
            this.isVolumeCreated = isVolumeCreated;
        }


        public PrimaryStorageInventory getPrimaryStorageInventory() {
            return primaryStorageInventory;
        }

        public void setPrimaryStorageInventory(PrimaryStorageInventory primaryStorageInventory) {
            this.primaryStorageInventory = primaryStorageInventory;
        }

        public String getDiskOfferingUuid() {
            return diskOfferingUuid;
        }

        public void setDiskOfferingUuid(String diskOfferingUuid) {
            this.diskOfferingUuid = diskOfferingUuid;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public boolean isRoot() {
            return VolumeType.Root.toString().equals(type);
        }
    }

    public static class ImageSpec implements Serializable {
        private ImageInventory inventory;
        private boolean needDownload = true;
        private ImageBackupStorageRefInventory selectedBackupStorage;

        public ImageInventory getInventory() {
            return inventory;
        }

        public void setInventory(ImageInventory inventory) {
            this.inventory = inventory;
        }

        public ImageBackupStorageRefInventory getSelectedBackupStorage() {
            return selectedBackupStorage;
        }

        public void setSelectedBackupStorage(ImageBackupStorageRefInventory selectedBackupStorage) {
            this.selectedBackupStorage = selectedBackupStorage;
        }

        public boolean isNeedDownload() {
            return needDownload;
        }

        public void setNeedDownload(boolean needDownload) {
            this.needDownload = needDownload;
        }
    }

    public static class IsoSpec implements Serializable {
        private String installPath;
        private String imageUuid;
        private String primaryStorageUuid;
        private String backupStorageUuid;
        private int deviceId;

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }
    }

    public static class CdRomSpec implements Serializable {
        private String uuid;
        private int deviceId;
        private String installPath;
        private String imageUuid;
        private String primaryStorageUuid;
        private String backupStorageUuid;

        public boolean isAttachedIso() {
            return imageUuid != null;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }
    }

    public static class HostName implements Serializable {
        private String l3NetworkUuid;
        private String hostname;

        public String getL3NetworkUuid() {
            return l3NetworkUuid;
        }

        public void setL3NetworkUuid(String l3NetworkUuid) {
            this.l3NetworkUuid = l3NetworkUuid;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }

    private VmInstanceInventory vmInventory;
    private List<VmNicSpec> l3Networks;
    private List<DiskOfferingInventory> dataDiskOfferings;
    private List<String> dataVolumeTemplateUuids;
    private Map<String, List<String>> dataVolumeFromTemplateSystemTags = new HashMap<>();
    private DiskOfferingInventory rootDiskOffering;
    private String hostAllocatorStrategy;
    private String ipAllocatorStrategy;
    private Message message;
    private ImageSpec imageSpec = new ImageSpec();
    private List<VolumeSpec> volumeSpecs = new ArrayList<>();
    private String requiredClusterUuid;
    private String requiredHostUuid;
    private List<String> softAvoidHostUuids;
    private List<String> avoidHostUuids;
    private String memorySnapshotUuid;
    private String requiredPrimaryStorageUuidForRootVolume;
    private String requiredPrimaryStorageUuidForDataVolume;
    private String bootMode;

    private List<HostName> hostnames = new ArrayList<>();
    private HostInventory srcHost;
    private HostInventory destHost;
    private List<VmNicInventory> destNics = new ArrayList<>();
    private List<VolumeInventory> destDataVolumes = new ArrayList<>();
    private List<VolumeInventory> destCacheVolumes = new ArrayList<>();
    private VolumeInventory destRootVolume;
    private VmOperation currentVmOperation;
    @NoJsonSchema
    private Map<String, JsonWrapper> extensionData = new HashMap<>();
    private String dataIsoPath;
    private List<IsoSpec> destIsoList = new ArrayList<>();
    private List<CdRomSpec> cdRomSpecs = new ArrayList<>();
    private List<String> userdataList;
    private List<String> bootOrders;
    private boolean gcOnStopFailure;
    private boolean ignoreResourceReleaseFailure;
    private boolean usbRedirect = false;
    private boolean enableSecurityElement = false;
    private String enableRDP = "false";
    private String VDIMonitorNumber = "1";
    @NoLogging
    private String consolePassword;
    private VmAccountPreference accountPerference;
    private boolean createPaused;
    private boolean instantiateResourcesSuccess;
    private boolean instantiateResourcesSkipExisting;
    private AllocationScene allocationScene;

    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private Map<String, List<String>> dataVolumeSystemTagsOnIndex;
    private boolean skipIpAllocation = false;

    private List<String> disableL3Networks;

    public boolean isSkipIpAllocation() {
        return skipIpAllocation;
    }

    public void setSkipIpAllocation(boolean skipIpAllocation) {
        this.skipIpAllocation = skipIpAllocation;
    }

    public AllocationScene getAllocationScene() {
        return allocationScene;
    }

    public void setAllocationScene(AllocationScene allocationScene) {
        this.allocationScene = allocationScene;
    }

    public String getVDIMonitorNumber() {
        return VDIMonitorNumber == null ? "1" : VDIMonitorNumber;
    }

    public void setVDIMonitorNumber(String VDIMonitorNumber) {
        this.VDIMonitorNumber = VDIMonitorNumber;
    }

    public String getEnableRDP() {
        return enableRDP;
    }

    public void setEnableRDP(String enableRDP) {
        this.enableRDP = enableRDP;
    }

    public boolean isUsbRedirect() {
        return usbRedirect;
    }

    public void setUsbRedirect(boolean usbRedirect) {
        this.usbRedirect = usbRedirect;
    }
    
    public boolean isEnableSecurityElement() {
        return enableSecurityElement;
    }
    
    public void setEnableSecurityElement(boolean enableSecurityElement) {
        this.enableSecurityElement = enableSecurityElement;
    }

    public void setCreatePaused(boolean createPaused) {
        this.createPaused = createPaused;
    }

    public boolean isCreatePaused() {
        return createPaused;
    }

    public VmAccountPreference getAccountPerference() {
        return accountPerference;
    }

    public void setAccountPerference(VmAccountPreference accountPerference) {
        this.accountPerference = accountPerference;
    }

    public String getRequiredClusterUuid() {
        return requiredClusterUuid;
    }

    public void setRequiredClusterUuid(String requiredClusterUuid) {
        this.requiredClusterUuid = requiredClusterUuid;
    }

    public String getRequiredHostUuid() {
        return requiredHostUuid;
    }

    public void setRequiredHostUuid(String requiredHostUuid) {
        this.requiredHostUuid = requiredHostUuid;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public boolean isGcOnStopFailure() {
        return gcOnStopFailure;
    }

    public void setGcOnStopFailure(boolean gcOnStopFailure) {
        this.gcOnStopFailure = gcOnStopFailure;
    }

    public List<String> getBootOrders() {
        return bootOrders;
    }

    public void setBootOrders(List<String> bootOrders) {
        this.bootOrders = bootOrders;
    }

    public String getConsolePassword() {
        return consolePassword;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }

    public List<String> getUserdataList() {
        return userdataList;
    }

    public void setUserdataList(List<String> userdataList) {
        this.userdataList = userdataList;
    }

    public List<IsoSpec> getDestIsoList() {
        return destIsoList;
    }

    public void setDestIsoList(List<IsoSpec> destIsoList) {
        this.destIsoList = destIsoList;
    }

    public List<CdRomSpec> getCdRomSpecs() {
        return cdRomSpecs;
    }

    public void setCdRomSpecs(List<CdRomSpec> cdRomSpecs) {
        this.cdRomSpecs = cdRomSpecs;
    }

    public VmInstanceSpec() {
    }

    public List<HostName> getHostnames() {
        return hostnames;
    }

    public void setHostnames(List<HostName> hostnames) {
        this.hostnames = hostnames;
    }

    public List<VolumeSpec> getVolumeSpecs() {
        return volumeSpecs;
    }

    public String getAllocatedUrlFromVolumeSpecs(String associatedVolumeUuid) {
        VolumeSpec vspec = volumeSpecs.stream()
                .filter(v -> associatedVolumeUuid.equals(v.getAssociatedVolumeUuid()))
                .findFirst()
                .orElse(null);

        return vspec != null ? vspec.getAllocatedInstallUrl() : null;
    }

    public void setVolumeSpecs(List<VolumeSpec> volumeSpecs) {
        this.volumeSpecs = volumeSpecs;
    }

    public List<VmNicSpec> getL3Networks() {
        return l3Networks;
    }

    public void setL3Networks(List<VmNicSpec> l3Networks) {
        this.l3Networks = l3Networks;
    }

    public List<DiskOfferingInventory> getDataDiskOfferings() {
        if (dataDiskOfferings == null) {
            dataDiskOfferings = new ArrayList<>(0);
        }
        return dataDiskOfferings;
    }

    public void setDataDiskOfferings(List<DiskOfferingInventory> dataDiskOfferings) {
        this.dataDiskOfferings = dataDiskOfferings;
    }

    public DiskOfferingInventory getRootDiskOffering() {
        return rootDiskOffering;
    }

    public void setRootDiskOffering(DiskOfferingInventory rootDiskOffering) {
        this.rootDiskOffering = rootDiskOffering;
    }

    public String getHostAllocatorStrategy() {
        return hostAllocatorStrategy;
    }

    public void setHostAllocatorStrategy(String hostAllocatorStrategy) {
        this.hostAllocatorStrategy = hostAllocatorStrategy;
    }

    public String getIpAllocatorStrategy() {
        return ipAllocatorStrategy;
    }

    public void setIpAllocatorStrategy(String ipAllocatorStrategy) {
        this.ipAllocatorStrategy = ipAllocatorStrategy;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public ImageSpec getImageSpec() {
        return imageSpec;
    }

    public void setImageSpec(ImageSpec imageSpec) {
        this.imageSpec = imageSpec;
    }

    public <T> T getExtensionData(String key, Class<?> clazz) {
        JsonWrapper<T> wrapper = extensionData.get(key);
        if (wrapper == null) {
            return null;
        }

        return wrapper.get();
    }

    public void putExtensionData(String key, Object data) {
        this.extensionData.put(key, JsonWrapper.wrap(data));
    }

    public HostInventory getDestHost() {
        return destHost;
    }

    public void setDestHost(HostInventory destHost) {
        this.destHost = destHost;
    }

    public List<VmNicInventory> getDestNics() {
        return destNics;
    }

    public void setDestNics(List<VmNicInventory> destNics) {
        this.destNics = destNics;
    }

    public List<VolumeInventory> getDestDataVolumes() {
        return destDataVolumes;
    }

    public void setDestDataVolumes(List<VolumeInventory> destDataVolumes) {
        this.destDataVolumes = destDataVolumes;
    }

    public List<VolumeInventory> getDestCacheVolumes() {
        return destCacheVolumes;
    }

    public void setDestCacheVolumes(List<VolumeInventory> destCacheVolumes) {
        this.destCacheVolumes = destCacheVolumes;
    }

    public VolumeInventory getDestRootVolume() {
        return destRootVolume;
    }

    public void setDestRootVolume(VolumeInventory destRootVolume) {
        this.destRootVolume = destRootVolume;
    }

    public VmOperation getCurrentVmOperation() {
        return currentVmOperation;
    }

    public void setCurrentVmOperation(VmOperation currentVmOperation) {
        this.currentVmOperation = currentVmOperation;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public String getDataIsoPath() {
        return dataIsoPath;
    }

    public void setDataIsoPath(String dataIsoPath) {
        this.dataIsoPath = dataIsoPath;
    }

    public String getMemorySnapshotUuid() {
        return memorySnapshotUuid;
    }

    public void setMemorySnapshotUuid(String memorySnapshotUuid) {
        this.memorySnapshotUuid = memorySnapshotUuid;
    }

    public List<String> getRequiredNetworkServiceTypes() {
        List<String> nsTypes = new ArrayList<>();
        if (getL3Networks() != null) {
            for (VmNicSpec nicSpec : getL3Networks()) {
                for (L3NetworkInventory l3: nicSpec.l3Invs) {
                    nsTypes.addAll(l3.getNetworkServiceTypes());
                }
            }
        }
        return nsTypes;
    }

    public String getRequiredPrimaryStorageUuidForRootVolume() {
        return requiredPrimaryStorageUuidForRootVolume;
    }

    public void setRequiredPrimaryStorageUuidForRootVolume(String requiredPrimaryStorageUuidForRootVolume) {
        this.requiredPrimaryStorageUuidForRootVolume = requiredPrimaryStorageUuidForRootVolume;
    }

    public String getRequiredPrimaryStorageUuidForDataVolume() {
        return requiredPrimaryStorageUuidForDataVolume;
    }

    public void setRequiredPrimaryStorageUuidForDataVolume(String requiredPrimaryStorageUuidForDataVolume) {
        this.requiredPrimaryStorageUuidForDataVolume = requiredPrimaryStorageUuidForDataVolume;
    }

    public HostInventory getSrcHost() {
        return srcHost;
    }

    public void setSrcHost(HostInventory srcHost) {
        this.srcHost = srcHost;
    }

    public boolean isIgnoreResourceReleaseFailure() {
        return ignoreResourceReleaseFailure;
    }

    public void setIgnoreResourceReleaseFailure(boolean ignoreResourceReleaseFailure) {
        this.ignoreResourceReleaseFailure = ignoreResourceReleaseFailure;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public List<String> getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(List<String> dataVolumeSystemTags) {
        this.dataVolumeSystemTags = dataVolumeSystemTags;
    }

    public Map<String, List<String>> getDataVolumeSystemTagsOnIndex() {
        return dataVolumeSystemTagsOnIndex;
    }

    public void setDataVolumeSystemTagsOnIndex(Map<String, List<String>> dataVolumeSystemTagsOnIndex) {
        this.dataVolumeSystemTagsOnIndex = dataVolumeSystemTagsOnIndex;
    }

    public boolean isInstantiateResourcesSuccess() {
        return instantiateResourcesSuccess;
    }

    public void setInstantiateResourcesSuccess(boolean instantiateResourcesSuccess) {
        this.instantiateResourcesSuccess = instantiateResourcesSuccess;
    }

    public boolean isInstantiateResourcesSkipExisting() {
        return instantiateResourcesSkipExisting;
    }

    public void setInstantiateResourcesSkipExisting(boolean instantiateResourcesSkipExisting) {
        this.instantiateResourcesSkipExisting = instantiateResourcesSkipExisting;
    }

    public String getBootMode() {
        return bootMode;
    }

    public void setBootMode(String bootMode) {
        this.bootMode = bootMode;
    }

    public long getRootDiskAllocateSize() {
        if (rootDiskOffering == null) {
            return this.getImageSpec().getInventory().getSize();
        }
        return rootDiskOffering.getDiskSize();
    }

    public List<String> getDisableL3Networks() {
        return disableL3Networks;
    }

    public void setDisableL3Networks(List<String> disableL3Networks) {
        this.disableL3Networks = disableL3Networks;
    }
}
