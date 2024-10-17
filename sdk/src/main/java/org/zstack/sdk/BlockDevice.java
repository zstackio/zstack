package org.zstack.sdk;



public class BlockDevice  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public long size;
    public void setSize(long size) {
        this.size = size;
    }
    public long getSize() {
        return this.size;
    }

    public long used;
    public void setUsed(long used) {
        this.used = used;
    }
    public long getUsed() {
        return this.used;
    }

    public long available;
    public void setAvailable(long available) {
        this.available = available;
    }
    public long getAvailable() {
        return this.available;
    }

    public long physicalSector;
    public void setPhysicalSector(long physicalSector) {
        this.physicalSector = physicalSector;
    }
    public long getPhysicalSector() {
        return this.physicalSector;
    }

    public long logicalSector;
    public void setLogicalSector(long logicalSector) {
        this.logicalSector = logicalSector;
    }
    public long getLogicalSector() {
        return this.logicalSector;
    }

    public java.lang.String mountPoint;
    public void setMountPoint(java.lang.String mountPoint) {
        this.mountPoint = mountPoint;
    }
    public java.lang.String getMountPoint() {
        return this.mountPoint;
    }

    public java.util.List children;
    public void setChildren(java.util.List children) {
        this.children = children;
    }
    public java.util.List getChildren() {
        return this.children;
    }

    public java.lang.String partitionTable;
    public void setPartitionTable(java.lang.String partitionTable) {
        this.partitionTable = partitionTable;
    }
    public java.lang.String getPartitionTable() {
        return this.partitionTable;
    }

    public java.lang.String FSType;
    public void setFSType(java.lang.String FSType) {
        this.FSType = FSType;
    }
    public java.lang.String getFSType() {
        return this.FSType;
    }

    public java.lang.String serialNumber;
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }
    public java.lang.String getSerialNumber() {
        return this.serialNumber;
    }

    public java.lang.String model;
    public void setModel(java.lang.String model) {
        this.model = model;
    }
    public java.lang.String getModel() {
        return this.model;
    }

    public java.lang.String mediaType;
    public void setMediaType(java.lang.String mediaType) {
        this.mediaType = mediaType;
    }
    public java.lang.String getMediaType() {
        return this.mediaType;
    }

    public long usedRatio;
    public void setUsedRatio(long usedRatio) {
        this.usedRatio = usedRatio;
    }
    public long getUsedRatio() {
        return this.usedRatio;
    }

    public boolean status;
    public void setStatus(boolean status) {
        this.status = status;
    }
    public boolean getStatus() {
        return this.status;
    }

}
