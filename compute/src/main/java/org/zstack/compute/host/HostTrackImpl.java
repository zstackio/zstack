package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusSteppingCallback;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 */
public class HostTrackImpl implements HostTracker, ManagementNodeChangeListener, Component {
    private final static CLogger logger = Utils.getLogger(HostTrackImpl.class);

    private final List<String> hostUuids = Collections.synchronizedList(new ArrayList<String>());
    private Set<String> hostInTracking = Collections.synchronizedSet(new HashSet<String>());
    private Future<Void> trackerThread = null;
    private final Map<String, HostStatusEvent> hostConnectionStateEventMap = Collections.synchronizedMap(new HashMap<String, HostStatusEvent>());
    private final List<String> inReconnectingHost = Collections.synchronizedList(new ArrayList<String>());

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    private class Tracker implements PeriodicTask {
        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.SECONDS;
        }

        @Override
        public long getInterval() {
            return HostGlobalConfig.PING_HOST_INTERVAL.value(Integer.class);
        }

        @Override
        public String getName() {
            return "hostTrack-for-managementNode-" + Platform.getManagementServerId();
        }

        private void handleReply(final String hostUuid, MessageReply reply) {
            if (!reply.isSuccess()) {
                logger.warn(String.format("[Host Tracker]: unable track host[uuid:%s], %s", hostUuid, reply.getError()));
                return;
            }

            final PingHostReply preply = (PingHostReply)reply;
            HostStatusEvent cevt = preply.isConnected() ? HostStatusEvent.connected : HostStatusEvent.disconnected;
            if (logger.isTraceEnabled()) {
                String moreInfo = preply.isConnected() ? "all good!" : preply.getError().toString();
                logger.trace(String.format("[Host Tracker]: ping host[uuid:%s], connection state[%s], %s", hostUuid, cevt, moreInfo));
            }

            //TODO: use hostConnectionStateEventMap to implement stopping PING after failing specific times

            boolean needReconnect = cevt == HostStatusEvent.disconnected && preply.isSuccess() && HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.value(Boolean.class);
            if (needReconnect && !inReconnectingHost.contains(hostUuid)) {
                inReconnectingHost.add(hostUuid);
                logger.debug(String.format("[Host Tracker]: detected host[uuid:%s] connection lost, issue a reconnect because %s is set to true",
                        hostUuid, HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.getCanonicalName()));
                ReconnectHostMsg msg = new ReconnectHostMsg();
                msg.setHostUuid(hostUuid);
                msg.setSkipIfHostConnected(true);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack() {
                    @Override
                    public void run(MessageReply reply) {
                        inReconnectingHost.remove(hostUuid);

                        if (!reply.isSuccess()) {
                            logger.warn(String.format("host[uuid:%s] failed to reconnect, %s", hostUuid, reply.getError()));
                            hostConnectionStateEventMap.put(hostUuid, HostStatusEvent.disconnected);
                        } else {
                            hostConnectionStateEventMap.put(hostUuid, HostStatusEvent.connected);
                        }

                    }
                });
            } else {
                hostConnectionStateEventMap.put(hostUuid, cevt);
            }
        }

        @Override
        public void run() {
            try {
                List<PingHostMsg> msgs = null;
                synchronized (hostUuids) {
                    msgs = new ArrayList<PingHostMsg>();
                    for (String huuid : hostUuids) {
                        if (hostInTracking.contains(huuid)) {
                            continue;
                        }

                        PingHostMsg msg = new PingHostMsg();
                        msg.setHostUuid(huuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
                        msgs.add(msg);
                        hostInTracking.add(huuid);
                    }
                }

                if (msgs.isEmpty()) {
                    return;
                }

                bus.send(msgs, HostGlobalConfig.HOST_TRACK_PARALLELISM_DEGREE.value(Integer.class), new CloudBusSteppingCallback() {
                    @Override
                    public void run(NeedReplyMessage msg, MessageReply reply) {
                        PingHostMsg pmsg = (PingHostMsg)msg;
                        handleReply(pmsg.getHostUuid(), reply);
                        hostInTracking.remove(pmsg.getHostUuid());
                    }
                });
            } catch (Throwable t) {
                logger.warn("unhandled exception", t);
            }
        }
    }

    public void trackHost(String hostUuid) {
        synchronized (hostUuids) {
            if (!hostUuids.contains(hostUuid)) {
                hostUuids.add(hostUuid);
                logger.debug(String.format("start tracking host[uuid:%s]", hostUuid));
            }
        }
    }

    @Override
    public void untrackHost(String hostUuid) {
        synchronized (hostUuids) {
            hostUuids.remove(hostUuid);
            hostConnectionStateEventMap.remove(hostUuid);
            logger.debug(String.format("stop tracking host[uuid:%s]", hostUuid));
        }
    }

    @Override
    public void trackHost(Collection<String> huuids) {
        synchronized (hostUuids) {
            for (String huuid : huuids) {
                if (!hostUuids.contains(huuid)) {
                    hostUuids.add(huuid);
                    logger.debug(String.format("start tracking host[uuid:%s]", huuid));
                }
            }
        }
    }

    @Override
    public void untrackHost(Collection<String> huuids) {
        synchronized (hostUuids) {
            for (String huuid : huuids) {
                hostUuids.remove(huuid);
                hostConnectionStateEventMap.remove(huuid);
                logger.debug(String.format("stop tracking host[uuid:%s]", huuid));
            }
        }
    }

    private void reScanHost() {
        synchronized (hostUuids) {
            hostUuids.clear();

            long count = dbf.count(HostVO.class);
            int times = (int)count / 10000 + (count%10000 == 0 ? 0 : 1);
            int offset = 0;
            for (int i=0; i<times; i++) {
                SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
                q.select(HostVO_.uuid);
                q.setStart(offset);
                q.setLimit(10000);
                List<String> huuids = q.listValue();
                for (String h : huuids) {
                    if (destMaker.isManagedByUs(h)) {
                        hostUuids.add(h);
                    }
                }

                offset += 10000;
            }
        }
    }

    @Override
    public void nodeJoin(String nodeId) {
        reScanHost();
    }

    @Override
    public void nodeLeft(String nodeId) {
        reScanHost();
    }

    @Override
    public void iAmDead(String nodeId) {

    }

    @Override
    public void iJoin(String nodeId) {

    }

    private void startTracker() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }

        trackerThread = thdf.submitPeriodicTask(new Tracker());
    }

    private void setupTracker() {
        startTracker();

        HostGlobalConfig.PING_HOST_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                logger.debug(String.format("%s change from %s to %s, restart tracker thread",
                        oldConfig.getCanonicalName(), oldConfig.value(), newConfig.value()));
                startTracker();
            }
        });
    }

    @Override
    public boolean start() {
        setupTracker();
        return true;
    }

    @Override
    public boolean stop() {
        trackerThread.cancel(true);
        return true;
    }
}
