package org.zstack.header.vm;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.JsonWrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VmInstanceSpec implements Serializable {

    public static class VolumeSpec {
        private PrimaryStorageInventory primaryStorageInventory;
        private boolean isRoot;
        private long size;
        private String diskOfferingUuid;
        private boolean isVolumeCreated;

        public boolean isVolumeCreated() {
            return isVolumeCreated;
        }

        public void setIsVolumeCreated(boolean isVolumeCreated) {
            this.isVolumeCreated = isVolumeCreated;
        }

        public void setIsRoot(boolean isRoot) {
            this.isRoot = isRoot;
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

        public boolean isRoot() {
            return isRoot;
        }

        public void setRoot(boolean isRoot) {
            this.isRoot = isRoot;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class ImageSpec implements Serializable {
        private ImageInventory inventory;
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
    }

    public static class IsoSpec implements Serializable {
        private String installPath;
        private String imageUuid;
        private String primaryStorageUuid;
        private String backupStorageUuid;

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
    private List<L3NetworkInventory> l3Networks;
    private List<DiskOfferingInventory> dataDiskOfferings;
    private DiskOfferingInventory rootDiskOffering;
    private String hostAllocatorStrategy;
    private String ipAllocatorStrategy;
    private Message message;
    private ImageSpec imageSpec = new ImageSpec();
    private List<VolumeSpec> volumeSpecs = new ArrayList<>();
    private String requiredClusterUuid;
    private String requiredHostUuid;
    private String requiredPrimaryStorageUuidForRootVolume;
    private String requiredPrimaryStorageUuidForDataVolume;

    private List<HostName> hostnames = new ArrayList<>();
    private HostInventory destHost;
    private List<VmNicInventory> destNics = new ArrayList<>();
    private List<VolumeInventory> destDataVolumes = new ArrayList<>();
    private VolumeInventory destRootVolume;
    private VmOperation currentVmOperation;
    @NoJsonSchema
    private Map<String, JsonWrapper> extensionData = new HashMap<>();
    private String dataIsoPath;
    private IsoSpec destIso;
    private String userdata;
    private List<String> bootOrders;
    private boolean gcOnStopFailure;
    private String usbRedirect = "false";
    private String enableRDP = "false";
    private String VDIMonitorNumber = "1";
    private String consolePassword;
    private VmAccountPreference accountPerference;

    public String getVDIMonitorNumber() {
        return VDIMonitorNumber;
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

    public String getUsbRedirect() {
        return usbRedirect;
    }

    public void setUsbRedirect(String usbRedirect) {
        this.usbRedirect = usbRedirect;
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

    public String getUserdata() {
        return userdata;
    }

    public void setUserdata(String userdata) {
        this.userdata = userdata;
    }

    public IsoSpec getDestIso() {
        return destIso;
    }

    public void setDestIso(IsoSpec destIso) {
        this.destIso = destIso;
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

    public void setVolumeSpecs(List<VolumeSpec> volumeSpecs) {
        this.volumeSpecs = volumeSpecs;
    }

    public List<L3NetworkInventory> getL3Networks() {
        return l3Networks;
    }

    public void setL3Networks(List<L3NetworkInventory> l3Networks) {
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


    public List<String> getRequiredNetworkServiceTypes() {
        List<String> nsTypes = new ArrayList<>();
        if (getL3Networks() != null) {
            for (L3NetworkInventory l3 : getL3Networks()) {
                for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
                    nsTypes.add(ref.getNetworkServiceType());
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
}
