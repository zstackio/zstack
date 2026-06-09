package org.zstack.sdnController.znsproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConnectionReestablishExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.sdnController.SdnControllerSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

public class ZnsProxyKvmReconnectExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ZnsProxyKvmReconnectExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        reinstallOnPreparedHost(inv);
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return KVMHostFactory.hypervisorType;
    }

    protected boolean isZnsProxyPrepared(String hostUuid) {
        return SdnControllerSystemTags.ZNS_PROXY_PREPARED.hasTag(hostUuid);
    }

    protected void reinstallPreparedHost(String hostUuid) {
        new ZnsProxyInstaller(dbf).reinstallPreparedHost(hostUuid);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "reinstall-zns-proxy";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.isNewAddedHost()) {
                    trigger.next();
                    return;
                }

                try {
                    reinstallOnPreparedHost(context.getInventory());
                    trigger.next();
                } catch (OperationFailureException e) {
                    trigger.fail(e.getErrorCode());
                }
            }
        };
    }

    private void reinstallOnPreparedHost(HostInventory inv) {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            return;
        }

        if (!isZnsProxyPrepared(inv.getUuid())) {
            logger.debug(String.format("skip zns-proxy reconnect install for host[uuid:%s], prepared tag not found", inv.getUuid()));
            return;
        }

        logger.info(String.format("reinstall zns-proxy on host[uuid:%s] during reconnect", inv.getUuid()));
        reinstallPreparedHost(inv.getUuid());
    }
}
