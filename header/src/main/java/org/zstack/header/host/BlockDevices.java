package org.zstack.header.host;

import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class BlockDevices {
    List<BlockDevice> unusedBlockDevices = new ArrayList<>();
    List<BlockDevice> usedBlockDevices = new ArrayList<>();

    BlockDevices() {
    }

    public static BlockDevices valueOf(List<BlockDevicesParser.BlockDevice> allBlockDevices) {
        BlockDevices blockDevices = new BlockDevices();

        List<BlockDevices.BlockDevice> devices = new ArrayList<>();
        allBlockDevices.forEach(blockDevice -> devices.add(BlockDevices.BlockDevice.valueOf(blockDevice)));

        devices.forEach(blockDevice -> {
            if (blockDevice.isUsed(blockDevice)) {
                blockDevices.usedBlockDevices.add(blockDevice);
            } else {
                blockDevices.unusedBlockDevices.add(blockDevice);
            }
        });
        return blockDevices;
    }

    public void filter(List<String> excludedBlockDevicesType) {
        if (CollectionUtils.isEmpty(excludedBlockDevicesType)) {
            return;
        }

        unusedBlockDevices.removeIf(blockDevice -> excludedBlockDevicesType.contains(blockDevice.getType()));
        usedBlockDevices.removeIf(blockDevice -> excludedBlockDevicesType.contains(blockDevice.getType()));
    }

    public List<BlockDevice> getAllBlockDevices() {
        List<BlockDevice> allBlockDevices = new ArrayList<>();
        allBlockDevices.addAll(unusedBlockDevices);
        allBlockDevices.addAll(usedBlockDevices);
        return allBlockDevices;
    }

    public static class BlockDevice {
        private String name;
        private String type;
        private long size;
        private long used;
        private long available;
        private long physicalSector;
        private long logicalSector;
        private String mountPoint;
        private List<BlockDevice> children;
        private String partitionTable;
        private String FSType;
        private String serialNumber;
        private String model;
        private String mediaType;
        private long usedRatio;
        private boolean status;

        BlockDevice() {

        }

        public static BlockDevice valueOf(BlockDevicesParser.BlockDevice blockDevice) {
            BlockDevice device = new BlockDevice();
            device.name = blockDevice.getName();
            device.type = blockDevice.getType();
            device.size = blockDevice.getSize();
            device.physicalSector = blockDevice.getPhysicalSector();
            device.logicalSector = blockDevice.getLogicalSector();
            device.mountPoint = blockDevice.getMountPoint();
            device.partitionTable = blockDevice.getPartitionTable() != null ? blockDevice.getPartitionTable() : "unknown";
            if (!CollectionUtils.isEmpty(blockDevice.getChildren())) {
                device.children = new ArrayList<>();
                for (BlockDevicesParser.BlockDevice child : blockDevice.getChildren()) {
                    device.children.add(valueOf(child));
                }
            }
            device.FSType = blockDevice.getFstype();
            device.serialNumber = blockDevice.getSerial();
            device.model = blockDevice.getModel();
            return device;
        }

        private boolean isUsed(BlockDevice device) {
            if (device.mountPoint != null) {
                return true;
            }

            if (!CollectionUtils.isEmpty(device.children)) {
                for (BlockDevice child : device.children) {
                    if (isUsed(child)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public long getUsed() {
            return used;
        }

        public void setUsed(long used) {
            this.used = used;
        }

        public long getAvailable() {
            return available;
        }

        public void setAvailable(long available) {
            this.available = available;
        }

        public long getPhysicalSector() {
            return physicalSector;
        }

        public void setPhysicalSector(long physicalSector) {
            this.physicalSector = physicalSector;
        }

        public long getLogicalSector() {
            return logicalSector;
        }

        public void setLogicalSector(long logicalSector) {
            this.logicalSector = logicalSector;
        }

        public String getMountPoint() {
            return mountPoint;
        }

        public void setMountPoint(String mountPoint) {
            this.mountPoint = mountPoint;
        }

        public List<BlockDevices.BlockDevice> getChildren() {
            return children;
        }

        public void setChildren(List<BlockDevices.BlockDevice> children) {
            this.children = children;
        }

        public String getPartitionTable() {
            return partitionTable;
        }

        public void setPartitionTable(String partitionTable) {
            this.partitionTable = partitionTable;
        }

        public String getFSType() {
            return FSType;
        }

        public void setFSType(String FSType) {
            this.FSType = FSType;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public long getUsedRatio() {
            return usedRatio;
        }

        public void setUsedRatio(long usedRatio) {
            this.usedRatio = usedRatio;
        }

        public boolean isStatus() {
            return status;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }
    }
}
