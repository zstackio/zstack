package org.zstack.networksecuritypolicyschedule;

import java.sql.Date;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class NetworkSecurityPolicyScheduleTime {
    private static final Duration EXPIRING_WINDOW = Duration.ofHours(24);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseStrict()
                    .appendPattern("HH:mm")
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);

    private final NetworkSecurityPolicyScheduleTimeType timeType;
    private final NetworkSecurityPolicyScheduleRepeatType repeatType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final List<Integer> weekDays;

    NetworkSecurityPolicyScheduleTime(NetworkSecurityPolicyScheduleTimeType timeType,
                            NetworkSecurityPolicyScheduleRepeatType repeatType,
                            LocalDate startDate,
                            LocalDate endDate,
                            LocalTime startTime,
                            LocalTime endTime,
                            List<Integer> weekDays) {
        this.timeType = timeType;
        this.repeatType = repeatType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.weekDays = weekDays == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(weekDays));
    }

    static NetworkSecurityPolicyScheduleTime valueOf(NetworkSecurityPolicyScheduleVO vo) {
        List<Integer> weekDays = null;
        if (vo.getWeekDays() != null) {
            weekDays = new ArrayList<>();
            for (String value : vo.getWeekDays().split(",")) {
                weekDays.add(Integer.valueOf(value));
            }
        }

        return new NetworkSecurityPolicyScheduleTime(
                vo.getTimeType(),
                vo.getRepeatType(),
                vo.getStartDate().toLocalDate(),
                vo.getEndDate().toLocalDate(),
                vo.getStartTime().toLocalTime().truncatedTo(ChronoUnit.MINUTES),
                vo.getEndTime().toLocalTime().truncatedTo(ChronoUnit.MINUTES),
                weekDays);
    }

    static NetworkSecurityPolicyScheduleTime valueOf(String timeType,
                                           String repeatType,
                                           String startDate,
                                           String endDate,
                                           String startTime,
                                           String endTime,
                                           List<Integer> weekDays) {
        return new NetworkSecurityPolicyScheduleTime(
                NetworkSecurityPolicyScheduleTimeType.valueOf(timeType),
                NetworkSecurityPolicyScheduleRepeatType.valueOf(repeatType),
                parseDate(startDate),
                parseDate(endDate),
                parseTime(startTime),
                parseTime(endTime),
                weekDays);
    }

    static NetworkSecurityPolicyScheduleTime valueOf(NetworkSecurityPolicyScheduleTimeType timeType,
                                           NetworkSecurityPolicyScheduleRepeatType repeatType,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           LocalTime startTime,
                                           LocalTime endTime,
                                           List<Integer> weekDays) {
        return new NetworkSecurityPolicyScheduleTime(
                timeType, repeatType, startDate, endDate, startTime, endTime, weekDays);
    }

    static LocalDate parseDate(String value) {
        return LocalDate.parse(value, DATE_FORMATTER);
    }

    static LocalTime parseTime(String value) {
        return LocalTime.parse(value, TIME_FORMATTER);
    }

    void applyTo(NetworkSecurityPolicyScheduleVO vo) {
        vo.setTimeType(timeType);
        vo.setRepeatType(repeatType);
        vo.setStartDate(Date.valueOf(startDate));
        vo.setEndDate(Date.valueOf(endDate));
        vo.setStartTime(Time.valueOf(startTime));
        vo.setEndTime(Time.valueOf(endTime));
        vo.setWeekDays(weekDays.isEmpty()
                ? null : weekDays.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    boolean isInSchedule(Instant instant) {
        LocalDateTime current = currentDateTime(instant);
        if (repeatType == NetworkSecurityPolicyScheduleRepeatType.Once) {
            LocalDateTime start = LocalDateTime.of(startDate, startTime);
            LocalDateTime end = LocalDateTime.of(endDate, endTime);
            return !current.isBefore(start) && current.isBefore(end);
        }

        LocalDate date = current.toLocalDate();
        if (date.isBefore(startDate) || date.isAfter(endDate)
                || !weekDays.contains(current.getDayOfWeek().getValue())) {
            return false;
        }
        if (isAllDay()) {
            return true;
        }

        LocalTime time = current.toLocalTime();
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    boolean hasRemainingSchedule(Instant instant) {
        LocalDateTime current = currentDateTime(instant);
        if (repeatType == NetworkSecurityPolicyScheduleRepeatType.Once) {
            return current.isBefore(LocalDateTime.of(endDate, endTime));
        }

        LocalDate firstDate = current.toLocalDate().isAfter(startDate)
                ? current.toLocalDate() : startDate;
        if (firstDate.isAfter(endDate)) {
            return false;
        }
        LocalDate lastDate = firstDate.plusDays(7).isBefore(endDate)
                ? firstDate.plusDays(7) : endDate;

        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            if (!weekDays.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            if (date.isAfter(current.toLocalDate()) || isAllDay()
                    || current.toLocalTime().isBefore(endTime)) {
                return true;
            }
        }
        return false;
    }

    NetworkSecurityPolicyScheduleTimeStatus status(Instant instant) {
        LocalDateTime current = currentDateTime(instant);
        boolean started = repeatType == NetworkSecurityPolicyScheduleRepeatType.Once
                ? !current.isBefore(LocalDateTime.of(startDate, startTime))
                : !current.toLocalDate().isBefore(startDate);
        if (!started) {
            return NetworkSecurityPolicyScheduleTimeStatus.NotStarted;
        }
        if (isInSchedule(instant)) {
            return NetworkSecurityPolicyScheduleTimeStatus.InWindow;
        }
        return hasRemainingSchedule(instant)
                ? NetworkSecurityPolicyScheduleTimeStatus.OutOfWindow
                : NetworkSecurityPolicyScheduleTimeStatus.Ended;
    }

    boolean isExpiring(Instant instant) {
        long remainingSeconds = Duration.between(
                instant.truncatedTo(ChronoUnit.MINUTES), expirationInstant()).getSeconds();
        return remainingSeconds > 0 && remainingSeconds < EXPIRING_WINDOW.getSeconds();
    }

    boolean hasScheduledDay() {
        if (repeatType == NetworkSecurityPolicyScheduleRepeatType.Once) {
            return true;
        }

        LocalDate lastDate = startDate.plusDays(6).isBefore(endDate)
                ? startDate.plusDays(6) : endDate;
        for (LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            if (weekDays.contains(date.getDayOfWeek().getValue())) {
                return true;
            }
        }
        return false;
    }

    Instant expirationInstant() {
        if (repeatType == NetworkSecurityPolicyScheduleRepeatType.Once) {
            return LocalDateTime.of(endDate, endTime).atZone(zoneId()).toInstant();
        }

        LocalDate lastScheduledDate = null;
        for (int offset = 0; offset < 7; offset++) {
            LocalDate candidate = endDate.minusDays(offset);
            if (candidate.isBefore(startDate)) {
                break;
            }
            if (weekDays.contains(candidate.getDayOfWeek().getValue())) {
                lastScheduledDate = candidate;
                break;
            }
        }
        if (lastScheduledDate == null) {
            throw new IllegalStateException("Weekly schedule has no scheduled day");
        }

        LocalDateTime expiration = isAllDay()
                ? lastScheduledDate.plusDays(1).atStartOfDay()
                : LocalDateTime.of(lastScheduledDate, endTime);
        return expiration.atZone(zoneId()).toInstant();
    }

    private LocalDateTime currentDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant.truncatedTo(ChronoUnit.MINUTES), zoneId());
    }

    private ZoneId zoneId() {
        return timeType == NetworkSecurityPolicyScheduleTimeType.UTC
                ? ZoneOffset.UTC : ZoneId.systemDefault();
    }

    boolean isAllDay() {
        return startTime.equals(LocalTime.MIDNIGHT) && endTime.equals(LocalTime.MIDNIGHT);
    }

    NetworkSecurityPolicyScheduleTimeType getTimeType() {
        return timeType;
    }

    NetworkSecurityPolicyScheduleRepeatType getRepeatType() {
        return repeatType;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    LocalTime getStartTime() {
        return startTime;
    }

    LocalTime getEndTime() {
        return endTime;
    }

    List<Integer> getWeekDays() {
        return weekDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetworkSecurityPolicyScheduleTime)) {
            return false;
        }
        NetworkSecurityPolicyScheduleTime that = (NetworkSecurityPolicyScheduleTime) o;
        return timeType == that.timeType
                && repeatType == that.repeatType
                && Objects.equals(startDate, that.startDate)
                && Objects.equals(endDate, that.endDate)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && Objects.equals(weekDays, that.weekDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeType, repeatType, startDate, endDate, startTime, endTime, weekDays);
    }
}
