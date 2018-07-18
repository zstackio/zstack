package org.zstack.storage.fusionstor.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.fusionstor.FusionstorConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 6/16/2015.
 */


public class FusionstorPrimaryStorageVmMigrationExtension implements VmInstanceMigrateExtensionPoint {
    public static final String KVM_FUSIONSTOR_QUERY_PATH = "/fusionstor/query";

    public static class FusionstorQueryRsp extends AgentResponse {
        public String rsp;
    }

    public static class FusionstorQueryCmd extends AgentCommand {
        public String query;
    }

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

    @Transactional(readOnly = true)
    private boolean needLink(VmInstanceInventory inv) {
        String sql = "select ps.type from PrimaryStorageVO ps, VolumeVO vol where ps.uuid = vol.primaryStorageUuid" +
                " and vol.uuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", inv.getRootVolumeUuid());
        List<String> res = q.getResultList();
        if (res.isEmpty()) {
            return false;
        }

        String type = res.get(0);
        return FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE.equals(type);
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (!needLink(inv)) {
            return;
        }

        FusionstorQueryCmd cmd = new FusionstorQueryCmd();
        cmd.query = "query";

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(KVM_FUSIONSTOR_QUERY_PATH);
        msg.setHostUuid(destHostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHostUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        KVMHostAsyncHttpCallReply r = reply.castReply();
        FusionstorQueryRsp rsp = r.toResponse(FusionstorQueryRsp.class);
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
        }

        return;
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
