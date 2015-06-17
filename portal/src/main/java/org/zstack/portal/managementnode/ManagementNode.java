package org.zstack.portal.managementnode;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.EventSubscriberReceipt;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.safeguard.Guard;
import org.zstack.core.safeguard.SafeGuard;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.*;
import org.zstack.header.managementnode.ManagementNodeExitMsg.Reason;
import org.zstack.header.message.Event;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ManagementNode implements CloudBusEventListener {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBusIN bus;

    private static final CLogger logger = Utils.getLogger(ManagementNode.class);
    private Event[] myEvents;
    private List<ManagementNodeChangeListener> listeners = new ArrayList<ManagementNodeChangeListener>();

    private volatile ManagementNodeVO node = null;
    private Future<Void> heartBeatTask = null;
    private volatile AtomicBoolean isLeft = new AtomicBoolean(false);

    public static final String MANAGEMENT_NODE_EVENT = "MANAGEMENT_NODE_EVENT";

    private JdbcTemplate jdbc;
    private Connection heartbeatDbConnection;
    private EventSubscriberReceipt unsubscriber;

    public ManagementNode() {
        myEvents = new Event[] { new ManagementNodeJoinEvent(), new ManagementNodeLeftEvent(), };
        try {
            heartbeatDbConnection = dbf.getExtraDataSource().getConnection();
            SingleConnectionDataSource dbSource = new SingleConnectionDataSource(heartbeatDbConnection, true);
            jdbc = new JdbcTemplate(dbSource);
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void setNodeRunning() {
        node.setState(ManagementNodeState.RUNNING);
        node = dbf.updateAndRefresh(node);
    }

    private void notifyIJoin() {
        for (ManagementNodeChangeListener l : listeners) {
            try {
                l.iJoin(node.getUuid());
            } catch (Exception e) {
                logger.warn("Exception happened while notifying listener " + l.getClass().getCanonicalName() + " that I joined", e);
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
                long count = jdbc.queryForObject(sql, new Object[]{node.getUuid()}, Long.class);
                return count != 0;
            }

            private ManagementNodeVO getNode(String uuid) {
                try {
                    String sql = "select * from ManagementNodeVO where uuid = ?";
                    ManagementNodeVO vo = (ManagementNodeVO) jdbc.queryForObject(sql, new Object[]{uuid}, new BeanPropertyRowMapper(ManagementNodeVO.class));
                    return vo;
                } catch (IncorrectResultSizeDataAccessException e) {
                    return null;
                }
            }

            private int deleteNode(String uuid) {
                String sql = "delete from ManagementNodeVO where uuid = ?";
                return jdbc.update(sql, uuid);
            }

            private void fenceSuspects() {
                for (ManagementNodeVO vo : suspects) {
                    ManagementNodeVO n =  getNode(vo.getUuid());
                    if (n == null) {
                        continue;
                    }

                    long elapse = n.getHeartBeat().getTime() - vo.getHeartBeat().getTime();
                    if (elapse != 0) {
                        continue;
                    }

                    int ret = deleteNode(n.getUuid());
                    if (ret > 0) {
                        ManagementNodeLeftEvent evt = new ManagementNodeLeftEvent(n.getUuid(), node.getUuid(), true);
                        bus.publish(evt);
                        logger.debug("Node " + n.getUuid() + " has gone");
                    }
                }
            }

            private void updateHeartbeat() {
                String sql = "update ManagementNodeVO set heartBeat = NULL where uuid = ?";
                if (jdbc.update(sql, node.getUuid()) > 0) {
                    node = getNode(node.getUuid());
                }
            }

            private void checkAllNodesHealth() {
                String sql = "select * from ManagementNodeVO where state = 'RUNNING'";
                List<ManagementNodeVO> all = jdbc.query(sql, new BeanPropertyRowMapper(ManagementNodeVO.class));
                suspects.clear();
                for (ManagementNodeVO vo : all) {
                    if (vo.getUuid().equals(node.getUuid())) {
                        continue;
                    }

                    Timestamp curr = getCurrentSqlTime();
                    Timestamp lastHeartbeat = vo.getHeartBeat();
                    long end = lastHeartbeat.getTime() + TimeUnit.SECONDS.toMillis(2 * ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class));
                    if (end < curr.getTime()) {
                        suspects.add(vo);
                        logger.warn(String.format("management node[uuid:%s, hostname: %s]'s heart beat has stopped for %s secs, add it in suspicious list",
                                vo.getUuid(), vo.getHostName(), TimeUnit.MILLISECONDS.toSeconds(curr.getTime() - lastHeartbeat.getTime())));
                    }
                }
            }

            @Override
            public Void call() throws Exception {
                while (true) {
                    try {
                        if (!amIalive()) {
                            logger.warn(String.format("cannot find my[uuid:%s] heartbeat in database, quit process", node.getUuid()));
                            ManagementNodeExitMsg msg = new ManagementNodeExitMsg();
                            msg.setServiceId(bus.makeLocalServiceId(ManagementNodeConstant.SERVICE_ID));
                            msg.setReason(Reason.HeartBeatStopped);
                            bus.send(msg);
                        } else {
                            fenceSuspects();
                            updateHeartbeat();
                            checkAllNodesHealth();
                        }
                    } catch (Throwable t) {
                        logger.warn("unhandled exception happened", t);
                    }

                    try {
                        TimeUnit.SECONDS.sleep(ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class));
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

    private void setupHeartbeat() {
        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startHeartbeat();
            }
        });

        startHeartbeat();
    }

    @Guard
    public String join() {
        try {
            if (node == null) {
                final ManagementNodeVO vo = new ManagementNodeVO();
                vo.setHostName(Platform.getManagementServerIp());
                vo.setUuid(Platform.getManagementServerId());
                node = dbf.persistAndRefresh(vo);
                SafeGuard.guard(new Runnable() {
                    @Override
                    public void run() {
                        deleteNode(vo.getUuid());
                        logger.debug(node.getUuid() + " fails to join, delete from database");
                    }
                });

                unsubscriber = bus.subscribeEvent(this, myEvents);
                final ManagementNode grid = this;
                SafeGuard.guard(new Runnable() {
                    @Override
                    public void run() {
                        unsubscriber.unsubscribeAll();
                        logger.debug(node.getUuid() + " fails to join, unregister from CloudBus");
                    }
                });

                int delay = ManagementNodeGlobalConfig.NODE_JOIN_DELAY.value(Integer.class);
                if (delay != 0) {
                    try {
                        TimeUnit.SECONDS.sleep(delay);
                    } catch (InterruptedException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }

                setupHeartbeat();
                setNodeRunning();
                
                notifyIJoin();
                ManagementNodeJoinEvent evt = new ManagementNodeJoinEvent(node.getUuid());
                bus.publish(evt);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to join into cluster", e);
        }

        return node.getUuid();
    }

    private List<ManagementNodeVO> getRunningNodes() {
        SimpleQuery<ManagementNodeVO> q = dbf.createQuery(ManagementNodeVO.class);
        q.add(ManagementNodeVO_.state, Op.EQ, ManagementNodeState.RUNNING);
        List<ManagementNodeVO> vos = q.list();
        return vos;
    }

    public List<String> getNodes() {
        List<ManagementNodeVO> vos = getRunningNodes();
        List<String> ids = new ArrayList<String>(vos.size());
        for (ManagementNodeVO vo : vos) {
            ids.add(vo.getUuid());
        }
        return ids;
    }

    @Transactional
    private Timestamp getCurrentSqlTime() {
        Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
        return (Timestamp) query.getSingleResult();
    }


    @Transactional
    private int deleteNode(String uuid) {
        String sql = "delete from ManagementNodeVO m where m.uuid = :uuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuid", uuid);
        return q.executeUpdate();
    }

    private void leave(boolean isNormalleave) {
        if (!isLeft.compareAndSet(false, true)) {
            return;
        }

        try {
            if (isNormalleave) {
                deleteNode(node.getUuid());
            	notifyIAmDead();
                logger.debug("Node " + node.getUuid() + " leaves on its own");
                ManagementNodeLeftEvent evt = new ManagementNodeLeftEvent(node.getUuid(), node.getUuid(), false);
                bus.publish(evt);
            } else {
                logger.debug("Node " + node.getUuid() + " leaves due to heartbeat stopped");
            }

            unsubscriber.unsubscribeAll();
        } catch (Exception e) {
            throw new CloudRuntimeException("Node fails to leave", e);
        } finally {
            heartBeatTask.cancel(true);
            try {
                heartbeatDbConnection.close();
            } catch (SQLException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public void leave() {
        leave(true);
    }

    private void handleEvent(ManagementNodeJoinEvent evt) throws IOException {
        if (!evt.getNodeId().equals(node.getUuid())) {
            notifyNodeJoin(evt.getNodeId());
        }
    }

    private void handleEvent(ManagementNodeLeftEvent evt) throws IOException {
        if (evt.getLeftNodeId().equals(node.getUuid())) {
            if (!evt.getSponsorNodeId().equals(node.getUuid())) {
                logger.debug("Node " + evt.getSponsorNodeId() + " indicated I have been dead, now I(" + node.getUuid() + ") am leaving");
                leave(false);
                ManagementNodeExitMsg msg = new ManagementNodeExitMsg();
                msg.setServiceId(bus.makeLocalServiceId(ManagementNodeConstant.SERVICE_ID));
                msg.setReason(Reason.HeartBeatStopped);
                bus.send(msg);
            }
        } else {
            notifyNodeLeft(evt.getLeftNodeId());
        }
    }

    private void notifyIAmDead() {
        for (ManagementNodeChangeListener l : listeners) {
            try {
                l.iAmDead(node.getUuid());
            } catch (Exception e) {
                logger.warn("Exception happened while notifying listener " + l.getClass().getCanonicalName() + " that I am dead event", e);
            }
        }
    }

    @SyncThread
    private void notifyNodeLeft(String nodeId) {
        for (ManagementNodeChangeListener l : listeners) {
            try {
                l.nodeLeft(nodeId);
            } catch (Exception e) {
                logger.warn("Exception happened while notifying listener " + l.getClass().getCanonicalName() + " that node " + nodeId + " leaving event", e);
            }
        }
    }

    @SyncThread
    private void notifyNodeJoin(String nodeId) {
        for (ManagementNodeChangeListener l : listeners) {
            try {
                l.nodeJoin(nodeId);
            } catch (Exception e) {
                logger.warn("Exception happened while notifying listener " + l.getClass().getCanonicalName() + " that node " + nodeId + " joining event", e);
            }
        }
    }

    @Override
    public boolean handleEvent(Event e) {
        try {
            if (e instanceof ManagementNodeJoinEvent) {
                handleEvent((ManagementNodeJoinEvent) e);
            } else if (e instanceof ManagementNodeLeftEvent) {
                handleEvent((ManagementNodeLeftEvent) e);
            } else {
            	bus.dealWithUnknownMessage(e);
            }
        } catch (Exception et) {
        	bus.logExceptionWithMessageDump(e, et);
        }
        
        return false;
    }

    public void addListener(ManagementNodeChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public void removeListener(ManagementNodeChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    public void setListeners(List<ManagementNodeChangeListener> listeners) {
        this.listeners = listeners;
    }
}
