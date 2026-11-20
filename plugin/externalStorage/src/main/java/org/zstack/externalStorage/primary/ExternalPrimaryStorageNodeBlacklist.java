package org.zstack.externalStorage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostExtensionManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.storage.addon.primary.SetHostStorageNodeBlacklistMsg;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.tag.SystemTagCreator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH;
import static org.zstack.kvm.KVMSystemTags.NODE_BLACKLIST;
import static org.zstack.kvm.KVMSystemTags.NODE_BLACKLIST_PRIMARY_STORAGE_UUID_TOKEN;

public class ExternalPrimaryStorageNodeBlacklist implements HostExtensionManager, Component {
    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    @Override
    public boolean start() {
        evtf.onLocal(PRIMARY_STORAGE_DELETED_PATH, new EventCallback<PrimaryStorageDeletedData>() {
            @Override
            protected void run(Map<String, String> tokens, PrimaryStorageDeletedData data) {
                NODE_BLACKLIST.deleteInherentTag(null, blacklistTag(data.getPrimaryStorageUuid()));
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getMessageClasses() {
        return Collections.singletonList(SetHostStorageNodeBlacklistMsg.class);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof SetHostStorageNodeBlacklistMsg) {
            handle((SetHostStorageNodeBlacklistMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(SetHostStorageNodeBlacklistMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "storage-node-blacklist-" + msg.getHostUuid();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                PrimaryStorageNodeSvc nodeSvc = getNodeBlacklistSvc(msg.getPrimaryStorageUuid());
                if (nodeSvc != null) {
                    HostVO host = getHost(msg.getHostUuid());
                    if (msg.isBlacklist()) {
                        blacklist(host, msg.getPrimaryStorageUuid(), nodeSvc);
                    } else {
                        unblacklist(host, msg.getPrimaryStorageUuid(), nodeSvc);
                    }
                }
                bus.reply(msg, new MessageReply());
                chain.next();
            }
        });
    }

    private void blacklist(HostVO host, String psUuid, PrimaryStorageNodeSvc nodeSvc) {
        SystemTagCreator creator = NODE_BLACKLIST.newSystemTagCreator(host.getUuid());
        creator.tag = blacklistTag(psUuid);
        creator.inherent = true;
        creator.ignoreIfExisting = true;
        creator.create();
        nodeSvc.blacklistNode(HostInventory.valueOf(host));
    }

    private void unblacklist(HostVO host, String psUuid, PrimaryStorageNodeSvc nodeSvc) {
        String tag = blacklistTag(psUuid);
        if (NODE_BLACKLIST.getTags(host.getUuid()).contains(tag)) {
            nodeSvc.unblacklistNode(HostInventory.valueOf(host));
            NODE_BLACKLIST.deleteInherentTag(host.getUuid(), tag);
        }
    }

    private PrimaryStorageNodeSvc getNodeBlacklistSvc(String psUuid) {
        if (!dbf.isExist(psUuid, ExternalPrimaryStorageVO.class)
                || !extPsFactory.getControllerSvc(psUuid).reportCapabilities().isSupportNodeBlacklist()) {
            return null;
        }
        return extPsFactory.getNodeSvc(psUuid);
    }

    private HostVO getHost(String hostUuid) {
        HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
        if (host == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("host[uuid:%s] no longer exists", hostUuid)));
        }
        return host;
    }

    private String blacklistTag(String psUuid) {
        return NODE_BLACKLIST.instantiateTag(
                Collections.singletonMap(NODE_BLACKLIST_PRIMARY_STORAGE_UUID_TOKEN, psUuid));
    }
}
