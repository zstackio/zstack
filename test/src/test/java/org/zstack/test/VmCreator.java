package org.zstack.test;

import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.APICreateVmInstanceEvent;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
public class VmCreator {
    List<String> l3NetworkUuids = new ArrayList<String>();
    public String imageUuid;
    public String instanceOfferingUuid;
    List<String> diskOfferingUuids = new ArrayList<String>();
    public String zoneUuid;
    public String clusterUUid;
    public String hostUuid;
    public String defaultL3NetworkUuid;
    public String description;
    public String name = "vm";
    public List<String> systemTags;
    public List<String> userTags;
    public String rootDiskOfferingUuid;
    public int timeout = 15;
    public SessionInventory session;

    final Api api;

    public VmCreator(Api api) {
        this.api = api;
    }

    public void addL3Network(String uuid) {
        l3NetworkUuids.add(uuid);
    }

    public void addDisk(String uuid) {
        diskOfferingUuids.add(uuid);
    }

    public VmInstanceInventory create() throws ApiSenderException {
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setClusterUuid(clusterUUid);
        msg.setImageUuid(imageUuid);
        msg.setName(name);
        msg.setHostUuid(hostUuid);
        msg.setDataDiskOfferingUuids(diskOfferingUuids);
        msg.setInstanceOfferingUuid(instanceOfferingUuid);
        msg.setL3NetworkUuids(l3NetworkUuids);
        msg.setDefaultL3NetworkUuid(defaultL3NetworkUuid == null ? l3NetworkUuids.get(0) : defaultL3NetworkUuid);
        msg.setType(VmInstanceConstant.USER_VM_TYPE);
        msg.setZoneUuid(zoneUuid);
        msg.setHostUuid(hostUuid);
        msg.setRootDiskOfferingUuid(rootDiskOfferingUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSystemTags(systemTags);
        msg.setUserTags(userTags);
        msg.setDescription(description);
        msg.setSession(session == null ? api.getAdminSession() : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVmInstanceEvent evt = sender.send(msg, APICreateVmInstanceEvent.class);
        return evt.getInventory();
    }
}
