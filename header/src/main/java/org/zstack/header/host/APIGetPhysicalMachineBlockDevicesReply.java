package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.header.host.BlockDevicesParser.blockDevicesExample;

@RestResponse(allTo = "blockDevices")
public class APIGetPhysicalMachineBlockDevicesReply extends APIReply {
    BlockDevices blockDevices;

    public BlockDevices getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(BlockDevices blockDevices) {
        this.blockDevices = blockDevices;
    }

    public static class BlockDevices {
        List<BlockDevice> unusedBlockDevices = new ArrayList<>();
        List<BlockDevice> usedBlockDevices = new ArrayList<>();

        BlockDevices() {
        }

        public static BlockDevices valueOf(List<BlockDevicesParser.BlockDevice> allBlockDevices) {
            BlockDevices blockDevices = new BlockDevices();

            List<BlockDevice> devices = new ArrayList<>();
            allBlockDevices.forEach(blockDevice -> devices.add(BlockDevice.valueOf(blockDevice)));

            devices.forEach(blockDevice -> {
                if (blockDevice.isUsed(blockDevice)) {
                    blockDevices.usedBlockDevices.add(blockDevice);
                } else {
                    blockDevices.unusedBlockDevices.add(blockDevice);
                }
            });
            return blockDevices;
        }

        public static class BlockDevice {
            private String name;
            private String type;
            private long size;
            private long physicalSector;
            private long logicalSector;
            private String mountPoint;
            private List<BlockDevice> children;
            private String partitionTable;

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
                device.partitionTable = blockDevice.getPartitionTable();
                if (!CollectionUtils.isEmpty(blockDevice.getChildren())) {
                    device.children = new ArrayList<>();
                    for (BlockDevicesParser.BlockDevice child : blockDevice.getChildren()) {
                        device.children.add(valueOf(child));
                    }
                }
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
        }
    }

    public static APIGetPhysicalMachineBlockDevicesReply __example__() {
        APIGetPhysicalMachineBlockDevicesReply reply = new APIGetPhysicalMachineBlockDevicesReply();
        reply.setBlockDevices(BlockDevices.valueOf(BlockDevicesParser.parse(blockDevicesExample)));
        return reply;
    }
}