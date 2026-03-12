package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.RangeSet;
import org.zstack.utils.RangeSet.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 */
public class TestRangeSet {
    @Test
    public void test() {
        RangeSet set = new RangeSet();
        set.closed(1, 100);
        set.closed(101, 110);
        set.open(22, 33);
        set.closed(120, 130);
        List<Range> ret = set.mergeAndSort();
        System.out.println(ret.toString());
        Assert.assertEquals(2, ret.size());

        Range r1 = ret.get(0);
        Assert.assertTrue(r1.is(1, 110));
        Range r2 = ret.get(1);
        Assert.assertTrue(r2.is(120, 130));

        set = new RangeSet();
        set.closed(22, 22);
        set.closed(33, 25);
        set.closed(1, 23);
        ret = set.mergeAndSort();
        System.out.println(ret.toString());
        Assert.assertEquals(2, ret.size());

        r1 = ret.get(0);
        Assert.assertTrue(r1.is(1, 23));
        r2 = ret.get(1);
        Assert.assertTrue(r2.is(25, 33));

        set = new RangeSet();
        set.closed(1, 1);
        set.closed(2, 10);
        set.closed(10, 20);
        ret = set.mergeAndSort();
        System.out.println(ret.toString());
        Assert.assertEquals(1, ret.size());
        r1 = ret.get(0);
        Assert.assertTrue(r1.is(1, 20));
    }

    @Test
    public void testValueOfCollection() {
        // Test empty collection
        Collection<Long> emptyCollection = new ArrayList<>();
        RangeSet emptyRangeSet = RangeSet.valueOf(emptyCollection);
        assertTrue(emptyRangeSet.getRanges().isEmpty());

        // Test single element
        Collection<Long> singleElement = Collections.singletonList(5L);
        RangeSet singleRangeSet = RangeSet.valueOf(singleElement);
        assertEquals(1, singleRangeSet.getRanges().size());
        assertTrue(singleRangeSet.getRanges().get(0).is(5, 5));

        // Test continuous elements
        Collection<Long> continuousElements = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        RangeSet continuousRangeSet = RangeSet.valueOf(continuousElements);
        assertEquals(1, continuousRangeSet.getRanges().size());
        assertTrue(continuousRangeSet.getRanges().get(0).is(1, 5));

        // Test discontinuous elements
        Collection<Long> discontinuousElements = Arrays.asList(1L, 2L, 4L, 5L, 8L);
        RangeSet discontinuousRangeSet = RangeSet.valueOf(discontinuousElements);
        assertEquals(3, discontinuousRangeSet.getRanges().size());
        assertTrue(discontinuousRangeSet.getRanges().get(0).is(1, 2));
        assertTrue(discontinuousRangeSet.getRanges().get(1).is(4, 5));
        assertTrue(discontinuousRangeSet.getRanges().get(2).is(8, 8));

        // Test duplicate elements
        Collection<Long> duplicateElements = Arrays.asList(1L, 1L, 2L, 3L, 3L, 5L);
        RangeSet duplicateRangeSet = RangeSet.valueOf(duplicateElements);
        assertEquals(2, duplicateRangeSet.getRanges().size());
        assertTrue(duplicateRangeSet.getRanges().get(0).is(1, 3));
        assertTrue(duplicateRangeSet.getRanges().get(1).is(5, 5));

        // Test unsorted elements
        Collection<Long> unsortedElements = Arrays.asList(5L, 3L, 1L, 2L, 4L);
        RangeSet unsortedRangeSet = RangeSet.valueOf(unsortedElements);
        assertEquals(1, unsortedRangeSet.getRanges().size());
        assertTrue(unsortedRangeSet.getRanges().get(0).is(1, 5));
    }

    @Test
    public void testValueOfString() {
        // Test empty string
        RangeSet emptyRangeSet = RangeSet.valueOf("");
        assertTrue(emptyRangeSet.getRanges().isEmpty());

        // Test single number
        RangeSet singleNumRangeSet = RangeSet.valueOf("5");
        assertEquals(1, singleNumRangeSet.getRanges().size());
        assertTrue(singleNumRangeSet.getRanges().get(0).is(5, 5));

        // Test single range
        RangeSet singleRangeRangeSet = RangeSet.valueOf("1-5");
        assertEquals(1, singleRangeRangeSet.getRanges().size());
        assertTrue(singleRangeRangeSet.getRanges().get(0).is(1, 5));

        // Test multiple ranges
        RangeSet multiRangeRangeSet = RangeSet.valueOf("1-3,5-7,9");
        assertEquals(3, multiRangeRangeSet.getRanges().size());
        assertTrue(multiRangeRangeSet.getRanges().get(0).is(1, 3));
        assertTrue(multiRangeRangeSet.getRanges().get(1).is(5, 7));
        assertTrue(multiRangeRangeSet.getRanges().get(2).is(9, 9));
    }

