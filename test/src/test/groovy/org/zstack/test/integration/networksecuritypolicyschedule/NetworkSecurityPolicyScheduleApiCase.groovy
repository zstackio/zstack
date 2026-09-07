package org.zstack.test.integration.networksecuritypolicyschedule

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.network.securitygroup.SecurityGroupVO
import org.zstack.network.securitygroup.SecurityGroupVO_
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleFacade
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleScanTask
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleVO
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.ApiException
import org.zstack.sdk.CreateNetworkSecurityPolicyScheduleAction
import org.zstack.sdk.CreateSecurityGroupAction
import org.zstack.sdk.DeleteNetworkSecurityPolicyScheduleAction
import org.zstack.sdk.GetNetworkSecurityPolicyScheduleAction
import org.zstack.sdk.NetworkSecurityPolicyScheduleInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.SetNetworkSecurityPolicyScheduleAction
import org.zstack.sdk.UpdateNetworkSecurityPolicyScheduleAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentLinkedQueue

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011

class NetworkSecurityPolicyScheduleApiCase extends SubCase {
    EnvSpec env
    NetworkSecurityPolicyScheduleFacade scheduleFacade
    NetworkSecurityPolicyScheduleScanTask scanTask
    @Override
    void setup() {
        useSpring(NetworkSecurityPolicyScheduleTest.springSpec)
    }

    @Override
    void environment() {
        env = Test.makeEnv {
        }
    }

    @Override
    void clean() {
        try {
            env.delete()
        } finally {
            scheduleFacade?.resetClock()
            scanTask?.start()
        }
    }

    private NetworkSecurityPolicyScheduleInventory createSchedule(
            String resourceUuid, String name = "office-hours") {
        return createNetworkSecurityPolicySchedule {
            resourceType = "SecurityGroup"
            delegate.resourceUuid = resourceUuid
            delegate.name = name
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-01"
            endDate = "2026-08-31"
            startTime = "09:00"
            endTime = "18:00"
            weekDays = [1, 2, 3, 4, 5]
        } as NetworkSecurityPolicyScheduleInventory
    }

    private CreateNetworkSecurityPolicyScheduleAction.Result createScheduleResult(
            String resourceType, String resourceUuid, Map overrides = [:], String sessionId = null) {
        Map fields = [
                name: "office-hours",
                description: null,
                resourceType: resourceType,
                resourceUuid: resourceUuid,
                timeType: "UTC",
                repeatType: "Weekly",
                startDate: "2026-07-01",
                endDate: "2026-08-31",
                startTime: "09:00",
                endTime: "18:00",
                weekDays: [1, 2, 3, 4, 5]
        ]
        fields.putAll(overrides)
        return new CreateNetworkSecurityPolicyScheduleAction(
                name: fields.name,
                description: fields.description,
                resourceType: fields.resourceType,
                resourceUuid: fields.resourceUuid,
                timeType: fields.timeType,
                repeatType: fields.repeatType,
                startDate: fields.startDate,
                endDate: fields.endDate,
                startTime: fields.startTime,
                endTime: fields.endTime,
                weekDays: fields.weekDays,
                sessionId: sessionId ?: adminSession()
        ).call()
    }

    private UpdateNetworkSecurityPolicyScheduleAction.Result updateScheduleResult(
            NetworkSecurityPolicyScheduleInventory schedule,
            Map overrides = [:],
            String sessionId = null) {
        Map fields = [
                uuid: schedule.uuid,
                name: schedule.name,
                description: schedule.description,
                timeType: schedule.timeType,
                repeatType: schedule.repeatType,
                startDate: schedule.startDate,
                endDate: schedule.endDate,
                startTime: schedule.startTime,
                endTime: schedule.endTime,
                weekDays: schedule.weekDays
        ]
        fields.putAll(overrides)
        return new UpdateNetworkSecurityPolicyScheduleAction(
                uuid: fields.uuid,
                name: fields.name,
                description: fields.description,
                timeType: fields.timeType,
                repeatType: fields.repeatType,
                startDate: fields.startDate,
                endDate: fields.endDate,
                startTime: fields.startTime,
                endTime: fields.endTime,
                weekDays: fields.weekDays,
                sessionId: sessionId ?: adminSession()
        ).call()
    }

    private String scheduleUuidOf(String resourceUuid) {
        return Q.New(SecurityGroupVO.class)
                .eq(SecurityGroupVO_.uuid, resourceUuid)
                .select(SecurityGroupVO_.scheduleUuid)
                .findValue()
    }

