package org.zstack.test;

import org.zstack.core.MessageCommandRecorder;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.*;
import org.zstack.sdk.ApiException;
import org.zstack.sdk.CreateVmInstanceAction;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class VmCreator {
    private static final CLogger logger = Utils.getLogger(VmCreator.class);

    List<String> l3NetworkUuids = new ArrayList<>();
    public String imageUuid;
    public String instanceOfferingUuid;
    List<String> diskOfferingUuids = new ArrayList<>();
    public String zoneUuid;
    public String clusterUUid;
    public String hostUuid;
    public String primaryStorageUuidForRootVolume;
    public String defaultL3NetworkUuid;
    public String description;
    public String name = "vm";
    public List<String> systemTags = new ArrayList<>();
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

        CreateVmInstanceAction action = new CreateVmInstanceAction();
        action.clusterUuid = clusterUUid;
        action.imageUuid = imageUuid;
        action.name = name;
        action.hostUuid = hostUuid;
        action.dataDiskOfferingUuids = diskOfferingUuids;
        action.instanceOfferingUuid = instanceOfferingUuid;
        action.l3NetworkUuids = l3NetworkUuids;
        action.defaultL3NetworkUuid = defaultL3NetworkUuid == null ? l3NetworkUuids.get(0) : defaultL3NetworkUuid;
        action.type = VmInstanceConstant.USER_VM_TYPE;
        action.zoneUuid = zoneUuid;
        action.hostUuid = hostUuid;
        action.rootDiskOfferingUuid = rootDiskOfferingUuid;
        action.systemTags = systemTags;
        action.userTags = userTags;
        action.description = description;
        action.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
        action.strategy = strategy == null ? null : strategy.toString();
        action.sessionId = session == null ? api.getAdminSession().getUuid() : session.getUuid();
        CreateVmInstanceAction.Result res = action.call();
        api.throwExceptionIfNeed(res.error);

        String callingChain = MessageCommandRecorder.endAndToString();
        logger.debug(callingChain);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), VmInstanceInventory.class);
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
