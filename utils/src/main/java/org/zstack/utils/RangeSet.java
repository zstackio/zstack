package org.zstack.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 */
public class RangeSet {
    public static class Range {
        private long start;
        private long end;

        public Range(long start, long end) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
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
    }

    private List<Range> ranges = new ArrayList<Range>();

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

        Collections.sort(ranges, new Comparator<Range>() {
            @Override
            public int compare(Range o1, Range o2) {
                return (int)(o1.start - o2.start);
            }
        });

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
        return ret;
    }

    public List<Range> mergeAndSort() {
        List<Range> ret = merge();
        Collections.sort(ranges, new Comparator<Range>() {
            @Override
            public int compare(Range o1, Range o2) {
                return (int)(o1.start - o2.start);
            }
        });
        return ret;
    }
}
