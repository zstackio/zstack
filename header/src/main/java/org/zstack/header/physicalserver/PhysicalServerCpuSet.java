package org.zstack.header.physicalserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class PhysicalServerCpuSet {
    private PhysicalServerCpuSet() {
    }

    public static String normalize(String value) {
        return formatRanges(parseRanges(value));
    }

    public static SortedSet<Integer> parse(String value, Set<Integer> allowed) {
        List<Range> ranges = parseRanges(value);
        SortedSet<Integer> cpus = new TreeSet<>();
        for (Range range : ranges) {
            long width = (long) range.end - range.start + 1;
            if (width > allowed.size()) {
                throw new IllegalArgumentException(String.format(
                        "CPU set range[%s-%s] contains CPUs outside the online topology", range.start, range.end));
            }
            for (long cursor = range.start; cursor <= range.end; cursor++) {
                int cpu = (int) cursor;
                if (!allowed.contains(cpu)) {
                    throw new IllegalArgumentException(String.format("CPU[%s] is outside the online topology", cpu));
                }
                cpus.add(cpu);
            }
        }
        return cpus;
    }

    public static SortedSet<Integer> parse(String value) {
        List<Range> ranges = parseRanges(value);
        SortedSet<Integer> cpus = new TreeSet<>();
        for (Range range : ranges) {
            long width = (long) range.end - range.start + 1;
            if (width > 1048576) {
                throw new IllegalArgumentException(String.format(
                        "CPU set range[%s-%s] is too large", range.start, range.end));
            }
            for (long cursor = range.start; cursor <= range.end; cursor++) {
                cpus.add((int) cursor);
            }
        }
        return cpus;
    }

    public static String format(Collection<Integer> cpus) {
        if (cpus == null || cpus.isEmpty()) {
            return "";
        }
        List<Integer> sorted = new ArrayList<>(new LinkedHashSet<>(cpus));
        Collections.sort(sorted);
        List<Range> ranges = new ArrayList<>();
        int start = sorted.get(0);
        int end = start;
        for (int index = 1; index < sorted.size(); index++) {
            int current = sorted.get(index);
            if ((long) current == (long) end + 1) {
                end = current;
            } else {
                ranges.add(new Range(start, end));
                start = current;
                end = current;
            }
        }
        ranges.add(new Range(start, end));
        return formatRanges(ranges);
    }

    public static int count(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        long count = 0;
        for (Range range : parseRanges(value)) {
            count += (long) range.end - range.start + 1;
            if (count > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("CPU set contains too many CPUs");
            }
        }
        return (int) count;
    }

    public static String union(String left, String right) {
        boolean leftEmpty = left == null || left.trim().isEmpty();
        boolean rightEmpty = right == null || right.trim().isEmpty();
        if (leftEmpty && rightEmpty) {
            return "";
        }
        if (leftEmpty) {
            return normalize(right);
        }
        if (rightEmpty) {
            return normalize(left);
        }
        return normalize(left + "," + right);
    }

    public static String firstAvailable(PhysicalServerCpuTopology topology, Set<Integer> unavailable, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("CPU count must be greater than zero");
        }
        SortedSet<Integer> available = topology.getOnlineCpus();
        if (unavailable != null) {
            available.removeAll(unavailable);
        }
        List<Integer> selected = new ArrayList<>();
        for (Integer cpu : available) {
            selected.add(cpu);
            if (selected.size() == count) {
                break;
            }
        }
        return format(selected);
    }

    public static String firstAvailableExcludingCpuZeroCore(
            PhysicalServerCpuTopology topology, Set<Integer> unavailable, int count) {
        Set<Integer> excluded = unavailable == null ? new HashSet<>() : new HashSet<>(unavailable);
        excluded.addAll(topology.getCpuZeroGroup().getCpus());
        Map<String, SortedSet<Integer>> cpusByNuma = new TreeMap<>();
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            cpusByNuma.computeIfAbsent(group.getNumaId(), ignored -> new TreeSet<>()).addAll(group.getCpus());
        }
        for (SortedSet<Integer> numaCpus : cpusByNuma.values()) {
            numaCpus.removeAll(excluded);
            if (numaCpus.size() >= count) {
                return format(new ArrayList<>(numaCpus).subList(0, count));
            }
        }
        return firstAvailable(topology, excluded, count);
    }

    private static List<Range> parseRanges(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("CPU set cannot be empty");
        }

        List<Range> ranges = new ArrayList<>();
        for (String item : value.split(",")) {
            String part = item.trim();
            if (part.isEmpty()) {
                throw new IllegalArgumentException("CPU set contains an empty item");
            }

            String[] range = part.split("-", -1);
            if (range.length > 2) {
                throw new IllegalArgumentException(String.format("Invalid CPU set item[%s]", part));
            }

            int start = parseCpu(range[0]);
            int end = range.length == 1 ? start : parseCpu(range[1]);
            if (start > end) {
                throw new IllegalArgumentException(String.format("Invalid CPU set range[%s]", part));
            }
            ranges.add(new Range(start, end));
        }

        ranges.sort(Comparator.comparingInt(range -> range.start));
        List<Range> mergedRanges = new ArrayList<>();
        Range merged = ranges.get(0);
        for (int index = 1; index < ranges.size(); index++) {
            Range current = ranges.get(index);
            if ((long) current.start <= (long) merged.end + 1) {
                merged.end = Math.max(merged.end, current.end);
            } else {
                mergedRanges.add(merged);
                merged = current;
            }
        }
        mergedRanges.add(merged);
        return mergedRanges;
    }

    private static String formatRanges(List<Range> ranges) {
        StringBuilder result = new StringBuilder();
        for (Range range : ranges) {
            appendRange(result, range.start, range.end);
        }
        return result.toString();
    }

    private static int parseCpu(String value) {
        String normalized = value.trim();
        if (!normalized.matches("[0-9]+")) {
            throw new IllegalArgumentException(String.format("Invalid CPU id[%s]", value));
        }
        BigInteger cpu = new BigInteger(normalized);
        if (cpu.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException(String.format("CPU id[%s] is too large", value));
        }
        return cpu.intValue();
    }

    private static void appendRange(StringBuilder result, int start, int end) {
        if (result.length() > 0) {
            result.append(',');
        }
        result.append(start);
        if (start != end) {
            result.append('-').append(end);
        }
    }

    private static class Range {
        private final int start;
        private int end;

        private Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
