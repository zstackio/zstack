package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.console.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SessionLogoutExtensionPoint;
import org.zstack.header.managementnode.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleManagerImpl extends AbstractService implements ConsoleManager, VmInstanceMigrateExtensionPoint, ManagementNodeChangeListener,
        VmReleaseResourceExtensionPoint, SessionLogoutExtensionPoint {
    private static CLogger logger = Utils.getLogger(ConsoleManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;

    private Map<String, ConsoleBackend> consoleBackends = new HashMap<String, ConsoleBackend>();
    private Map<String, ConsoleHypervisorBackend> consoleHypervisorBackends = new HashMap<String, ConsoleHypervisorBackend>();
    private String useBackend;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ConsoleProxyAgentMessage) {
            passThrough(msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(Message msg) {
        getBackend().handleMessage(msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIRequestConsoleAccessMsg) {
            handle((APIRequestConsoleAccessMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIRequestConsoleAccessMsg msg) {
        final APIRequestConsoleAccessEvent evt = new APIRequestConsoleAccessEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("request-console-for-vm-%s", msg.getVmInstanceUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                VmInstanceVO vmvo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                ConsoleBackend bkd = getBackend();
                bkd.grantConsoleAccess(msg.getSession(), VmInstanceInventory.valueOf(vmvo), new ReturnValueCompletion<ConsoleInventory>(chain) {
                    @Override
                    public void success(ConsoleInventory returnValue) {
                        if (!"0.0.0.0".equals(CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP) &&
                                !"".equals(CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP)) {
                            returnValue.setHostname(CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP);
                        } else {
                            returnValue.setHostname(CoreGlobalProperty.UNIT_TEST_ON ? "127.0.0.1" : Platform.getManagementServerIp());
                        }
                        evt.setInventory(returnValue);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        evt.setSuccess(false);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ConsoleConstants.SERVICE_ID);
    }

    private void populateExtensions() {
        for (ConsoleBackend bkd : pluginRgty.getExtensionList(ConsoleBackend.class)) {
            ConsoleBackend old = consoleBackends.get(bkd.getConsoleBackendType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ConsoleBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), old.getConsoleBackendType()));
            }
            consoleBackends.put(bkd.getConsoleBackendType(), bkd);
        }

        for (ConsoleHypervisorBackend bkd : pluginRgty.getExtensionList(ConsoleHypervisorBackend.class)) {
            ConsoleHypervisorBackend old = consoleHypervisorBackends.get(bkd.getConsoleBackendHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ConsoleHypervisorBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), bkd.getConsoleBackendHypervisorType()));
            }
            consoleHypervisorBackends.put(bkd.getConsoleBackendHypervisorType().toString(), bkd);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        clean();
        return true;
    }

    @Transactional
    public void clean() {
        String sql = "delete from ConsoleProxyVO";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.executeUpdate();
    }

    @Override
    public boolean stop() {
        return true;
    }

    private ConsoleBackend getBackend() {
        ConsoleBackend bkd = consoleBackends.get(useBackend);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("no plugin registered ConsoleBackend[type:%s]", useBackend));
        }
        return bkd;
    }

    public void setUseBackend(String useBackend) {
        this.useBackend = useBackend;
    }

    @Override
    public ConsoleHypervisorBackend getHypervisorConsoleBackend(HypervisorType type) {
        ConsoleHypervisorBackend bkd = consoleHypervisorBackends.get(type.toString());
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find ConsoleHypervisorBackend[type:%s]", type.toString()));
        }
        return bkd;
    }

    @Override
    public ConsoleBackend getConsoleBackend() {
        return getBackend();
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        ConsoleBackend bkd = getBackend();
        FutureCompletion completion = new FutureCompletion(null);
        bkd.deleteConsoleSession(inv, completion);
        try {
            synchronized (completion) {
                completion.wait(1500);
            }
        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {

    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, final Completion completion) {
        ConsoleBackend bkd = getBackend();
        bkd.deleteConsoleSession(spec.getVmInventory(), new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO
                logger.warn(errorCode.toString());
                completion.success();
            }
        });
    }

    @Override
    public void sessionLogout(final SessionInventory session) {
        ConsoleBackend bkd = getBackend();
        bkd.deleteConsoleSession(session, new NoErrorCompletion() {
            @Override
            public void done() {
                logger.debug(String.format("deleted all console proxy opened by the session[uuid:%s]", session.getUuid()));
            }
        });
    }

    @Transactional
    private void deleteConsoleProxyByManagementNode(ManagementNodeVO managementNode) {
        String managementHostName = managementNode.getHostName();
        String sql = "delete from ConsoleProxyVO q where q.proxyHostname = :managementHostName";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("managementHostName", managementHostName);
        q.executeUpdate();
    }

    public void cleanupNode(String nodeId) {
        logger.debug(String.format("Management node[uuid:%s] left, will clean the record in ConsoleProxyVO", nodeId));
        SimpleQuery<ManagementNodeVO> query = dbf.createQuery(ManagementNodeVO.class);
        query.add(ManagementNodeVO_.uuid, SimpleQuery.Op.EQ, nodeId);
        ManagementNodeVO managementNode = query.find();
        if (managementNode == null) {
            logger.debug("Cannot find management node: " + nodeId + ", it may have been deleted");
            return;
        }

        deleteConsoleProxyByManagementNode(managementNode);
    }

    @Override
    public void nodeLeft(String nodeId) {
        cleanupNode(nodeId);
    }

    @Override
    public void iAmDead(String nodeId) {
        cleanupNode(nodeId);
    }

    @Override
    public void nodeJoin(String nodeId) {
    }

    @Override
    public void iJoin(String nodeId) {
    }
}
