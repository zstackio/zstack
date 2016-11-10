package org.zstack.portal.managementnode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.*;
import org.zstack.header.managementnode.ManagementNodeCanonicalEvent.LifeCycle;
import org.zstack.header.managementnode.ManagementNodeCanonicalEvent.ManagementNodeLifeCycleData;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.portal.apimediator.ApiMediator;
import org.zstack.utils.BootErrorLog;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.utils.ExceptionDSL.throwableSafe;

public class ManagementNodeManagerImpl extends AbstractService implements ManagementNodeManager {
    private static final CLogger logger = Utils.getLogger(ManagementNodeManager.class);

    private List<ComponentWrapper> components;
    private List<PrepareDbInitialValueExtensionPoint> prepareDbExts;
    private ManagementNodeVO node;
    private volatile boolean isRunning = true;
    private volatile int isNodeRunning = NODE_STARTING;
    private static final String INVENTORY_LOCK = "ManagementNodeManager.inventory_lock";
    private final int INVENTORY_LOCK_TIMEOUT = 600; /* 10 mins */
    private static boolean started = false;
    private static boolean stopped = false;
    private Future<Void> heartBeatTask = null;
    private HeartBeatDBSource heartBeatDBSource;
    private List<ManagementNodeChangeListener> lifeCycleExtension = new ArrayList<ManagementNodeChangeListener>();

    private static int NODE_STARTING = 0;
    private static int NODE_RUNNING = 1;
    private static int NODE_FAILED = -1;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBusIN bus;
    @Autowired
    private ApiMediator apim;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private EventFacade evtf;

    void init() {
        heartBeatDBSource = new HeartBeatDBSource();
    }

