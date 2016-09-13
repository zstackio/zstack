package org.zstack.test;

import org.zstack.core.MessageCommandRecorder;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class VmCreator {
    private static final CLogger logger = Utils.getLogger(VmCreator.class);

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
    public List<String> systemTags = new ArrayList<String>();
    public List<String> userTags;
    public String rootDiskOfferingUuid;
    public int timeout = 15;
    public SessionInventory session;
    public VmCreationStrategy strategy;

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
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APICreateVmInstanceMsg.class);

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
        msg.setStrategy(strategy.toString());
        msg.setSession(session == null ? api.getAdminSession() : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVmInstanceEvent evt = sender.send(msg, APICreateVmInstanceEvent.class);

        String callingChain = MessageCommandRecorder.endAndToString();
        logger.debug(callingChain);

        return evt.getInventory();
    }

    public CloneVmInstanceResults cloneVm(List<String> names, String instanceUuid) throws ApiSenderException {
        APICloneVmInstanceMsg msg = new APICloneVmInstanceMsg();
        msg.setNames(names);
        msg.setVmInstanceUuid(instanceUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? api.getAdminSession() : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICloneVmInstanceEvent evt = sender.send(msg, APICloneVmInstanceEvent.class);

        return evt.getResult();
    }
}