    @Test
    public void testMerge() {
        // Test empty range set
        RangeSet emptyRangeSet = new RangeSet();
        List<RangeSet.Range> emptyResult = emptyRangeSet.merge();
        assertTrue(emptyResult.isEmpty());

        // Test single range
        RangeSet singleRangeSet = new RangeSet().closed(1, 5);
        List<RangeSet.Range> singleResult = singleRangeSet.merge();
        assertEquals(1, singleResult.size());
        assertTrue(singleResult.get(0).is(1, 5));

        // Test overlapping ranges
        RangeSet overlappingRangeSet = new RangeSet()
                .closed(1, 5)
                .closed(3, 8);
        List<RangeSet.Range> overlappingResult = overlappingRangeSet.merge();
        assertEquals(1, overlappingResult.size());
        assertTrue(overlappingResult.get(0).is(1, 8));

        // Test connected ranges
        RangeSet connectedRangeSet = new RangeSet()
                .closed(1, 5)
                .closed(6, 10);
        List<RangeSet.Range> connectedResult = connectedRangeSet.merge();
        assertEquals(1, connectedResult.size());
        assertTrue(connectedResult.get(0).is(1, 10));

        // Test disjoint ranges (no overlap, no connection)
        RangeSet disjointRangeSet = new RangeSet()
                .closed(1, 5)
                .closed(7, 10);
        List<RangeSet.Range> disjointResult = disjointRangeSet.merge();
        assertEquals(2, disjointResult.size());
        assertTrue(disjointResult.get(0).is(1, 5));
        assertTrue(disjointResult.get(1).is(7, 10));

        // Test mixed scenario with multiple ranges
        RangeSet mixedRangeSet = new RangeSet()
                .closed(1, 5)
                .closed(4, 8)
                .closed(10, 15)
                .closed(16, 20)
                .closed(25, 30);
        List<RangeSet.Range> mixedResult = mixedRangeSet.merge();
        assertEquals(3, mixedResult.size());
        assertTrue(mixedResult.get(0).is(1, 8));
        assertTrue(mixedResult.get(1).is(10, 20));
        assertTrue(mixedResult.get(2).is(25, 30));
    }

    @Test
    public void testMergeAndSort() {
        // Test already ordered ranges
        RangeSet orderedRangeSet = new RangeSet()
                .closed(1, 5)
                .closed(7, 10);
        List<RangeSet.Range> orderedResult = orderedRangeSet.mergeAndSort();
        assertEquals(2, orderedResult.size());
        assertTrue(orderedResult.get(0).is(1, 5));
        assertTrue(orderedResult.get(1).is(7, 10));

        // Test unordered ranges
        RangeSet unorderedRangeSet = new RangeSet()
                .closed(7, 10)
                .closed(1, 5);
        List<RangeSet.Range> unorderedResult = unorderedRangeSet.mergeAndSort();
        assertEquals(2, unorderedResult.size());
        assertTrue(unorderedResult.get(0).is(1, 5));
        assertTrue(unorderedResult.get(1).is(7, 10));

        // Test ranges requiring both merging and sorting
        RangeSet mixedRangeSet = new RangeSet()
                .closed(15, 20)
                .closed(5, 10)
                .closed(8, 16);
        List<RangeSet.Range> mixedResult = mixedRangeSet.mergeAndSort();
        assertEquals(1, mixedResult.size());
        assertTrue(mixedResult.get(0).is(5, 20));
    }

    @Test
    public void testClosed() {
        // Test normal case
        RangeSet rangeSet = new RangeSet();
        RangeSet result = rangeSet.closed(1, 5);

        // Verify return value is this (for chaining)
        assertSame(rangeSet, result);

        // Verify range is correctly added
        assertEquals(1, rangeSet.getRanges().size());
        assertTrue(rangeSet.getRanges().get(0).is(1, 5));

        // Test multiple calls
        rangeSet.closed(7, 10);
        assertEquals(2, rangeSet.getRanges().size());
        assertTrue(rangeSet.getRanges().get(0).is(1, 5));
        assertTrue(rangeSet.getRanges().get(1).is(7, 10));

        // Test when start > end
        rangeSet.closed(15, 12);
        assertEquals(3, rangeSet.getRanges().size());
        assertTrue(rangeSet.getRanges().get(2).is(12, 15)); // Should automatically swap order
    }
}
