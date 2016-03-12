package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostMessageHandlerExtensionPoint;
import org.zstack.header.host.MigrateVmOnHypervisorMsg;
import org.zstack.header.host.MigrateVmOnHypervisorReply;
import org.zstack.header.message.Message;
import org.zstack.header.simulator.ChangeVmStateOnSimulatorHostMsg;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstanceHostMessageHandlerExtension implements HostMessageHandlerExtensionPoint<SimulatorHost> {
    private static final CLogger logger = Utils.getLogger(VmInstanceHostMessageHandlerExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private SimulatorConfig config;
    @Autowired
    private ErrorFacade errf;

    @Override
    public List<String> getMessageNameTheExtensionServed() {
        List<String> msgs = new ArrayList<String>();
        msgs.add(new CreateVmOnHypervisorMsg().getMessageName());
        msgs.add(new StopVmOnHypervisorMsg().getMessageName());
        msgs.add(new RebootVmOnHypervisorMsg().getMessageName());
        msgs.add(new StartVmOnHypervisorMsg().getMessageName());
        msgs.add(new MigrateVmOnHypervisorMsg().getMessageName());
        msgs.add(new AttachVolumeToVmOnHypervisorMsg().getMessageName());
        msgs.add(new DetachVolumeFromVmOnHypervisorMsg().getMessageName());
        msgs.add(new DestroyVmOnHypervisorMsg().getMessageName());
        return msgs;
    }

    @Override
    public void handleMessage(Message msg, SimulatorHost context) {
        if (msg instanceof CreateVmOnHypervisorMsg) {
            handle((CreateVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof StopVmOnHypervisorMsg) {
            handle((StopVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof RebootVmOnHypervisorMsg) {
            handle((RebootVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof StartVmOnHypervisorMsg) {
            handle((StartVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof MigrateVmOnHypervisorMsg) {
            handle((MigrateVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof AttachVolumeToVmOnHypervisorMsg) {
            handle((AttachVolumeToVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof DetachVolumeFromVmOnHypervisorMsg) {
            handle((DetachVolumeFromVmOnHypervisorMsg)msg, context);
        } else if (msg instanceof DestroyVmOnHypervisorMsg) {
            handle((DestroyVmOnHypervisorMsg)msg, context);
        }
    }

    private void handle(DestroyVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully destroyed vm on simulator host[uuid:%s], %s", host.getSimulatorHostVO().getUuid(), JSONObjectUtil.toJsonString(msg.getVmInventory())));
        DestroyVmOnHypervisorReply reply = new DestroyVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(DetachVolumeFromVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully detached volume[uuid:%s] to vm[uuid:%s] on simulator host[uuid:%s]", msg.getInventory().getUuid(), msg.getVmInventory().getUuid(), host.getSimulatorHostVO().getUuid()));
        DetachVolumeFromVmOnHypervisorReply reply = new DetachVolumeFromVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(AttachVolumeToVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully attached volume[uuid:%s] to vm[uuid:%s] on simulator host[uuid:%s]", msg.getInventory().getUuid(), msg.getVmInventory().getUuid(), host.getSimulatorHostVO().getUuid()));
        AttachVolumeToVmOnHypervisorReply reply = new AttachVolumeToVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(MigrateVmOnHypervisorMsg msg, SimulatorHost host) {
        MigrateVmOnHypervisorReply reply = new MigrateVmOnHypervisorReply();
        if (!config.migrateSuccess) {
            reply.setError(errf.stringToOperationError("on purpose"));
        } else {
            logger.debug(String.format("Successfully migrate vm[uuid:%s] on simulator host[uuid:%s] to host[uuid:%s]", msg.getVmInventory().getUuid(), host.getSimulatorHostVO().getUuid(), msg.getDestHostInventory().getUuid()));
            synchronized (config) {
                config.removeVm(msg.getSrcHostUuid(), msg.getVmInventory().getUuid());
                config.putVm(msg.getDestHostInventory().getUuid(), msg.getVmInventory().getUuid(), VmInstanceState.Running);
            }
            bus.reply(msg, reply);

            /*
            // add vm on new simulator host to emulate migration
            ChangeVmStateOnSimulatorHostMsg cmsg = new ChangeVmStateOnSimulatorHostMsg();
            cmsg.setHostUuid(msg.getDestHostInventory().getUuid());
            cmsg.setVmUuid(msg.getInventory().getUuid());
            cmsg.setVmState(VmInstanceState.Running.toString());
            bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, cmsg.getHostUuid());
            bus.send(cmsg);
            */
        }

        bus.reply(msg, reply);
    }

    private void handle(StartVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully started vm on simulator host[uuid:%s], %s", host.getSimulatorHostVO().getUuid(), JSONObjectUtil.toJsonString(msg.getVmSpec())));
        host.setVmState(msg.getVmSpec().getVmInventory().getUuid(), VmInstanceState.Running);
        StartVmOnHypervisorReply reply = new StartVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(StopVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully stopped vm on simulator host[uuid:%s], %s", host.getSimulatorHostVO().getUuid(), JSONObjectUtil.toJsonString(msg.getVmInventory())));
        host.setVmState(msg.getVmInventory().getUuid(), VmInstanceState.Stopped);
        StopVmOnHypervisorReply reply = new StopVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(CreateVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully created vm on simulator host[uuid:%s], %s", host.getSimulatorHostVO().getUuid(), JSONObjectUtil.toJsonString(msg.getVmSpec())));
        host.setVmState(msg.getVmSpec().getVmInventory().getUuid(), VmInstanceState.Running);
        CreateVmOnHypervisorReply reply = new CreateVmOnHypervisorReply();
        bus.reply(msg, reply);
    }
    
    private void handle(RebootVmOnHypervisorMsg msg, SimulatorHost host) {
        logger.debug(String.format("Successfully rebooted vm on simulator host[uuid:%s], %s", host.getSimulatorHostVO().getUuid(), JSONObjectUtil.toJsonString(msg.getVmInventory())));
        host.setVmState(msg.getVmInventory().getUuid(), VmInstanceState.Running);
        RebootVmOnHypervisorReply reply = new RebootVmOnHypervisorReply();
        bus.reply(msg, reply);
    }
}
