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

}
