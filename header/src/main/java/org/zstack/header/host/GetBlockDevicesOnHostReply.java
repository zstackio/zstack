package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetBlockDevicesOnHostReply extends MessageReply {
    private List<HostBlockDeviceStruct> blockDevices;

    public List<HostBlockDeviceStruct> getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(List<HostBlockDeviceStruct> blockDevices) {
        this.blockDevices = blockDevices;
    }
}
