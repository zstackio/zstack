package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostBase;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.CheckNetworkPhysicalInterfaceMsg;
import org.zstack.header.network.l2.CheckNetworkPhysicalInterfaceReply;
import org.zstack.header.simulator.*;
import org.zstack.header.vm.VmAttachNicOnHypervisorMsg;
import org.zstack.header.vm.VmAttachNicOnHypervisorReply;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

class SimulatorHost extends HostBase {
    private static final CLogger logger = Utils.getLogger(SimulatorHost.class);
    private SimulatorConnection conn;
    private volatile boolean isDisconnected;

    @Autowired
    private PluginRegistry pluginRegty;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private SimulatorConfig config;


    protected SimulatorHost(SimulatorHostVO self) {
        super(self);
        conn = new SimulatorConnectionImpl(this);
    }

    @Override
    public void changeStateHook(HostState current, HostStateEvent stateEvent, HostState next) {
        logger.debug(String.format("Host: %s changed state from %s to %s by %s", self.getName(), current, next, stateEvent));
    }

    @Override
    public void deleteHook() {
        logger.debug(String.format("Host: %s is being deleted", self.getName()));
    }

    @Override
    public void connectHook(ConnectHostInfo info, Completion complete) {
        for (SimulatorConnectExtensionPoint sc : pluginRegty.getExtensionList(SimulatorConnectExtensionPoint.class)) {
            String err = sc.connect(conn);
            if (err != null) {
                logger.warn(err);
                complete.fail(errf.stringToOperationError(err));
                return;
            }
        }

        logger.debug(String.format("Host: %s is connected", self.getName()));
        complete.success();
    }

    @Override
    public void maintenanceHook(Completion completion) {
        logger.debug(String.format("Host: %s entered maintenance mode", self.getName()));
        completion.success();
    }

    SimulatorHostVO getSimulatorHostVO() {
        return (SimulatorHostVO) self;
    }

    @Override
    protected void pingHook(Completion completion) {
        if (!isDisconnected) {
            completion.success();
        } else {
            completion.fail(errf.stringToOperationError("set to disconnected"));
        }
    }

    @Override
    protected int getVmMigrateQuantity() {
        return 1;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        super.handleApiMessage(msg);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CheckNetworkPhysicalInterfaceMsg) {
            handle((CheckNetworkPhysicalInterfaceMsg) msg);
        } else if (msg instanceof SimulatorHostConnectionControlMsg) {
            handle((SimulatorHostConnectionControlMsg) msg);
        } else if (msg instanceof ChangeVmStateOnSimulatorHostMsg) {
            handle((ChangeVmStateOnSimulatorHostMsg) msg);
        } else if (msg instanceof RemoveVmOnSimulatorMsg) {
            handle((RemoveVmOnSimulatorMsg) msg);
        } else if (msg instanceof TakeSnapshotOnHypervisorMsg) {
            handle((TakeSnapshotOnHypervisorMsg) msg);
        } else if (msg instanceof DetachNicFromVmOnHypervisorMsg) {
            handle((DetachNicFromVmOnHypervisorMsg) msg);
        } else if (msg instanceof VmAttachNicOnHypervisorMsg) {
            handle((VmAttachNicOnHypervisorMsg) msg);

	    } else if (msg instanceof ChangeVmPasswordMsg) {
            handle((ChangeVmPasswordMsg) msg);
        } else {
	        super.handleLocalMessage(msg);
	    }
	}

    private void handle(MigrateVmOnHypervisorMsg msg) {
        config.removeVm(msg.getSrcHostUuid(), msg.getVmInventory().getUuid());
        config.putVm(msg.getDestHostInventory().getUuid(), msg.getVmInventory().getUuid(), VmInstanceState.Running);
        MigrateVmOnHypervisorReply reply = new MigrateVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(VmAttachNicOnHypervisorMsg msg) {
        bus.reply(msg, new VmAttachNicOnHypervisorReply());
    }

    private void handle(DetachNicFromVmOnHypervisorMsg msg) {
        DetachNicFromVmOnHypervisorReply reply = new DetachNicFromVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(TakeSnapshotOnHypervisorMsg msg) {
        TakeSnapshotOnHypervisorReply reply = new TakeSnapshotOnHypervisorReply();
        if (!config.snapshotSuccess) {
            reply.setError(errf.stringToOperationError("on purpose"));
            bus.reply(msg, reply);
            return;
        }

        List<TakeSnapshotOnHypervisorMsg> lst = config.snapshots.get(msg.getHostUuid());
        lst = new ArrayList<TakeSnapshotOnHypervisorMsg>();
        lst.add(msg);
        config.snapshots.put(msg.getHostUuid(), lst);
    }

    private void handle(RemoveVmOnSimulatorMsg msg) {
        logger.debug(String.format("Successfully remove vm[uuid:%s] on simulator host[uuid:%s]", msg.getVmUuid(), msg.getHostUuid()));
        removeVm(msg.getVmUuid());
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    private void handle(ChangeVmStateOnSimulatorHostMsg msg) {
        synchronized (config.vms) {
            if (!config.containVm(msg.getVmUuid())) {
                logger.warn(String.format("Cannot find vm[uuid:%s] on simulator host[uuid:%s], it's a stranger vm", msg.getVmUuid(), self.getUuid()));
            } else {
                logger.debug(String.format("Change state of vm[uuid:%s] on simulator host[uuid:%s] to %s", msg.getVmUuid(), self.getUuid(), msg.getVmState()));
            }
            String hostUuid = msg.getHostUuid() == null ? self.getUuid() : msg.getHostUuid();
            config.putVm(hostUuid, msg.getVmUuid(), VmInstanceState.valueOf(msg.getVmState()));
        }
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    private void handle(SimulatorHostConnectionControlMsg msg) {
        logger.debug(String.format("Change simulator[uuid:%s] isDisonnected to %s", self.getUuid(), msg.isDisconnected()));
        isDisconnected = msg.isDisconnected();
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    private void handle(CheckNetworkPhysicalInterfaceMsg msg) {
        logger.debug(String.format("Successfully checked physical network interface %s on simulator host", msg.getPhysicalInterface()));
        CheckNetworkPhysicalInterfaceReply reply = new CheckNetworkPhysicalInterfaceReply();
        bus.reply(msg, reply);
    }

    private void handle(final ChangeVmPasswordMsg msg) {
        logger.debug(String.format("SimulatorHost handle the message, hostid = %s ", msg.getHostUuid()));
        ChangeVmPasswordReply reply = new ChangeVmPasswordReply();
        bus.reply(msg, reply);
    }

    @Override
    public void executeHostMessageHandlerHook(HostMessageHandlerExtensionPoint ext, Message msg) {
        ext.handleMessage(msg, this);
    }

    void setVmState(String vmUuid, VmInstanceState state) {
        synchronized (config.vms) {
            config.putVm(self.getUuid(), vmUuid, state);
        }
    }

    void removeVm(String vmUuid) {
        synchronized (config.vms) {
            config.removeVm(self.getUuid(), vmUuid);
        }
    }
}
