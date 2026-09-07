package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = NetworkSecurityPolicyScheduleVO.class)
public class NetworkSecurityPolicyScheduleInventory {
    private String uuid;
    private String name;
    private String description;
    private String resourceType;
    private String resourceUuid;
    private String timeType;
    private String repeatType;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private List<Integer> weekDays;
    private NetworkSecurityPolicyScheduleTimeStatus timeStatus;
    private boolean expiring;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static NetworkSecurityPolicyScheduleInventory valueOf(NetworkSecurityPolicyScheduleVO vo) {
        return valueOf(vo, Instant.now());
    }

    public static NetworkSecurityPolicyScheduleInventory __example__() {
        NetworkSecurityPolicyScheduleInventory inventory =
                new NetworkSecurityPolicyScheduleInventory();
        inventory.setUuid("4c4aa4f9b7254d76b48ad99f6a20c9ee");
        inventory.setName("office-hours");
        inventory.setDescription("Weekday office hours");
        inventory.setResourceType(
                NetworkSecurityPolicyScheduleConstant.SECURITY_GROUP_RESOURCE_TYPE);
        inventory.setResourceUuid("f1a72f89f9624c92a84cbd07347be003");
        inventory.setTimeType("UTC");
        inventory.setRepeatType("Weekly");
        inventory.setStartDate("2026-01-01");
        inventory.setEndDate("2026-12-31");
        inventory.setStartTime("09:00");
        inventory.setEndTime("18:00");
        inventory.setWeekDays(Arrays.asList(1, 2, 3, 4, 5));
        inventory.setTimeStatus(NetworkSecurityPolicyScheduleTimeStatus.InWindow);
        inventory.setExpiring(false);
        inventory.setCreateDate(Timestamp.valueOf("2026-01-01 00:00:00"));
        inventory.setLastOpDate(Timestamp.valueOf("2026-01-01 00:00:00"));
        return inventory;
    }

    public static NetworkSecurityPolicyScheduleInventory valueOf(NetworkSecurityPolicyScheduleVO vo, Instant now) {
        NetworkSecurityPolicyScheduleInventory inventory = new NetworkSecurityPolicyScheduleInventory();
        inventory.uuid = vo.getUuid();
        inventory.name = vo.getName();
        inventory.description = vo.getDescription();
        inventory.resourceType = vo.getResourceType();
        inventory.resourceUuid = vo.getResourceUuid();
        inventory.timeType = vo.getTimeType().name();
        inventory.repeatType = vo.getRepeatType().name();
        inventory.startDate = vo.getStartDate().toLocalDate().toString();
        inventory.endDate = vo.getEndDate().toLocalDate().toString();
        inventory.startTime = vo.getStartTime().toLocalTime()
                .truncatedTo(ChronoUnit.MINUTES).toString();
        inventory.endTime = vo.getEndTime().toLocalTime()
                .truncatedTo(ChronoUnit.MINUTES).toString();
        inventory.weekDays = toWeekDays(vo.getWeekDays());
        NetworkSecurityPolicyScheduleTime time = NetworkSecurityPolicyScheduleTime.valueOf(vo);
        inventory.timeStatus = time.status(now);
        inventory.expiring = time.isExpiring(now);
        inventory.createDate = vo.getCreateDate();
        inventory.lastOpDate = vo.getLastOpDate();
        return inventory;
    }

    public static List<NetworkSecurityPolicyScheduleInventory> valueOf(
            Collection<NetworkSecurityPolicyScheduleVO> vos) {
        return valueOf(vos, Instant.now());
    }

    public static List<NetworkSecurityPolicyScheduleInventory> valueOf(
            Collection<NetworkSecurityPolicyScheduleVO> vos, Instant now) {
        List<NetworkSecurityPolicyScheduleInventory> inventories = new ArrayList<>(vos.size());
        for (NetworkSecurityPolicyScheduleVO vo : vos) {
            inventories.add(valueOf(vo, now));
        }
        return inventories;
    }

    private static List<Integer> toWeekDays(String weekDays) {
        if (weekDays == null || weekDays.isEmpty()) {
            return null;
        }

        return Arrays.stream(weekDays.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public List<Integer> getWeekDays() {
        return weekDays;
    }

    public void setWeekDays(List<Integer> weekDays) {
        this.weekDays = weekDays;
    }

    public NetworkSecurityPolicyScheduleTimeStatus getTimeStatus() {
        return timeStatus;
    }

    public void setTimeStatus(NetworkSecurityPolicyScheduleTimeStatus timeStatus) {
        this.timeStatus = timeStatus;
    }

    public boolean isExpiring() {
        return expiring;
    }

    public void setExpiring(boolean expiring) {
        this.expiring = expiring;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
