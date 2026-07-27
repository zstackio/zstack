package org.zstack.networksecuritypolicyschedule;

import org.zstack.core.db.Q;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NetworkSecurityPolicyScheduleFacade {
    private static final CLogger logger = Utils.getLogger(NetworkSecurityPolicyScheduleFacade.class);

    private volatile Clock clock = Clock.systemUTC();

    public Instant currentInstant() {
        return clock.instant();
    }

    public Instant now() {
        return currentInstant().truncatedTo(ChronoUnit.MINUTES);
    }

    public boolean isInSchedule(NetworkSecurityPolicyScheduleVO vo, Instant instant) {
        return NetworkSecurityPolicyScheduleTime.valueOf(vo).isInSchedule(instant);
    }

    public boolean changesBetween(NetworkSecurityPolicyScheduleVO vo, Instant first, Instant second) {
        NetworkSecurityPolicyScheduleTime time = NetworkSecurityPolicyScheduleTime.valueOf(vo);
        return time.isInSchedule(first) != time.isInSchedule(second);
    }

    public Instant expirationInstant(NetworkSecurityPolicyScheduleVO vo) {
        return NetworkSecurityPolicyScheduleTime.valueOf(vo).expirationInstant();
    }

    Set<String> findInactiveResourceUuids(Map<String, String> scheduleUuidByResource) {
        if (scheduleUuidByResource.isEmpty()) {
            return Collections.emptySet();
        }

        List<NetworkSecurityPolicyScheduleVO> schedules = Q.New(NetworkSecurityPolicyScheduleVO.class)
                .in(NetworkSecurityPolicyScheduleVO_.uuid, new HashSet<>(scheduleUuidByResource.values()))
                .list();
        Map<String, NetworkSecurityPolicyScheduleVO> scheduleByUuid = new HashMap<>();
        for (NetworkSecurityPolicyScheduleVO schedule : schedules) {
            scheduleByUuid.put(schedule.getUuid(), schedule);
        }

        Instant now = now();
        Set<String> inactiveResources = new HashSet<>();
        for (Map.Entry<String, String> entry : scheduleUuidByResource.entrySet()) {
            NetworkSecurityPolicyScheduleVO schedule = scheduleByUuid.get(entry.getValue());
            if (schedule == null) {
                logger.warn(String.format(
                        "resource[uuid:%s] references missing network security policy schedule[uuid:%s]",
                        entry.getKey(), entry.getValue()));
                continue;
            }

            if (!isInSchedule(schedule, now)) {
                inactiveResources.add(entry.getKey());
            }
        }
        return inactiveResources;
    }

    public void setClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public void resetClock() {
        clock = Clock.systemUTC();
    }
}