    private ManagementNodeChangeListener nodeLifeCycle = new ManagementNodeChangeListener() {
        @Override
        public void nodeJoin(String nodeId) {
            if (destinationMaker.getManagementNodesInHashRing().contains(nodeId)) {
                logger.debug(String.format("the management node[uuid:%s] is already in our hash ring, ignore this node-join call", nodeId));
                return;
            }

            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.nodeJoin(nodeId);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.nodeJoin(nodeId);
                }
            });
        }

        @Override
        public void nodeLeft(String nodeId) {
            if (!destinationMaker.getManagementNodesInHashRing().contains(nodeId)) {
                logger.debug(String.format("the management node[uuid:%s] is not in our hash ring, ignore this node-left call", nodeId));
                return;
            }

            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.nodeLeft(nodeId);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.nodeLeft(nodeId);
                }
            });
        }

        @Override
        public void iAmDead(String nodeId) {
            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.iAmDead(nodeId);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.iAmDead(nodeId);
                }
            });
        }

        @Override
        public void iJoin(String nodeId) {
            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.iJoin(nodeId);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.iJoin(nodeId);
                }
            });
        }
    };

    private interface ComponentWrapper {
        void start();

        void stop();
    }

    private void notifyStop() {
        isRunning = false;
        synchronized (this) {
            this.notify();
        }
    }

    private void handle(ManagementNodeExitMsg msg) {
        logger.debug(getId() + " received ManagementNodeExitMsg, going to exit");
        notifyStop();
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

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIListManagementNodeMsg) {
            handle((APIListManagementNodeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIListManagementNodeMsg msg) {
        List<ManagementNodeVO> vos = dbf.listAll(ManagementNodeVO.class);
        APIListManagementNodeReply reply = new APIListManagementNodeReply();
        reply.setInventories(ManagementNodeInventory.valueOf(vos));
        bus.reply(msg, reply);
    }

    private void handleLocalMessage(Message msg) {
        if (msg.getClass() == ManagementNodeExitMsg.class) {
            handle((ManagementNodeExitMsg) msg);
        } else if (msg instanceof IsManagementNodeReadyMsg) {
            handle((IsManagementNodeReadyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(IsManagementNodeReadyMsg msg) {
        IsManagementNodeReadyReply reply = new IsManagementNodeReadyReply();
        reply.setReady(isNodeRunning == NODE_RUNNING);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ManagementNodeConstant.SERVICE_ID);
    }


    private void startComponents() {
        for (ComponentWrapper c : components) {
            c.start();
        }
    }

    private void stopComponents() {
        for (final ComponentWrapper c : components) {
            try {
                c.stop();
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }

    private void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                logger.debug("JVM shutdown hook is called, start stopping management node");
                stop();
            }
        }));
    }

    private void populateComponents() {
        components = new ArrayList<ComponentWrapper>();
        for (final Component c : pluginRgty.getExtensionList(Component.class)) {
            components.add(new ComponentWrapper() {
                boolean isStart = false;

                @Override
                public void start() {
                    logger.info("starting component: " + c.getClass().getName());
                    c.start();
                    logger.info(String.format("component[%s] starts successfully", c.getClass()));
                    isStart = true;
                }

                @Override
                public void stop() {
                    if (isStart) {
                        throwableSafe(new Runnable() {
                            @Override
                            public void run() {
                                c.stop();
                                logger.info("Stopped component: " + c.getClass().getName());
                                isStart = false;
                            }
                        }, String.format("unable to stop component[%s]", c.getClass().getName()));
                    }
                }
            });
        }

        prepareDbExts = pluginRgty.getExtensionList(PrepareDbInitialValueExtensionPoint.class);
    }

    private void callPrepareDbExtensions() {
        for (PrepareDbInitialValueExtensionPoint extp : prepareDbExts) {
            extp.prepareDbInitialValue();
        }
    }

    private void populateExtensions() {
        lifeCycleExtension = pluginRgty.getExtensionList(ManagementNodeChangeListener.class);
    }

    private EventCallback nodeLifeCycleCallback = new EventCallback() {
        @Override
        protected void run(Map tokens, Object data) {
            if (evtf.isFromThisManagementNode(tokens)) {
                return;
            }

            ManagementNodeLifeCycleData d = (ManagementNodeLifeCycleData) data;

            if (LifeCycle.NodeJoin.toString().equals(d.getLifeCycle())) {
                nodeLifeCycle.nodeJoin(d.getNodeUuid());
            } else if (LifeCycle.NodeLeft.toString().equals(d.getLifeCycle())) {
                nodeLifeCycle.nodeLeft(d.getNodeUuid());
            } else {
                throw new CloudRuntimeException(String.format("unknown lifecycle[%s]", d.getLifeCycle()));
            }
        }
    };

    @Override
    public boolean start() {
        if (started) {
            /* largely for unittest, the ComponentLoaderWebListener and Api may both call start()
	         */
            logger.debug("Management Node has already started, ignore this call");
            return true;
        }

        populateExtensions();

        started = true;
        stopped = true;

        class Result {
            boolean success;
        }

        final Result ret = new Result();

        GLock lock = new GLock(INVENTORY_LOCK, INVENTORY_LOCK_TIMEOUT);
		/*
	     * The lock is being held until we join in, otherwise the inventory
	     * may be deleted by other exiting node because we have not
		 * persisted our entry in management_node table yet, or two starting
		 * nodes persist inventory concurrently.
	     */
        lock.lock();
        try {
            final ManagementNodeManagerImpl self = this;
            FlowChain bootstrap = FlowChainBuilder.newSimpleFlowChain();
            bootstrap.setName("management-node-bootstrap");
            bootstrap.then(new Flow() {
                String __name__ = "bootstrap-cloudbus";

                // CloudBus is special, it is initialized in Platform.createComponentLoaderFromWebApplicationContext(),
                // however, when exception happens in bootstrap we need to stop bus in rollback, because the exception
                // cannot make JVM exist and cloudbus.stop is only called in JVM exit hook;
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    bus.stop();
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "populate-components";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    populateComponents();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "register-node-on-cloudbus";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    bus.registerService(self);
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    bus.unregisterService(self);
                    trigger.rollback();
                }
            }).then(new Flow() {
                String __name__ = "start-components";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    startComponents();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    stopComponents();
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "call-prepare-db-extension";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    callPrepareDbExtensions();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "create-DB-record";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    ManagementNodeVO vo = new ManagementNodeVO();
                    vo.setHostName(Platform.getManagementServerIp());
                    vo.setUuid(Platform.getManagementServerId());
                    node = dbf.persistAndRefresh(vo);
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (node != null) {
                        dbf.remove(node);
                    }

                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "start-heartbeat";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    setupHeartbeat();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "start-api-mediator";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    apim.start();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    apim.stop();
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "set-node-to-running";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    node.setState(ManagementNodeState.RUNNING);
                    node = dbf.updateAndRefresh(node);
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "I-join";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    nodeLifeCycle.iJoin(node.getUuid());
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "node-is-ready";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    for (ManagementNodeReadyExtensionPoint ext : pluginRgty.getExtensionList(ManagementNodeReadyExtensionPoint.class)) {
                        ext.managementNodeReady();
                    }

                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "listen-node-life-cycle-events";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    evtf.on(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, nodeLifeCycleCallback);
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "say-I-join";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    ManagementNodeLifeCycleData d = new ManagementNodeLifeCycleData();
                    d.setNodeUuid(node.getUuid());
                    d.setInventory(ManagementNodeInventory.valueOf(node));
                    d.setLifeCycle(LifeCycle.NodeJoin.toString());
                    evtf.fire(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, d);
                    trigger.next();
                }
            }).done(new FlowDoneHandler() {
                @Override
                public void handle(Map data) {
                    ret.success = true;
                }
            }).error(new FlowErrorHandler() {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    new BootErrorLog().write(errCode.toString());
                    ret.success = false;
                }
            }).start();
        } finally {
            lock.unlock();
        }

        if (!ret.success) {
            logger.warn(String.format("management node[%s] failed to start for some reason", Platform.getUuid()));
            stopped = true;

            if (CoreGlobalProperty.EXIT_JVM_ON_BOOT_FAILURE) {
                logger.debug(String.format("unable to start management node[%s], see previous exception. exitJVMOnBootFailure is set to true, exit JVM now", Platform.getManagementServerId()));
                System.exit(1);
            } else {
                throw new CloudRuntimeException(String.format("unable to start management node[%s], see previous exception", Platform.getManagementServerId()));
            }
        }

        stopped = false;

        installShutdownHook();


        logger.info("Management node: " + getId() + " starts successfully");

        synchronized (this) {
            isNodeRunning = NODE_RUNNING;
            while (isRunning) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while daemon is running, continue ...", e);
                }
            }
        }

        logger.debug("quited main-loop, start stopping management node");
        stop();
        return true;
    }

    private void setupHeartbeat() {
        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startHeartbeat();
            }
        });

        startHeartbeat();
    }

    private class HeartBeatDBSource {
        private Connection conn;
        private SingleConnectionDataSource source;
        JdbcTemplate jdbc;
        AtomicBoolean destroyed = new AtomicBoolean(false);

        public HeartBeatDBSource() {
            try {
                conn = dbf.getExtraDataSource().getConnection();
                source = new SingleConnectionDataSource(conn, true);
                jdbc = new JdbcTemplate(source);
            } catch (SQLException e) {
                throw new CloudRuntimeException(e);
            }
        }

        @ExceptionSafe
        public void destroy() {
            if (!destroyed.compareAndSet(false, true)) {
                return;
            }

            source.destroy();
            try {
                conn.close();
            } catch (SQLException e) {
                throw new CloudRuntimeException(e);
            }
        }
    }


    private void startHeartbeat() {
        if (heartBeatTask != null) {
            heartBeatTask.cancel(true);
        }

        heartBeatTask = thdf.submit(new Task<Void>() {
            private List<ManagementNodeVO> suspects = new ArrayList<ManagementNodeVO>();

            @Override
            public String getName() {
                return String.format("managementNode-%s-heartbeat", Platform.getManagementServerId());
            }

            private boolean amIalive() {
                String sql = "select count(*) from ManagementNodeVO where uuid = ?";
                long count = heartBeatDBSource.jdbc.queryForObject(sql, new Object[]{node.getUuid()}, Long.class);
                return count != 0;
            }

            private ManagementNodeVO getNode(String uuid) {
                try {
                    String sql = "select * from ManagementNodeVO where uuid = ?";
                    return (ManagementNodeVO) heartBeatDBSource.jdbc.queryForObject(sql, new Object[]{uuid}, new BeanPropertyRowMapper(ManagementNodeVO.class));
                } catch (IncorrectResultSizeDataAccessException e) {
                    return null;
                }
            }

            private int deleteNode(String uuid) {
                String sql = "delete from ManagementNodeVO where uuid = ?";
                return heartBeatDBSource.jdbc.update(sql, uuid);
            }

            @AsyncThread
            private void nodeDie(ManagementNodeVO n) {
                logger.debug("Node " + n.getUuid() + " has gone because its heartbeat stopped");
                nodeLifeCycle.nodeLeft(n.getUuid());

                ManagementNodeLifeCycleData d = new ManagementNodeLifeCycleData();
                d.setInventory(ManagementNodeInventory.valueOf(n));
                d.setNodeUuid(n.getUuid());
                d.setLifeCycle(LifeCycle.NodeLeft.toString());
                evtf.fire(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, d);
            }

            private void fenceSuspects() {
                for (ManagementNodeVO vo : suspects) {
                    ManagementNodeVO n = getNode(vo.getUuid());
                    if (n == null) {
                        continue;
                    }

                    long elapse = n.getHeartBeat().getTime() - vo.getHeartBeat().getTime();
                    if (elapse != 0) {
                        continue;
                    }

                    int ret = deleteNode(n.getUuid());
                    if (ret > 0) {
                        nodeDie(n);
                    }
                }
            }

            private void updateHeartbeat() {
                String sql = "update ManagementNodeVO set heartBeat = NULL where uuid = ?";
                if (heartBeatDBSource.jdbc.update(sql, node.getUuid()) > 0) {
                    node = getNode(node.getUuid());
                }
            }

            private void checkAllNodesHealth() {
                String sql = "select * from ManagementNodeVO where state = 'RUNNING'";
                List<ManagementNodeVO> all = heartBeatDBSource.jdbc.query(sql, new BeanPropertyRowMapper(ManagementNodeVO.class));
                suspects.clear();

                List<String> nodesInDb = new ArrayList<String>();
                for (ManagementNodeVO vo : all) {
                    if (!StringDSL.isZstackUuid(vo.getUuid())) {
                        logger.warn(String.format("found a weird management node, it's UUID not a ZStack uuid, delete it. %s",
                                JSONObjectUtil.toJsonString(ManagementNodeInventory.valueOf(vo))));
                        dbf.remove(vo);
                        continue;
                    }

                    nodesInDb.add(vo.getUuid());

                    if (vo.getUuid().equals(node.getUuid())) {
                        continue;
                    }

                    Timestamp curr = dbf.getCurrentSqlTime();
                    Timestamp lastHeartbeat = vo.getHeartBeat();
                    long end = lastHeartbeat.getTime() + TimeUnit.SECONDS.toMillis(2 * ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class));
                    if (end < curr.getTime()) {
                        suspects.add(vo);
                        logger.warn(String.format("management node[uuid:%s, hostname: %s]'s heart beat has stopped for %s secs, add it in suspicious list",
                                vo.getUuid(), vo.getHostName(), TimeUnit.MILLISECONDS.toSeconds(curr.getTime() - lastHeartbeat.getTime())));
                    }
                }

                // When a node is dying, we may not receive the the dead notification because the message bus may be also dead
                // at that moment. By checking if the node UUID is still in our hash ring, we know what nodes should be kicked out
                for (String ourNode : destinationMaker.getManagementNodesInHashRing()) {
                    if (!nodesInDb.contains(ourNode)) {
                        logger.warn(String.format("found that a management node[uuid:%s] had no heartbeat in database but still in our hash ring," +
                                "notify that it's dead", ourNode));
                        nodeLifeCycle.nodeLeft(ourNode);
                    }
                }
            }

            @Override
            public Void call() throws Exception {
                int heartbeatFailure = 0;

                while (true) {
                    try {
                        if (!amIalive()) {
                            logger.warn(String.format("cannot find my[uuid:%s] heartbeat in database, quit process", node.getUuid()));
                            stop();
                            return null;
                        } else {
                            fenceSuspects();
                            updateHeartbeat();
                            checkAllNodesHealth();
                        }

                        heartbeatFailure = 0;
                    } catch (Throwable t) {
                        heartbeatFailure++;

                        if (heartbeatFailure > PortalGlobalProperty.MAX_HEARTBEAT_FAILURE) {
                            logger.warn(String.format("the heartbeat has failed %s times that is greater than the max allowed value[%s]," +
                                    " quit process", heartbeatFailure, PortalGlobalProperty.MAX_HEARTBEAT_FAILURE));
                            stop();
                            return null;
                        }

                        boolean databaseError = false;

                        logger.warn(String.format("an error happened when doing heartbeat, %s, it's going to recover", t.getMessage()));

                        try {
                            heartBeatDBSource.jdbc.queryForObject("select 1", Integer.class);
                        } catch (Throwable t1) {
                            logger.warn(String.format("cannot communicate to the database, it's most likely the DB stopped or rebooted;" +
                                    "try creating a new database connection. %s", t1.getMessage()), t1);
                            databaseError = true;
                        }

                        if (databaseError) {
                            if (heartBeatDBSource != null) {
                                heartBeatDBSource.destroy();
                            }

                            try {
                                heartBeatDBSource = new HeartBeatDBSource();
                            } catch (Throwable t1) {
                                logger.warn(String.format("unable to create a database connection, %s, will try it later", t1.getMessage()), t1);
                            }
                        } else {
                            logger.warn("unhandled exception happened", t);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Long.class));
                    } catch (InterruptedException ie) {
                    }

                    if (heartBeatTask.isCancelled()) {
                        break;
                    }
                }

                return null;
            }
        });

        logger.debug(String.format("started heartbeat thread for management node[uuid:%s]", Platform.getManagementServerId()));
    }

    @Override
    public boolean stop() {
        Platform.IS_RUNNING = false;

        if (stopped) {
	        /* avoid repeated call from JVM shutdown hook, if process is exited from a former stop() call
	         */
            return true;
        }

        stopped = true;
        final Service self = this;
        logger.debug(String.format("start stopping the management node[uuid:%s]", node.getUuid()));

        class Stopper {
            void stop() {
                stopApiOnCloudBus();
                stopApi();
                iAmDead();
                stopComponents();
                deleteNode();
                notifyOtherNodes();
                unregisterCloudBus();
                stopCloudBus();
                quitLoop();
                stopThreadFacade();
            }

            @ExceptionSafe
            private void stopThreadFacade() {
                thdf.stop();
            }

            @ExceptionSafe
            private void quitLoop() {
                notifyStop();
            }

            @ExceptionSafe
            private void stopCloudBus() {
                bus.stop();
            }

            @ExceptionSafe
            private void unregisterCloudBus() {
                bus.unregisterService(self);
            }

            @ExceptionSafe
            private void notifyOtherNodes() {
                ManagementNodeLifeCycleData d = new ManagementNodeLifeCycleData();
                d.setNodeUuid(node.getUuid());
                d.setLifeCycle(LifeCycle.NodeLeft.toString());
                d.setInventory(ManagementNodeInventory.valueOf(node));
                evtf.fire(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, d);
            }

            @ExceptionSafe
            private void deleteNode() {
                dbf.remove(node);
            }

            @ExceptionSafe
            private void iAmDead() {
                nodeLifeCycle.iAmDead(node.getUuid());
            }

            @ExceptionSafe
            private void stopApiOnCloudBus() {
                bus.unregisterService(apim);
            }

            @ExceptionSafe
            private void stopApi() {
                apim.stop();
            }
        }

        new Stopper().stop();

        logger.info("Management node: " + getId() + " exits successfully");
        if (CoreGlobalProperty.EXIT_JVM_ON_STOP) {
            logger.info("exitJVMOnStop is set to true, exit the JVM");
            System.exit(0);
        }

        return true;
    }

    @AsyncThread
    private void startInThread() {
        try {
            start();
            isNodeRunning = NODE_RUNNING;
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            isNodeRunning = NODE_FAILED;
        }
    }

    @Override
    public void startNode() {
        startInThread();
        while (isNodeRunning == NODE_STARTING) {
            logger.debug("management node is still initializing ...");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new CloudRuntimeException(e);
            }
        }

        if (isNodeRunning == NODE_FAILED) {
            logger.debug(String.format("error happened when starting node, stop the management node now"));
            stop();
            throw new CloudRuntimeException("failed to start management node");
        }
    }

    @Override
    public void quit(String reason) {
        logger.debug(String.format("stopping the management node because %s", reason));
        stop();
    }
}
