package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.network.NetworkUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The VirtualRouterPingFailureTracker will monitor the ping heart beat
 * and remove the failed virtual router from regular PingTracker.
 */
public class VirtualRouterPingFailureTracker implements ManagementNodeReadyExtensionPoint,
        VirtualRouterTrackerExtensionPoint {

    @Autowired
    private VirtualRouterPingTracker tracker;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

    private final ConcurrentHashMap<String, String> failed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> reconnecting = new ConcurrentHashMap<>();

    private String getVrMgmtIp(String vrUuid) {
        return SQL.New("select nic.ip from ApplianceVmVO vm, VmNicVO nic " +
                "where vm.uuid = :uuid " +
                "and nic.vmInstanceUuid = vm.uuid " +
                "and nic.l3NetworkUuid = vm.managementNetworkUuid", String.class)
                .param("uuid", vrUuid)
                .find();
    }

    public void notifyFailure(String resourceUuid) {
        failed.computeIfAbsent(resourceUuid, this::getVrMgmtIp);
    }

    @Override
    public void handleTracerReply(final String resourceUuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            notifyFailure(resourceUuid);
            tracker.untrack(resourceUuid);
        }
    }

    @Override
    public void managementNodeReady() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            final AtomicInteger cnt = new AtomicInteger(0);

            private void reconnectVR(String resUuid) {
                if (null != reconnecting.putIfAbsent(resUuid, Boolean.TRUE)) {
                    return;
                }

                ReconnectVirtualRouterVmMsg msg = new ReconnectVirtualRouterVmMsg();
                msg.setVirtualRouterVmUuid(resUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, resUuid);
                bus.send(msg, new CloudBusCallBack(null) {
                    @Override
                    public void run(MessageReply reply) {
                        reconnecting.remove(resUuid);

                        if (reply.isSuccess()) {
                            failed.remove(resUuid);
                            tracker.track(resUuid);
                        }
                    }
                });
            }

            private void checkReachable() {
                for (Map.Entry<String, String> entry: failed.entrySet()) {
                    final String resUuid = entry.getKey();

                    if (!destinationMaker.isManagedByUs(resUuid)) {
                        failed.remove(resUuid);
                        continue;
                    }

                    if (NetworkUtils.isReachable(entry.getValue(), 100)) {
                        reconnectVR(resUuid);
                    }
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return 200;
            }

            @Override
            public String getName() {
                return "tracking-vr-ping-failure";
            }

            @Override
            public void run() {
                if (cnt.addAndGet(1) > 1) {
                    return;
                }

                try {
                    checkReachable();
                } finally {
                    cnt.set(0);
                }
            }
        });
    }
}
