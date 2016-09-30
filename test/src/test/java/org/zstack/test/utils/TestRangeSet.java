package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.RangeSet;
import org.zstack.utils.RangeSet.Range;

import java.util.List;

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
}
