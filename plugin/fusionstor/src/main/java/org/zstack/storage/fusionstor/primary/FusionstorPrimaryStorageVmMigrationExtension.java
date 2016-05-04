package org.zstack.storage.fusionstor.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMAgentCommands.FusionstorQueryCmd;
import org.zstack.kvm.KVMAgentCommands.FusionstorQueryRsp;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 6/16/2015.
 */
public class FusionstorPrimaryStorageVmMigrationExtension implements VmInstanceMigrateExtensionPoint {
    private CLogger logger = Utils.getLogger(FusionstorPrimaryStorageVmMigrationExtension.class);
    private Map<String, List<VolumeInventory>> vmVolumes = new ConcurrentHashMap<String, List<VolumeInventory>>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public String preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<KVMHostAsyncHttpCallMsg>();

        FusionstorQueryCmd cmd = new FusionstorQueryCmd();
        cmd.setQuery("query");

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(KVMConstant.KVM_FUSIONSTOR_QUERY_PATH);
        msg.setHostUuid(destHostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHostUuid);

        msgs.add(msg);

        List<MessageReply> replies = bus.call(msgs);
        ErrorCode errorCode = null;
        for (MessageReply reply : replies) {
            if (!reply.isSuccess()) {
                errorCode = reply.getError();
                break;
            } else {
                KVMHostAsyncHttpCallReply r = reply.castReply();
                FusionstorQueryRsp rsp = r.toResponse(FusionstorQueryRsp.class);
                if (!rsp.isSuccess()) {
                    errorCode = errf.stringToOperationError(rsp.getError());
                    break;
                }
            }
        }

        if (errorCode != null) {
            throw new OperationFailureException(errorCode);
        }

        return null;
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
    }
}
