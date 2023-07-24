package org.zstack.kvm.xmlhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.directory.DirectoryMessage;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.Date;

import static org.zstack.core.Platform.err;

public class XmlHookManagerImpl extends AbstractService implements XmlHookManager, Component {
    private static final CLogger logger = Utils.getLogger(XmlHookManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof XmlHookMessage) {
            passThrough((XmlHookMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVmUserDefinedXmlHookScriptMsg) {
            handle((APICreateVmUserDefinedXmlHookScriptMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(XmlHookMessage msg) {
        XmlHookVO vo = dbf.findByUuid(msg.getXmlHookUuid(), XmlHookVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find xmlHook[uuid=%s]", msg.getXmlHookUuid()));
            return;
        }

        XmlHookBase base = new XmlHookBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateVmUserDefinedXmlHookScriptMsg msg) {
        APICreateVmUserDefinedXmlHookScriptEvent event = new APICreateVmUserDefinedXmlHookScriptEvent(msg.getId());
        XmlHookVO vo = new XmlHookVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setHookScript(msg.getHookScript());
        vo.setType(XmlHookType.Customization);
        vo.setCreateDate(new Timestamp(new Date().getTime()));
        dbf.persist(vo);
        event.setInventory(XmlHookInventory.valueOf(vo));
        bus.publish(event);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
