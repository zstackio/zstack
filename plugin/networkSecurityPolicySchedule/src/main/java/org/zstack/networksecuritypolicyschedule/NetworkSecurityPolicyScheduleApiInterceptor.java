package org.zstack.networksecuritypolicyschedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;
import org.zstack.identity.rbac.CheckIfAccountCanAccessResource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_IDENTITY_RBAC_10007;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10000;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10001;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10002;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10003;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10004;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10005;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10006;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10007;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10008;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10010;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011;

public class NetworkSecurityPolicyScheduleApiInterceptor implements ApiMessageInterceptor {
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
    private static final String TIME_REGEX = "\\d{2}:\\d{2}";

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private NetworkSecurityPolicyScheduleFacade scheduleFacade;
    @Autowired
    private AccountManager accountManager;
    @Autowired
    private NetworkSecurityPolicyScheduleResourceBackendRegistry backendRegistry;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        String resourceUuid = null;
        if (msg instanceof APICreateNetworkSecurityPolicyScheduleMsg) {
            validate((APICreateNetworkSecurityPolicyScheduleMsg) msg);
            resourceUuid = ((APICreateNetworkSecurityPolicyScheduleMsg) msg).getResourceUuid();
        } else if (msg instanceof APIUpdateNetworkSecurityPolicyScheduleMsg) {
            NetworkSecurityPolicyScheduleVO schedule = validate((APIUpdateNetworkSecurityPolicyScheduleMsg) msg);
            resourceUuid = schedule.getResourceUuid();
        } else if (msg instanceof APIDeleteNetworkSecurityPolicyScheduleMsg) {
            NetworkSecurityPolicyScheduleVO schedule = validate((APIDeleteNetworkSecurityPolicyScheduleMsg) msg);
            resourceUuid = schedule.getResourceUuid();
        } else if (msg instanceof APIGetNetworkSecurityPolicyScheduleMsg) {
            validate((APIGetNetworkSecurityPolicyScheduleMsg) msg);
            resourceUuid = ((APIGetNetworkSecurityPolicyScheduleMsg) msg).getResourceUuid();
        } else if (msg instanceof APISetNetworkSecurityPolicyScheduleMsg) {
            validate((APISetNetworkSecurityPolicyScheduleMsg) msg);
            resourceUuid = ((APISetNetworkSecurityPolicyScheduleMsg) msg).getResourceUuid();
        }

