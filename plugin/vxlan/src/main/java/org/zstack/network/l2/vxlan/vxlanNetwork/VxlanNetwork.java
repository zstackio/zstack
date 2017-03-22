package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by weiwang on 01/03/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VxlanNetwork extends L2NoVlanNetwork {
    private static final CLogger logger = Utils.getLogger(VxlanNetwork.class);

    @Autowired
    protected L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L2NetworkManager l2Mgr;
    @Autowired
    protected InventoryFacade inventoryMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;

    public VxlanNetwork(L2NetworkVO self) {
        super(self);
    }

    private L2NetworkVO getSelf() {
        return (L2NetworkVO) self;
    }

    @Override
    public void deleteHook() {
    }

    protected L2NetworkInventory getSelfInventory() {
        return L2NetworkInventory.valueOf(self);
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CheckL2NetworkOnHostMsg) {
            handle((CheckL2NetworkOnHostMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
    }

    private void handle(final CheckL2NetworkOnHostMsg msg) {
    }

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
        extpEmitter.afterDelete(inv);

        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        throw new CloudRuntimeException("VxlanNetwork can not detach from cluster which VxlanNetworkPool should be used");
    }

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        throw new CloudRuntimeException("VxlanNetwork can not attach to cluster which VxlanNetworkPool should be used");
    }

    private void superHandle(L2NetworkMessage msg) {
        super.handleMessage((Message) msg);
    }
}
