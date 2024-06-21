package org.zstack.header.host;

import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.*;

public class BlockDevicesParser {
    private final List<BlockDevice> allBlockDevices = new ArrayList<>();

    public static class BlockDevice {
        private String name;
        private String type;
        private long size;
        @SerializedName(value = "phy-sec")
        private long physicalSector;
        @SerializedName(value = "log-sec")
        private long logicalSector;
        @SerializedName(value = "mountpoint")
        private String mountPoint;
        private List<BlockDevice> children;
        private String partitionTable;

        BlockDevice() {
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

        public List<BlockDevice> getChildren() {
            return children;
        }

        public void setChildren(List<BlockDevice> children) {
            this.children = children;
        }

        public String getPartitionTable() {
            return partitionTable;
        }

        public void setPartitionTable(String partitionTable) {
            this.partitionTable = partitionTable;
        }
    }

    public static  String blockDevicesExample = "{\n" +
            "   \"blockdevices\": [\n" +
            "      {\"name\": \"/dev/sr0\", \"type\": \"rom\", \"size\": \"8019515392\", \"phy-sec\": \"2048\", \"log-sec\": \"2048\", \"mountpoint\": null},\n" +
            "      {\"name\": \"/dev/vda\", \"type\": \"disk\", \"size\": \"429496729600\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null,\n" +
            "         \"children\": [\n" +
            "            {\"name\": \"/dev/vda1\", \"type\": \"part\", \"size\": \"1073741824\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": \"/boot\"},\n" +
            "            {\"name\": \"/dev/vda2\", \"type\": \"part\", \"size\": \"428421939200\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null,\n" +
            "               \"children\": [\n" +
            "                  {\"name\": \"/dev/mapper/zstack-root\", \"type\": \"lvm\", \"size\": \"419950493696\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": \"/\"},\n" +
            "                  {\"name\": \"/dev/mapper/zstack-swap\", \"type\": \"lvm\", \"size\": \"8468299776\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": \"[SWAP]\"}\n" +
            "               ]\n" +
            "            }\n" +
            "         ]\n" +
            "      },\n" +
            "      {\"name\": \"/dev/vdb\", \"type\": \"disk\", \"size\": \"322122547200\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null},\n" +
            "      {\"name\": \"/dev/vdc\", \"type\": \"disk\", \"size\": \"429496729600\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null}\n" +
            "   ]\n" +
            "}\n" +
            "===\r\n" +
            "/dev/sr0:unknown\r\n" +
            "/dev/vda:msdos\r\n" +
            "/dev/vdb:loop\r\n" +
            "/dev/vdc:unknown";

    public static List<BlockDevice> parse(String blockDevices) {
        List<BlockDevice> allBlockDevices = new ArrayList<>();

        List<String> splits = Arrays.asList(blockDevices.split("===\\r\\n"));
        LinkedHashMap blockDevicesMap = JSONObjectUtil.toObject(splits.get(0), LinkedHashMap.class);
        List<LinkedTreeMap> blockDeviceList = (List<LinkedTreeMap>) blockDevicesMap.get("blockdevices");
        blockDeviceList.forEach(blockDevice -> allBlockDevices.add(JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(blockDevice), BlockDevice.class)));

        Map<String, String> blockDevicePartitionTable = new HashMap<>();
        Arrays.asList(splits.get(1).split("\\r\\n")).forEach(it -> blockDevicePartitionTable.put(it.split(":")[0], it.split(":")[1]));
        allBlockDevices.forEach(blockDevice -> blockDevice.setPartitionTable(blockDevicePartitionTable.get(blockDevice.getName())));

        return allBlockDevices;
    }

    public static String getBlockDevicesCommand() {
        String blockDevicesCommand = "lsblk -p -b -o NAME,TYPE,SIZE,PHY-SEC,LOG-SEC,MOUNTPOINT -J";
        String partitionTableInfoCommand = "for disk in $(lsblk -d -p -n -o NAME); do echo -n \"$disk:\"; " +
                "parted $disk print 2>/dev/null | awk '/Partition Table/ {print $3}'; done";
        return String.format("%s ; echo === ; %s", blockDevicesCommand, partitionTableInfoCommand);
    }
}