    private List<NetworkSecurityPolicyScheduleInventory> getSchedules(String resourceUuid) {
        return getNetworkSecurityPolicySchedule {
            delegate.resourceUuid = resourceUuid
        } as List<NetworkSecurityPolicyScheduleInventory>
    }

    private void setSchedule(NetworkSecurityPolicyScheduleInventory schedule,
                             String resourceUuid) {
        setNetworkSecurityPolicySchedule {
            scheduleUuid = schedule.uuid
            resourceType = "SecurityGroup"
            delegate.resourceUuid = resourceUuid
        }
    }

    private void unsetSchedule(String resourceUuid) {
        setNetworkSecurityPolicySchedule {
            resourceType = "SecurityGroup"
            delegate.resourceUuid = resourceUuid
        }
    }

    private SetNetworkSecurityPolicyScheduleAction.Result setScheduleResult(
            String scheduleUuid,
            String resourceType,
            String resourceUuid,
            String sessionId = null) {
        return new SetNetworkSecurityPolicyScheduleAction(
                scheduleUuid: scheduleUuid,
                resourceType: resourceType,
                resourceUuid: resourceUuid,
                sessionId: sessionId ?: adminSession()
        ).call()
    }

    private static void assertTimeStatus(
            NetworkSecurityPolicyScheduleInventory schedule,
            String expected) {
        String actual = schedule.timeStatus?.toString()
        assert actual == expected :
                "timeStatus: expected=${expected}, actual=${actual}, schedule=${schedule.name}"
    }

    private static void assertExpiring(
            NetworkSecurityPolicyScheduleInventory schedule,
            boolean expected) {
        assert schedule.expiring == expected :
                "expiring: expected=${expected}, actual=${schedule.expiring}, schedule=${schedule.name}"
    }

