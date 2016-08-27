package org.zstack.header.vm;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by david on 8/4/16.
 */
@ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
public class CreateVmInstanceMsg extends NeedReplyMessage implements CreateVmInstanceMessage {
    private String accountUuid;
    private String name;
    private String imageUuid;
    private String instanceOfferingUuid;
    private int cpuNum;
    private long cpuSpeed;
    private long memorySize;
    private List<String> l3NetworkUuids;
    private String type;
    private String rootDiskOfferingUuid;
    private List<String> dataDiskOfferingUuids;
    private String zoneUuid;
    private String clusterUuid;
    private String hostUuid;
    private String description;
    private String resourceUuid;
    private String defaultL3NetworkUuid;
    private String allocatorStrategy;

    @Override
    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    @Override
    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    @Override
    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    @Override
    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    @Override
    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    @Override
    public long getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    @Override
    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    @Override
    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
