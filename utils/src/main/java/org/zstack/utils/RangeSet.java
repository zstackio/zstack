package org.zstack.utils;

import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
public class RangeSet {
    public static class Range {
        private long start;
        private long end;
        private Boolean isSystem;

        public Range(long start, long end) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
            this.isSystem = false;
        }

        public Range(long start, long end, Boolean isSystem) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
            this.isSystem = isSystem;
        }

        public Boolean getSystem() {
            return isSystem;
        }

        public void setSystem(Boolean system) {
            isSystem = system;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        public boolean isOverlap(Range r) {
            return (start <= r.end && r.end <= end) || (start <= r.start && r.start <= end);
        }

        public boolean isConnected(Range r) {
            return end + 1 == r.getStart() || r.getEnd() + 1 == start;
        }

        public void merge(Range r) {
            DebugUtils.Assert(isOverlap(r) || isConnected(r), String.format("range %s is not overlap with %s", toString(), r.toString()));
            start = Math.min(start, r.start);
            end = Math.max(end, r.end);
        }

        public boolean is(long s, long e) {
            return start == s && end == e;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", start, end);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Range range = (Range) obj;
            return start == range.start && end == range.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    private List<Range> ranges = new ArrayList<Range>();

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public RangeSet open(long s, long e) {
        ranges.add(new Range(s+1, e-1));
        return this;
    }

    public RangeSet closed(long s, long e) {
        ranges.add(new Range(s, e));
        return this;
    }

    public RangeSet openClosed(long s, long e) {
        ranges.add(new Range(s+1, e));
        return this;
    }

    public RangeSet closedOpen(long s, long e) {
        ranges.add(new Range(s, e-1));
        return this;
    }

    public List<Range> merge() {
        List<Range> ret = new ArrayList<Range>();
        if (ranges.isEmpty()) {
            return ret;
        }

        sort();

        Range r = ranges.get(0);
        for (int i=1; i<ranges.size(); i++) {
            Range r1 = ranges.get(i);
            if (r.isOverlap(r1) || r.isConnected(r1)) {
                r.merge(r1);
            } else {
                ret.add(r);
                r = r1;
            }
        }
        ret.add(r);
        ranges = ret;
        return ret;
    }

    public List<Range> mergeAndSort() {
        merge();
        sort();
        return ranges;
    }

    public void sort(){
        Collections.sort(ranges, new Comparator<Range>() {
            @Override
            public int compare(Range o1, Range o2) {
                return Long.compare(o1.start, o2.start);
            }
        });
    }

    public List<String> sortAndToString() {
        List<String> strList = new ArrayList<String>();
        if (ranges.isEmpty()){
            return strList;
        }

        sort();

        Iterator<Range> it = ranges.iterator();
        while (it.hasNext()){
            Range range = it.next();
            for (long i = range.start; i <= range.end; i++){
                strList.add(Long.toString(i));
            }
        }

        return strList;
    }

    public static RangeSet valueOf(Collection<Long> numbers) {
        RangeSet results = new RangeSet();
        if (numbers.isEmpty()) {
            return results;
        }

        List<Long> asc = numbers.stream().sorted().distinct().collect(Collectors.toList());
        Long begin = asc.remove(0);
        Long end = begin;
        for (long n : asc) {
            long lastEnd = end;
            if (n != ++end) {
                results.ranges.add(new Range(begin, lastEnd));
                begin = n;
                end = n;
            }
        }
        results.ranges.add(new Range(begin, end));
        return results;
    }

    public static RangeSet valueOf(String str) {
        RangeSet results = new RangeSet();
        if (str == null || str.isEmpty()) {
            return results;
        }

        for (String s : str.split(",")) {
            if (Strings.isBlank(s)) {
                continue;
            }
            String[] range = s.split("-");
            try {
                if (range.length == 1) {
                    long value = Long.parseLong(range[0].trim());
                    results.closed(value, value);
                } else if (range.length == 2) {
                    String start = range[0].trim();
                    String end = range[1].trim();
                    results.closed(Long.parseLong(start), Long.parseLong(end));
                }
            } catch (NumberFormatException e) {
                /* intentionally ignored: skip unparseable range segments */
            }
        }

        return results;
    }

    public Set<Long> values(){
        Set<Long> result = new HashSet<>();
        for (Range range : ranges) {
            for (long i = range.start; i <= range.end; i++) {
                result.add(i);
            }
        }
        return result;
    }
}