        if (resourceUuid != null) {
            bus.makeTargetServiceIdByResourceUuid(
                    msg, NetworkSecurityPolicyScheduleConstant.SERVICE_ID, resourceUuid);
        }
        return msg;
    }

    private void validate(APICreateNetworkSecurityPolicyScheduleMsg msg) {
        validateResource(msg.getResourceType(), msg.getResourceUuid());
        NetworkSecurityPolicyScheduleTime time = validateTime(
                msg.getTimeType(), msg.getRepeatType(), msg.getStartDate(), msg.getEndDate(),
                msg.getStartTime(), msg.getEndTime(), msg.getWeekDays());
        msg.setWeekDays(time.getWeekDays());
        if (!time.hasRemainingSchedule(scheduleFacade.now())) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10007,
                    "network security policy schedule has no remaining active minute"));
        }
    }

    private NetworkSecurityPolicyScheduleVO validate(APIUpdateNetworkSecurityPolicyScheduleMsg msg) {
        NetworkSecurityPolicyScheduleVO schedule = dbf.findByUuid(msg.getUuid(), NetworkSecurityPolicyScheduleVO.class);
        if (schedule == null) {
            throw scheduleNotFound(msg.getUuid());
        }
        validateResource(schedule.getResourceType(), schedule.getResourceUuid());
        validateAccountAccess(msg, schedule.getResourceUuid());
        NetworkSecurityPolicyScheduleTime time = validateTime(
                msg.getTimeType(), msg.getRepeatType(), msg.getStartDate(), msg.getEndDate(),
                msg.getStartTime(), msg.getEndTime(), msg.getWeekDays());
        msg.setWeekDays(time.getWeekDays());
        return schedule;
    }

    private NetworkSecurityPolicyScheduleVO validate(APIDeleteNetworkSecurityPolicyScheduleMsg msg) {
        NetworkSecurityPolicyScheduleVO schedule = dbf.findByUuid(msg.getUuid(), NetworkSecurityPolicyScheduleVO.class);
        if (schedule == null) {
            throw scheduleNotFound(msg.getUuid());
        }
        validateResource(schedule.getResourceType(), schedule.getResourceUuid());
        validateAccountAccess(msg, schedule.getResourceUuid());
        return schedule;
    }

    private void validate(APIGetNetworkSecurityPolicyScheduleMsg msg) {
        validateGetResource(msg.getResourceUuid());
        validateAccountAccess(msg, msg.getResourceUuid());
    }

    private void validateAccountAccess(APIMessage msg, String resourceUuid) {
        if (!accountManager.isAdmin(msg.getSession())
                && !CheckIfAccountCanAccessResource.check(
                        Collections.singletonList(resourceUuid),
                        msg.getSession().getAccountUuid()).isEmpty()) {
            throw new ApiMessageInterceptionException(operr(
                    ORG_ZSTACK_IDENTITY_RBAC_10007,
                    "permission denied, the account[uuid:%s] is not authorized to access resource[uuid:%s]",
                    msg.getSession().getAccountUuid(), resourceUuid));
        }
    }

    private void validate(APISetNetworkSecurityPolicyScheduleMsg msg) {
        validateResource(msg.getResourceType(), msg.getResourceUuid());
        if (msg.getScheduleUuid() == null) {
            return;
        }
        NetworkSecurityPolicyScheduleVO schedule = validateScheduleOwner(
                msg.getScheduleUuid(), msg.getResourceType(), msg.getResourceUuid());
        if (!Objects.equals(
                scheduleUuidOf(msg.getResourceType(), msg.getResourceUuid()),
                msg.getScheduleUuid())
                && !NetworkSecurityPolicyScheduleTime.valueOf(schedule)
                        .hasRemainingSchedule(scheduleFacade.now())) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10007,
                    "network security policy schedule[uuid:%s] has no remaining active minute",
                    msg.getScheduleUuid()));
        }
    }

    private NetworkSecurityPolicyScheduleTime validateTime(
            String timeType, String repeatType, String startDate, String endDate,
            String startTime, String endTime, List<Integer> weekDays) {
        NetworkSecurityPolicyScheduleTimeType type = validateTimeType(timeType);
        NetworkSecurityPolicyScheduleRepeatType repeat = validateRepeatType(repeatType);
        LocalDate start = parseDate("startDate", startDate);
        LocalDate end = parseDate("endDate", endDate);
        LocalTime startAt = parseTime("startTime", startTime);
        LocalTime endAt = parseTime("endTime", endTime);
        List<Integer> normalizedWeekDays = validateWeekDays(repeat, weekDays);

        NetworkSecurityPolicyScheduleTime time = NetworkSecurityPolicyScheduleTime.valueOf(
                type, repeat, start, end, startAt, endAt, normalizedWeekDays);
        validateTimeRange(time);
        return time;
    }

    private NetworkSecurityPolicyScheduleTimeType validateTimeType(String timeType) {
        try {
            return NetworkSecurityPolicyScheduleTimeType.valueOf(timeType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10000,
                    "timeType must be Local or UTC, but got[%s]", timeType));
        }
    }

    private NetworkSecurityPolicyScheduleRepeatType validateRepeatType(String repeatType) {
        try {
            return NetworkSecurityPolicyScheduleRepeatType.valueOf(repeatType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10001,
                    "repeatType must be Once or Weekly, but got[%s]", repeatType));
        }
    }

    private LocalDate parseDate(String fieldName, String value) {
        if (value == null || !value.matches(DATE_REGEX)) {
            throw invalidDate(fieldName, value);
        }
        try {
            return NetworkSecurityPolicyScheduleTime.parseDate(value);
        } catch (DateTimeParseException ignored) {
            throw invalidDate(fieldName, value);
        }
    }

    private ApiMessageInterceptionException invalidDate(String fieldName, String value) {
        return new ApiMessageInterceptionException(argerr(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10002,
                "%s must use yyyy-MM-dd with a valid calendar date, but got[%s]",
                fieldName, value));
    }

    private LocalTime parseTime(String fieldName, String value) {
        if (value == null || !value.matches(TIME_REGEX)) {
            throw invalidTime(fieldName, value);
        }
        try {
            return NetworkSecurityPolicyScheduleTime.parseTime(value);
        } catch (DateTimeParseException ignored) {
            throw invalidTime(fieldName, value);
        }
    }

    private ApiMessageInterceptionException invalidTime(String fieldName, String value) {
        return new ApiMessageInterceptionException(argerr(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10003,
                "%s must use HH:mm with minute precision, but got[%s]",
                fieldName, value));
    }

    private List<Integer> validateWeekDays(NetworkSecurityPolicyScheduleRepeatType repeatType,
                                           List<Integer> weekDays) {
        if (repeatType == NetworkSecurityPolicyScheduleRepeatType.Once) {
            if (weekDays != null && !weekDays.isEmpty()) {
                throw invalidWeekDays("weekDays must be empty when repeatType is Once");
            }
            return Collections.emptyList();
        }
        if (weekDays == null || weekDays.isEmpty()) {
            throw invalidWeekDays("weekDays cannot be empty when repeatType is Weekly");
        }

        Set<Integer> unique = new HashSet<>(weekDays);
        if (unique.size() != weekDays.size()) {
            throw invalidWeekDays(String.format("weekDays contains duplicate values: %s", weekDays));
        }
        if (unique.stream().anyMatch(day -> day == null || day < 1 || day > 7)) {
            throw invalidWeekDays(String.format("weekDays must be between 1 and 7: %s", weekDays));
        }

        List<Integer> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return normalized;
    }

    private ApiMessageInterceptionException invalidWeekDays(String details) {
        return new ApiMessageInterceptionException(argerr(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10004, details));
    }

    private void validateTimeRange(NetworkSecurityPolicyScheduleTime time) {
        if (time.getRepeatType() == NetworkSecurityPolicyScheduleRepeatType.Once) {
            LocalDateTime start = LocalDateTime.of(time.getStartDate(), time.getStartTime());
            LocalDateTime end = LocalDateTime.of(time.getEndDate(), time.getEndTime());
            if (!end.isAfter(start)) {
                throw invalidTimeRange("the end of a Once schedule must be after its start");
            }
            return;
        }

        if (time.getEndDate().isBefore(time.getStartDate())) {
            throw invalidTimeRange("endDate cannot be before startDate");
        }
        if (!time.isAllDay() && !time.getEndTime().isAfter(time.getStartTime())) {
            throw invalidTimeRange(
                    "a Weekly time range must be 00:00-00:00 or have endTime after startTime");
        }
        if (!time.hasScheduledDay()) {
            throw invalidTimeRange(
                    "the Weekly date range does not contain any selected weekday");
        }
    }

    private ApiMessageInterceptionException invalidTimeRange(String details) {
        return new ApiMessageInterceptionException(argerr(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10005, details));
    }

    private void validateResource(String resourceType, String resourceUuid) {
        if (!resourceBackend(resourceType).resourceExists(resourceUuid)) {
            throw resourceNotFound(resourceType, resourceUuid);
        }
    }

    private void validateGetResource(String resourceUuid) {
        boolean exists = backendRegistry.getBackends().stream()
                .anyMatch(backend -> backend.resourceExists(resourceUuid));
        if (!exists) {
            throw new ApiMessageInterceptionException(err(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10008,
                    SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find a supported network security resource with uuid[%s]", resourceUuid));
        }
    }

    private NetworkSecurityPolicyScheduleVO validateScheduleOwner(String scheduleUuid,
                                                         String resourceType,
                                                         String resourceUuid) {
        NetworkSecurityPolicyScheduleVO schedule = dbf.findByUuid(scheduleUuid, NetworkSecurityPolicyScheduleVO.class);
        if (schedule == null) {
            throw scheduleNotFound(scheduleUuid);
        }
        validateScheduleOwner(schedule, resourceType, resourceUuid);
        return schedule;
    }

    private void validateScheduleOwner(NetworkSecurityPolicyScheduleVO schedule,
                                        String resourceType,
                                        String resourceUuid) {
        if (!resourceType.equals(schedule.getResourceType())
                || !resourceUuid.equals(schedule.getResourceUuid())) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10010,
                    "network security policy schedule[uuid:%s] does not belong to %s[uuid:%s]",
                    schedule.getUuid(), resourceType, resourceUuid));
        }
    }

    private String scheduleUuidOf(String resourceType, String resourceUuid) {
        return resourceBackend(resourceType).getScheduleUuid(resourceUuid);
    }

    private NetworkSecurityPolicyScheduleResourceBackend resourceBackend(String resourceType) {
        NetworkSecurityPolicyScheduleResourceBackend backend = backendRegistry.getBackend(resourceType);
        if (backend == null) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011,
                    "unsupported resourceType[%s]", resourceType));
        }
        return backend;
    }

    private ApiMessageInterceptionException scheduleNotFound(String scheduleUuid) {
        return new ApiMessageInterceptionException(err(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10006,
                SysErrors.RESOURCE_NOT_FOUND,
                "cannot find network security policy schedule[uuid:%s]",
                scheduleUuid));
    }

    private ApiMessageInterceptionException resourceNotFound(String resourceType, String resourceUuid) {
        return new ApiMessageInterceptionException(err(
                ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10008,
                SysErrors.RESOURCE_NOT_FOUND,
                "cannot find %s[uuid:%s]", resourceType, resourceUuid));
    }
}