    void testTimeStatus() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-time-status-sg"
            ipVersion = 4
        } as SecurityGroupInventory

        NetworkSecurityPolicyScheduleInventory upcoming = createNetworkSecurityPolicySchedule {
            name = "upcoming-once"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-29"
            endDate = "2026-07-29"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory active = createNetworkSecurityPolicySchedule {
            name = "active-once"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-29"
            endDate = "2026-07-29"
            startTime = "07:00"
            endTime = "09:00"
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory outOfWindowWeekly = createNetworkSecurityPolicySchedule {
            name = "out-of-window-weekly"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-08-05"
            startTime = "07:00"
            endTime = "08:00"
            weekDays = [3]
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory notStartedWeekly = createNetworkSecurityPolicySchedule {
            name = "not-started-weekly"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-30"
            endDate = "2026-08-05"
            startTime = "07:00"
            endTime = "09:00"
            weekDays = [4]
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory inWindowWeekly = createNetworkSecurityPolicySchedule {
            name = "in-window-weekly"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-08-05"
            startTime = "07:00"
            endTime = "09:00"
            weekDays = [3]
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory lastWindow = createNetworkSecurityPolicySchedule {
            name = "last-window"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-08-02"
            startTime = "08:00"
            endTime = "09:00"
            weekDays = [3]
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory within24Hours = createNetworkSecurityPolicySchedule {
            name = "within-24-hours"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "07:00"
            endTime = "07:59"
        } as NetworkSecurityPolicyScheduleInventory
        NetworkSecurityPolicyScheduleInventory exactly24Hours = createNetworkSecurityPolicySchedule {
            name = "exactly-24-hours"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "07:00"
            endTime = "08:00"
        } as NetworkSecurityPolicyScheduleInventory

        assertTimeStatus(upcoming, "NotStarted")
        assertTimeStatus(active, "InWindow")
        assertTimeStatus(outOfWindowWeekly, "OutOfWindow")
        assertTimeStatus(notStartedWeekly, "NotStarted")
        assertTimeStatus(inWindowWeekly, "InWindow")
        assertTimeStatus(lastWindow, "InWindow")
        assertTimeStatus(within24Hours, "NotStarted")
        assertTimeStatus(exactly24Hours, "NotStarted")
        assertExpiring(outOfWindowWeekly, false)
        assertExpiring(lastWindow, true)
        assertExpiring(within24Hours, true)
        assertExpiring(exactly24Hours, false)

        NetworkSecurityPolicyScheduleInventory endedOnce = updateNetworkSecurityPolicySchedule {
            uuid = upcoming.uuid
            name = upcoming.name
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-29"
            endDate = "2026-07-29"
            startTime = "06:00"
            endTime = "07:00"
        } as NetworkSecurityPolicyScheduleInventory
        assertTimeStatus(endedOnce, "Ended")
        assertExpiring(endedOnce, false)
        NetworkSecurityPolicyScheduleInventory endedWeekly = updateNetworkSecurityPolicySchedule {
            uuid = lastWindow.uuid
            name = lastWindow.name
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-08-02"
            startTime = "07:00"
            endTime = "08:00"
            weekDays = [3]
        } as NetworkSecurityPolicyScheduleInventory
        assertTimeStatus(endedWeekly, "Ended")
        assertExpiring(endedWeekly, false)

        Map<String, NetworkSecurityPolicyScheduleInventory> queried =
                getSchedules(securityGroup.uuid).collectEntries {
                    [(it.uuid): it]
                }
        assertTimeStatus(queried[upcoming.uuid], "Ended")
        assertTimeStatus(queried[active.uuid], "InWindow")
        assertTimeStatus(queried[outOfWindowWeekly.uuid], "OutOfWindow")
        assertTimeStatus(queried[notStartedWeekly.uuid], "NotStarted")
        assertTimeStatus(queried[inWindowWeekly.uuid], "InWindow")
        assertTimeStatus(queried[lastWindow.uuid], "Ended")
        assertTimeStatus(queried[within24Hours.uuid], "NotStarted")
        assertTimeStatus(queried[exactly24Hours.uuid], "NotStarted")
        assertExpiring(queried[within24Hours.uuid], true)
        assertExpiring(queried[exactly24Hours.uuid], false)

        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-08-06T00:00:00Z"), ZoneOffset.UTC))
        List<NetworkSecurityPolicyScheduleInventory> allEnded =
                getSchedules(securityGroup.uuid)
        assert allEnded.size() == 8 : "schedule count: expected=8, actual=${allEnded.size()}"
        allEnded.each {
            assertTimeStatus(it, "Ended")
            assertExpiring(it, false)
        }

        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-29T08:00:00Z"), ZoneOffset.UTC))
    }

    void testGetFilters() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-filter-sg"
            ipVersion = 4
        } as SecurityGroupInventory

        createNetworkSecurityPolicySchedule {
            name = "once-utc"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-29"
            endDate = "2026-07-29"
            startTime = "07:00"
            endTime = "09:00"
        }
        createNetworkSecurityPolicySchedule {
            name = "weekly-utc"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-08-05"
            startTime = "07:00"
            endTime = "08:00"
            weekDays = [3]
        }
        createNetworkSecurityPolicySchedule {
            name = "once-local"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "Local"
            repeatType = "Once"
            startDate = "2026-07-28"
            endDate = "2026-07-30"
            startTime = "00:00"
            endTime = "23:59"
        }

        List<NetworkSecurityPolicyScheduleInventory> byStatus =
                getNetworkSecurityPolicySchedule {
                    resourceUuid = securityGroup.uuid
                    timeStatus = "OutOfWindow"
                } as List<NetworkSecurityPolicyScheduleInventory>
        assert byStatus*.name == ["weekly-utc"] :
                "timeStatus filter: ${byStatus*.name}"

        List<NetworkSecurityPolicyScheduleInventory> byRepeatType =
                getNetworkSecurityPolicySchedule {
                    resourceUuid = securityGroup.uuid
                    repeatType = "Once"
                } as List<NetworkSecurityPolicyScheduleInventory>
        assert byRepeatType*.name as Set == ["once-utc", "once-local"] as Set :
                "repeatType filter: ${byRepeatType*.name}"

        List<NetworkSecurityPolicyScheduleInventory> byTimeType =
                getNetworkSecurityPolicySchedule {
                    resourceUuid = securityGroup.uuid
                    timeType = "Local"
                } as List<NetworkSecurityPolicyScheduleInventory>
        assert byTimeType*.name == ["once-local"] :
                "timeType filter: ${byTimeType*.name}"

        List<NetworkSecurityPolicyScheduleInventory> combined =
                getNetworkSecurityPolicySchedule {
                    resourceUuid = securityGroup.uuid
                    timeStatus = "InWindow"
                    repeatType = "Once"
                    timeType = "UTC"
                } as List<NetworkSecurityPolicyScheduleInventory>
        assert combined*.name == ["once-utc"] :
                "combined filters: ${combined*.name}"

        expect(ApiException.class) {
            getNetworkSecurityPolicySchedule {
                resourceUuid = securityGroup.uuid
                timeStatus = "Unknown"
            }
        }
        expect(ApiException.class) {
            getNetworkSecurityPolicySchedule {
                resourceUuid = securityGroup.uuid
                repeatType = "Unknown"
            }
        }
        expect(ApiException.class) {
            getNetworkSecurityPolicySchedule {
                resourceUuid = securityGroup.uuid
                timeType = "Unknown"
            }
        }
    }

    void testSecurityGroupScheduleLifecycle() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "scheduled-security-group"
            ipVersion = 4
        } as SecurityGroupInventory

        NetworkSecurityPolicyScheduleInventory first =
                createSchedule(securityGroup.uuid, "office-hours")
        NetworkSecurityPolicyScheduleInventory second =
                createSchedule(securityGroup.uuid, "night-shift")
        List<NetworkSecurityPolicyScheduleInventory> queried =
                getSchedules(securityGroup.uuid)
        assert queried*.uuid as Set == [first.uuid, second.uuid] as Set &&
                queried.every {
                    it.resourceType == "SecurityGroup" &&
                            it.resourceUuid == securityGroup.uuid
                }
        assert scheduleUuidOf(securityGroup.uuid) == null

        setSchedule(first, securityGroup.uuid)
        assert scheduleUuidOf(securityGroup.uuid) == first.uuid

        setSchedule(second, securityGroup.uuid)
        assert scheduleUuidOf(securityGroup.uuid) == second.uuid

        unsetSchedule(securityGroup.uuid)
        assert scheduleUuidOf(securityGroup.uuid) == null &&
                getSchedules(securityGroup.uuid).size() == 2

        setSchedule(first, securityGroup.uuid)

        NetworkSecurityPolicyScheduleInventory updated = updateNetworkSecurityPolicySchedule {
            uuid = first.uuid
            name = "all-week"
            timeType = "Local"
            repeatType = "Weekly"
            startDate = "2026-07-01"
            endDate = "2026-08-31"
            startTime = "00:00"
            endTime = "00:00"
            weekDays = [7, 6, 5, 4, 3, 2, 1]
        } as NetworkSecurityPolicyScheduleInventory
        assert updated.name == "all-week" && updated.description == null
        assert updated.timeType == "Local" && updated.weekDays == [1, 2, 3, 4, 5, 6, 7]

        deleteNetworkSecurityPolicySchedule {
            uuid = first.uuid
        }
        assert scheduleUuidOf(securityGroup.uuid) == null &&
                getSchedules(securityGroup.uuid)*.uuid == [second.uuid]

        deleteNetworkSecurityPolicySchedule {
            uuid = second.uuid
        }
        deleteNetworkSecurityPolicySchedule {
            uuid = second.uuid
        }
        assert getSchedules(securityGroup.uuid).isEmpty()
    }

    void testCreateApiValidation() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-validation-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        expect(ApiException.class) {
            createScheduleResult("UnsupportedResource", securityGroup.uuid)
        }
        expect(ApiException.class) {
            createScheduleResult("SecurityGroup", securityGroup.uuid,
                    [timeType: "Asia/Shanghai"])
        }

        List<CreateNetworkSecurityPolicyScheduleAction.Result> results = [
                createScheduleResult("SecurityGroup", Platform.getUuid()),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [repeatType: "Once", startDate: "2026-07-29", endDate: "2026-07-29",
                         startTime: "10:00", endTime: "10:00", weekDays: null]),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [weekDays: []]),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [weekDays: [1, 1, 2]]),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [startTime: "23:00", endTime: "06:00"]),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [startDate: "2026-08-01", endDate: "2026-07-31"]),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [startTime: "09:00:00"]),
                createScheduleResult("SecurityGroup", securityGroup.uuid,
                        [startDate: "2025-01-01", endDate: "2025-01-31"])
        ]
        assert results.every { it.error != null }

        NetworkSecurityPolicyScheduleInventory crossDayOnce = createNetworkSecurityPolicySchedule {
            name = "cross-day-once"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-29"
            endDate = "2026-07-30"
            startTime = "23:00"
            endTime = "06:00"
        } as NetworkSecurityPolicyScheduleInventory
        assert crossDayOnce != null

        NetworkSecurityPolicyScheduleInventory nextWeek = createNetworkSecurityPolicySchedule {
            name = "next-week-window"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-08-05"
            startTime = "07:00"
            endTime = "08:00"
            weekDays = [3]
        } as NetworkSecurityPolicyScheduleInventory
        assert nextWeek != null
        deleteNetworkSecurityPolicySchedule {
            uuid = nextWeek.uuid
        }
        deleteNetworkSecurityPolicySchedule {
            uuid = crossDayOnce.uuid
        }
        assert getSchedules(securityGroup.uuid).isEmpty() &&
                scheduleUuidOf(securityGroup.uuid) == null
    }

    void testRejectUninstalledResourceBackend() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "unsupported-schedule-resource"
            ipVersion = 4
        } as SecurityGroupInventory
        String resourceUuid = securityGroup.uuid
        def createResult = createScheduleResult("VpcFirewallRuleSet", resourceUuid)
        def setResult = setScheduleResult(null, "VpcFirewallRuleSet", resourceUuid)

        assert createResult.error.globalErrorCode == ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011
        assert setResult.error.globalErrorCode == ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10011
        assert !Q.New(NetworkSecurityPolicyScheduleVO.class)
                .eq(NetworkSecurityPolicyScheduleVO_.resourceUuid, resourceUuid)
                .isExists()
    }

    void testUpdateApiValidation() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-update-validation-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        NetworkSecurityPolicyScheduleInventory schedule =
                createSchedule(securityGroup.uuid)
        setSchedule(schedule, securityGroup.uuid)

        expect(ApiException.class) {
            updateScheduleResult(schedule, [name: null])
        }

        List<UpdateNetworkSecurityPolicyScheduleAction.Result> results = [
                updateScheduleResult(schedule, [startTime: "09:00:00"]),
                updateScheduleResult(schedule,
                        [repeatType: "Once", startDate: "2026-07-29", endDate: "2026-07-29",
                         startTime: "10:00", endTime: "10:00", weekDays: null]),
                updateScheduleResult(schedule, [weekDays: []]),
                updateScheduleResult(schedule, [startTime: "23:00", endTime: "06:00"]),
                updateScheduleResult(schedule,
                        [startDate: "2026-07-29", endDate: "2026-07-29", weekDays: [4]])
        ]
        assert results.every { it.error != null }

        NetworkSecurityPolicyScheduleInventory current =
                getSchedules(securityGroup.uuid).find {
                    it.uuid == schedule.uuid
                }
        assert current.uuid == schedule.uuid &&
                current.name == schedule.name &&
                current.timeType == schedule.timeType &&
                current.repeatType == schedule.repeatType &&
                current.startDate == schedule.startDate &&
                current.endDate == schedule.endDate &&
                current.startTime == schedule.startTime &&
                current.endTime == schedule.endTime &&
                current.weekDays == schedule.weekDays

        UpdateNetworkSecurityPolicyScheduleAction.Result expiredUpdate = updateScheduleResult(
                schedule,
                [repeatType: "Once", startDate: "2026-07-01", endDate: "2026-07-01",
                 startTime: "09:00", endTime: "10:00", weekDays: null])
        assert expiredUpdate.error == null &&
                expiredUpdate.value.inventory.repeatType == "Once" &&
                expiredUpdate.value.inventory.weekDays == null

        SetNetworkSecurityPolicyScheduleAction.Result idempotentSet = setScheduleResult(
                schedule.uuid, "SecurityGroup", securityGroup.uuid)
        assert idempotentSet.error == null &&
                scheduleUuidOf(securityGroup.uuid) == schedule.uuid

        unsetSchedule(securityGroup.uuid)
        SetNetworkSecurityPolicyScheduleAction.Result expiredSet = setScheduleResult(
                schedule.uuid, "SecurityGroup", securityGroup.uuid)
        assert expiredSet.error == null && scheduleUuidOf(securityGroup.uuid) == schedule.uuid :
                "expired schedule should remain bindable"
        unsetSchedule(securityGroup.uuid)
    }

    void testDescriptionLength() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-description-length-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        String accepted = "a" * 255

        CreateNetworkSecurityPolicyScheduleAction.Result created = createScheduleResult(
                "SecurityGroup", securityGroup.uuid, [description: accepted])
        assert created.error == null && created.value.inventory.description == accepted :
                "255-character description should be accepted"

        expect(ApiException.class) {
            createScheduleResult("SecurityGroup", securityGroup.uuid, [description: "a" * 256])
        }
        expect(ApiException.class) {
            updateScheduleResult(created.value.inventory, [description: "a" * 256])
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = created.value.inventory.uuid
        }
    }

    void testMultipleSchedulesPerResource() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "multiple-schedule-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        NetworkSecurityPolicyScheduleInventory firstSecurityGroupSchedule =
                createSchedule(securityGroup.uuid, "first")
        NetworkSecurityPolicyScheduleInventory secondSecurityGroupSchedule =
                createSchedule(securityGroup.uuid, "second")

        assert getSchedules(securityGroup.uuid)*.uuid as Set ==
                [firstSecurityGroupSchedule.uuid, secondSecurityGroupSchedule.uuid] as Set
        assert scheduleUuidOf(securityGroup.uuid) == null
    }

    void testConcurrentCreate() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "concurrent-schedule-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        ConcurrentLinkedQueue<CreateNetworkSecurityPolicyScheduleAction.Result> results =
                new ConcurrentLinkedQueue<>()
        List<Thread> threads = (1..2).collect { int index ->
            Thread.start {
                results.add(createScheduleResult(
                        "SecurityGroup", securityGroup.uuid, [name: "concurrent-${index}"]))
            }
        }
        threads*.join()

        assert results.size() == 2 && results.every { it.error == null }
        Set<String> scheduleUuids = results.collect {
            it.value.inventory.uuid
        } as Set<String>
        assert scheduleUuids.size() == 2 &&
                getSchedules(securityGroup.uuid)*.uuid as Set == scheduleUuids &&
                scheduleUuidOf(securityGroup.uuid) == null
    }

    void testConcurrentSet() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "concurrent-set-schedule-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        List<NetworkSecurityPolicyScheduleInventory> schedules = [
                createSchedule(securityGroup.uuid, "first"),
                createSchedule(securityGroup.uuid, "second")
        ]
        ConcurrentLinkedQueue<SetNetworkSecurityPolicyScheduleAction.Result> results =
                new ConcurrentLinkedQueue<>()

        List<Thread> threads = schedules.collect { NetworkSecurityPolicyScheduleInventory schedule ->
            Thread.start {
                results.add(setScheduleResult(
                        schedule.uuid, "SecurityGroup", securityGroup.uuid))
            }
        }
        threads*.join()

        String currentScheduleUuid = scheduleUuidOf(securityGroup.uuid)
        assert results.size() == 2 && results.every { it.error == null } &&
                schedules*.uuid.contains(currentScheduleUuid)
    }

    void testScheduleOwnership() {
        SecurityGroupInventory owner = createSecurityGroup {
            name = "schedule-owner-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        SecurityGroupInventory other = createSecurityGroup {
            name = "schedule-other-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        NetworkSecurityPolicyScheduleInventory schedule =
                createSchedule(owner.uuid)

        SetNetworkSecurityPolicyScheduleAction.Result setResult = setScheduleResult(
                schedule.uuid, "SecurityGroup", other.uuid)
        setSchedule(schedule, owner.uuid)

        assert setResult.error != null
        assert getSchedules(owner.uuid)*.uuid == [schedule.uuid] &&
                scheduleUuidOf(owner.uuid) == schedule.uuid
    }

    void testResourceCascadeDeletion() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-cascade-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        List<NetworkSecurityPolicyScheduleInventory> securityGroupSchedules = [
                createSchedule(securityGroup.uuid, "first"),
                createSchedule(securityGroup.uuid, "second")
        ]
        setSchedule(securityGroupSchedules[0], securityGroup.uuid)

        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
        assert !Q.New(SecurityGroupVO.class)
                .eq(SecurityGroupVO_.uuid, securityGroup.uuid)
                .isExists() &&
                Q.New(NetworkSecurityPolicyScheduleVO.class)
                        .eq(NetworkSecurityPolicyScheduleVO_.resourceType, "SecurityGroup")
                        .eq(NetworkSecurityPolicyScheduleVO_.resourceUuid, securityGroup.uuid)
                        .count() == 0
    }

    void testAccountIsolation() {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-account-isolation-sg"
            ipVersion = 4
        } as SecurityGroupInventory
        NetworkSecurityPolicyScheduleInventory schedule =
                createSchedule(securityGroup.uuid)
        setSchedule(schedule, securityGroup.uuid)
        AccountInventory account = createAccount {
            name = "schedule-other-account"
            password = "password"
        } as AccountInventory
        SessionInventory session = logInByAccount {
            accountName = account.name
            password = "password"
        } as SessionInventory

        GetNetworkSecurityPolicyScheduleAction.Result getResult =
                new GetNetworkSecurityPolicyScheduleAction(
                        resourceUuid: securityGroup.uuid,
                        sessionId: session.uuid
                ).call()
        UpdateNetworkSecurityPolicyScheduleAction.Result updateResult =
                updateScheduleResult(
                        schedule, [name: "changed-by-other-account"], session.uuid)
        DeleteNetworkSecurityPolicyScheduleAction.Result deleteResult =
                new DeleteNetworkSecurityPolicyScheduleAction(
                        uuid: schedule.uuid,
                        sessionId: session.uuid
                ).call()
        SetNetworkSecurityPolicyScheduleAction.Result setResult = setScheduleResult(
                schedule.uuid, "SecurityGroup", securityGroup.uuid, session.uuid)
        SetNetworkSecurityPolicyScheduleAction.Result unsetResult = setScheduleResult(
                null, "SecurityGroup", securityGroup.uuid, session.uuid)

        assert getResult.error != null && updateResult.error != null && deleteResult.error != null &&
                setResult.error != null && unsetResult.error != null
        assert getSchedules(securityGroup.uuid)*.uuid == [schedule.uuid] &&
                scheduleUuidOf(securityGroup.uuid) == schedule.uuid

        CreateSecurityGroupAction.Result createGroupResult = new CreateSecurityGroupAction(
                name: "schedule-account-owned-sg",
                ipVersion: 4,
                sessionId: session.uuid
        ).call()
        assert createGroupResult.error == null
        SecurityGroupInventory ownedGroup = createGroupResult.value.inventory
        CreateNetworkSecurityPolicyScheduleAction.Result createResult = createScheduleResult(
                "SecurityGroup", ownedGroup.uuid, [name: "account-owned-schedule"], session.uuid)
        assert createResult.error == null
        NetworkSecurityPolicyScheduleInventory ownedSchedule = createResult.value.inventory

        SetNetworkSecurityPolicyScheduleAction.Result ownedSetResult = setScheduleResult(
                ownedSchedule.uuid, "SecurityGroup", ownedGroup.uuid, session.uuid)

        GetNetworkSecurityPolicyScheduleAction.Result ownedGetResult = new GetNetworkSecurityPolicyScheduleAction(
                resourceUuid: ownedGroup.uuid,
                sessionId: session.uuid
        ).call()
        UpdateNetworkSecurityPolicyScheduleAction.Result ownedUpdateResult = updateScheduleResult(
                ownedSchedule,
                [name: "account-updated-schedule"],
                session.uuid)
        SetNetworkSecurityPolicyScheduleAction.Result ownedUnsetResult = setScheduleResult(
                null, "SecurityGroup", ownedGroup.uuid, session.uuid)
        DeleteNetworkSecurityPolicyScheduleAction.Result ownedDeleteResult =
                new DeleteNetworkSecurityPolicyScheduleAction(
                        uuid: ownedSchedule.uuid,
                        sessionId: session.uuid
                ).call()
        assert ownedSetResult.error == null &&
                ownedGetResult.error == null &&
                ownedGetResult.value.inventories*.uuid == [ownedSchedule.uuid] &&
                ownedUpdateResult.error == null &&
                ownedUpdateResult.value.inventory.name == "account-updated-schedule" &&
                ownedUnsetResult.error == null &&
                ownedDeleteResult.error == null
    }

    @Override
    void test() {
        env.create {
            scheduleFacade = bean(NetworkSecurityPolicyScheduleFacade.class)
            scanTask = bean(NetworkSecurityPolicyScheduleScanTask.class)
            scanTask.stop()
            scheduleFacade.setClock(Clock.fixed(
                    Instant.parse("2026-07-29T08:00:00Z"), ZoneOffset.UTC))

            testTimeStatus()
            testGetFilters()
            testSecurityGroupScheduleLifecycle()
            testCreateApiValidation()
            testDescriptionLength()
            testRejectUninstalledResourceBackend()
            testUpdateApiValidation()
            testMultipleSchedulesPerResource()
            testConcurrentCreate()
            testConcurrentSet()
            testScheduleOwnership()
            testResourceCascadeDeletion()
            testAccountIsolation()
        }
    }
}
