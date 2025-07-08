package org.zstack.resourceattribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.header.resourceattribute.AttributeConstant;
import org.zstack.header.resourceattribute.AttributeErrors;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyEvent;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;

public class ResourceAttributeManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ResourceAttributeManager.class);

    private final Object createLock = new Object();

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade databaseFacade;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ResourceAttributeMessage) {
            passThrough((ResourceAttributeMessage) msg);
        } else if (msg instanceof APICreateResourceAttributeKeyMsg) {
            handle((APICreateResourceAttributeKeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(ResourceAttributeMessage msg) {
        ResourceAttributeKeyVO vo = databaseFacade.findByUuid(msg.getKeyUuid(), ResourceAttributeKeyVO.class);

        if (vo == null) {
            String err = String.format("Cannot find ResourceAttributeKeyVO[uuid:%s], it may have been deleted",
                    msg.getKeyUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        ResourceAttributeBase base = new ResourceAttributeBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateResourceAttributeKeyMsg msg) {
        APICreateResourceAttributeKeyEvent event = new APICreateResourceAttributeKeyEvent(msg.getId());

        ResourceAttributeKeyVO key = new ResourceAttributeKeyVO();
        key.setUuid(msg.getResourceUuid());
        key.setName(msg.getName());
        key.setDescription(msg.getDescription());

        boolean duplicate;
        synchronized (createLock) {
            duplicate = Q.New(ResourceAttributeKeyVO.class)
                    .eq(ResourceAttributeKeyVO_.name, msg.getName())
                    .isExists();
            if (!duplicate) {
                databaseFacade.persistAndRefresh(key);
            }
        }

        if (duplicate) {
            event.setError(err(AttributeErrors.DUPLICATED_ATTRIBUTE,
                    "duplicate resource attribute key name[%s]", msg.getName()));
        } else {
            event.setInventory(ResourceAttributeKeyInventory.valueOf(key));
        }
        bus.publish(event);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AttributeConstant.SERVICE_ID);
    }
}
