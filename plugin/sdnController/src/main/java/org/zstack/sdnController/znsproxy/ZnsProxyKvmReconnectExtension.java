package org.zstack.sdnController.znsproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.host.HostConnectionReestablishExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

public class ZnsProxyKvmReconnectExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ZnsProxyKvmReconnectExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        ensureOnKvmHost(inv);
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return KVMHostFactory.hypervisorType;
    }

    protected void ensureHost(String hostUuid) {
        new ZnsProxyInstaller(dbf).ensureHost(hostUuid);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "reinstall-zns-proxy";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                try {
                    ensureOnKvmHost(context.getInventory());
                    trigger.next();
                } catch (OperationFailureException e) {
                    trigger.fail(e.getErrorCode());
                }
            }
        };
    }

    protected ErrorCode toOperationError(String details) {
        return errf.stringToOperationError(details);
    }

    private void ensureOnKvmHost(HostInventory inv) {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            return;
        }
        if (!CpuArchitecture.x86_64.name().equals(inv.getArchitecture())) {
            logger.info(String.format(
                    "skip zns-proxy installation for unsupported host architecture[%s], hostUuid[%s]",
                    inv.getArchitecture(), inv.getUuid()));
            return;
        }
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }
        try {
            ensureHost(inv.getUuid());
        } catch (CloudRuntimeException e) {
            throw new OperationFailureException(toOperationError(e.getMessage()));
        }
    }
}
