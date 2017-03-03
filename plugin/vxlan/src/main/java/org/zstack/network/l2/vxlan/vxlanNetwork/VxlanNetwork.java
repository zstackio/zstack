package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by weiwang on 01/03/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VxlanNetwork implements L2Network{
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

    protected L2NetworkVO self;

    public VxlanNetwork(L2NetworkVO self) {
        this.self = self;
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
        if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CheckL2NetworkOnHostMsg) {
            handle((CheckL2NetworkOnHostMsg) msg);
        } else if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof DetachL2NetworkFromClusterMsg) {
            handle((DetachL2NetworkFromClusterMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DetachL2NetworkFromClusterMsg msg) {
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
    }

    private void handle(final CheckL2NetworkOnHostMsg msg) {
    }

    private void handle(L2NetworkDeletionMsg msg) {
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkMsg) {
            handle((APIUpdateL2NetworkMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateL2NetworkMsg msg) {
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
    }

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
    }

    private void handle(APIDeleteL2NetworkMsg msg) {
    }

}
