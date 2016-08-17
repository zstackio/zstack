package org.zstack.core.keystore;

import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.keystore.KeystoreInventory;
import org.zstack.header.core.keystore.KeystoreVO;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;

/**
 * Created by miao on 16-8-15.
 */
public class KeystoreManagerImpl extends AbstractService implements KeystoreManager, ManagementNodeReadyExtensionPoint {

    private static final CLogger logger = Utils.getLogger(KeystoreManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    private KeystoreManager keystoreManager;

    protected KeystoreVO self;

    protected KeystoreInventory getInventory() {
        return KeystoreInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }


    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateKeystoreMsg) {
            handle((CreateKeystoreMsg) msg);
        } else if (msg instanceof DeleteKeystoreMsg) {
            handle((DeleteKeystoreMsg) msg);
        } else if (msg instanceof QueryKeystoreMsg) {
            handle((QueryKeystoreMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateKeystoreMsg) {
            handle((APICreateKeystoreMsg) msg);
        } else if (msg instanceof APIDeleteKeystoreMsg) {
            handle((APIDeleteKeystoreMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional
    private void handle(APICreateKeystoreMsg msg) {
        APICreateKeystoreReply reply = new APICreateKeystoreReply();

        KeystoreVO vo = new KeystoreVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setResourceUuid(msg.getResourceUuid());
        vo.setResourceType(msg.getResourceType());
        vo.setType(msg.getType());
        vo.setContent(msg.getContent());
        dbf.getEntityManager().persist(vo);

        bus.reply(msg, reply);
    }

    private void handle(APIDeleteKeystoreMsg msg) {
        APIDeleteKeystoreEvent evt = new APIDeleteKeystoreEvent(msg.getId());
        deleteKeystore(msg.getUuid());
        bus.publish(evt);
    }

    @Transactional
    private void handle(CreateKeystoreMsg msg) {
        CreateKeystoreReply reply = new CreateKeystoreReply();

        KeystoreVO vo = new KeystoreVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setResourceUuid(msg.getResourceUuid());
        vo.setResourceType(msg.getResourceType());
        vo.setType(msg.getType());
        vo.setContent(msg.getContent());
        dbf.getEntityManager().persist(vo);

        bus.reply(msg, reply);
    }

    @Transactional
    private void handle(QueryKeystoreMsg msg) {
        QueryKeystoreReply reply = new QueryKeystoreReply();
        KeystoreInventory kinv = new KeystoreInventory();
        if (msg.getUuid() != null) {
            self = dbf.findByUuid(msg.getUuid(), KeystoreVO.class);
        } else {
            String sql = "select uuid from KeystoreVO where resourceUuid = :resourceUuid " +
                    " and resourceType = :resourceType and type = :keystoreType";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("resourceUuid", msg.getResourceUuid());
            q.setParameter("resourceType", msg.getResourceType());
            q.setParameter("keystoreType", msg.getType());
            String uuid = dbf.find(q);

            if (uuid != null) {
                self = dbf.findByUuid(uuid, KeystoreVO.class);
            }
        }

        if (self == null) {
            bus.reply(msg, reply);
        } else {
            kinv.setUuid(self.getUuid());
            kinv.setResourceUuid(self.getResourceUuid());
            kinv.setResourceType(self.getResourceType());
            kinv.setType(self.getType());
            kinv.setContent(self.getContent());
            reply.setInventory(kinv);
        }

    }

    private void handle(DeleteKeystoreMsg msg) {
        final DeleteKeystoreReply reply = new DeleteKeystoreReply();
        deleteKeystore(msg.getUuid());
        bus.reply(msg, reply);
    }

    @Transactional
    private void deleteKeystore(String uuid) {
        self = dbf.findByUuid(uuid, KeystoreVO.class);
        dbf.remove(self);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(KeystoreConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] joins, start KeystoreManager...",
                Platform.getManagementServerId()));
    }
}
