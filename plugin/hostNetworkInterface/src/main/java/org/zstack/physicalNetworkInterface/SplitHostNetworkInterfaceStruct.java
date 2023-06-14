package org.zstack.physicalNetworkInterface;

import org.zstack.physicalNetworkInterface.header.HostNetworkInterfaceInventory;

import java.io.Serializable;

public class SplitHostNetworkInterfaceStruct implements Serializable {
    private HostNetworkInterfaceInventory hostNetworkInterface;
    private PhysicalNicActionType actionType;
    private Integer virtPartNum;
    //for smart nic
    private long socketMem;
    private long reserveSize;
    private long pageSize;


    public HostNetworkInterfaceInventory getHostNetworkInterface() {
        return hostNetworkInterface;
    }

    public void setHostNetworkInterface(HostNetworkInterfaceInventory hostNetworkInterface) {
        this.hostNetworkInterface = hostNetworkInterface;
    }

    public PhysicalNicActionType getActionType() {
        return actionType;
    }

    public void setActionType(PhysicalNicActionType actionType) {
        this.actionType = actionType;
    }

    public Integer getVirtPartNum() {
        return virtPartNum;
    }

    public void setVirtPartNum(Integer virtPartNum) {
        this.virtPartNum = virtPartNum;
    }

    public long getSocketMem() {
        return socketMem;
    }

    public void setSocketMem(long socketMem) {
        this.socketMem = socketMem;
    }

    public long getReserveSize() {
        return reserveSize;
    }

    public void setReserveSize(long reserveSize) {
        this.reserveSize = reserveSize;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
