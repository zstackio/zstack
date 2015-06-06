package org.zstack.header.vm;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
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
    private InstanceOfferingInventory instanceOffering;
    private ImageSpec imageSpec = new ImageSpec();
    private List<VolumeSpec> volumeSpecs = new ArrayList<VolumeSpec>();

    private List<HostName> hostnames = new ArrayList<HostName>();
    private HostInventory destHost;
    private List<VmNicInventory> destNics = new ArrayList<VmNicInventory>();
    private List<VolumeInventory> destDataVolumes = new ArrayList<VolumeInventory>();
    private VolumeInventory destRootVolume;
    private VmOperation currentVmOperation;
    private Map<String, JsonWrapper> extensionData = new HashMap<String, JsonWrapper>();
    private String dataIsoPath;
    private IsoSpec destIso;

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
            dataDiskOfferings = new ArrayList<DiskOfferingInventory>(0);
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
	
	
}
