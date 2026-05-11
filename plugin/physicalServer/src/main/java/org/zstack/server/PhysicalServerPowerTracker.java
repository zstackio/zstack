package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.tracker.PingTracker;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.server.PhysicalServerAO_;
import org.zstack.header.server.PhysicalServerConstant;
import org.zstack.header.server.PhysicalServerPowerStatus;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.PingPhysicalServerMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class PhysicalServerPowerTracker extends PingTracker implements
        ManagementNodeChangeListener,
        ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PhysicalServerPowerTracker.class);

    // Test seam (UNIT_TEST_ON only): (oobAddress, oobUsername) -> simulated power status.
    // Consumed by PhysicalServerManagerImpl.handle(PingPhysicalServerMsg) so IT cases
    // can drive the tracker without a real BMC.
    public static volatile BiFunction<String, String, PhysicalServerPowerStatus> powerOverride;

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Override
    public String getResourceName() {
        return "PhysicalServer";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingPhysicalServerMsg msg = new PingPhysicalServerMsg();
        msg.setUuid(resUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PhysicalServerConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return PhysicalServerGlobalConfig.POWER_PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return PhysicalServerGlobalConfig.POWER_PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to ping power status for PhysicalServer[uuid:%s]: %s",
                    resourceUuid, reply.getError().getDescription()));
        }
    }

    @Override
    protected void startHook() {
        PhysicalServerGlobalConfig.POWER_PING_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> pingIntervalChanged());
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        rescanPhysicalServers();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        rescanPhysicalServers();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void managementNodeReady() {
        rescanPhysicalServers();
    }

    private void rescanPhysicalServers() {
        List<String> all = Q.New(PhysicalServerVO.class)
                .notNull(PhysicalServerAO_.oobAddress)
                .notNull(PhysicalServerAO_.oobUsername)
                .notNull(PhysicalServerAO_.oobPassword)
                .select(PhysicalServerAO_.uuid)
                .listValues();
        List<String> toTrack = new ArrayList<>();
        for (String uuid : all) {
            if (destinationMaker.isManagedByUs(uuid)) {
                toTrack.add(uuid);
            }
        }

        untrackAll();
        track(toTrack);
    }
}
