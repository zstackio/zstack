package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.DateCountCache;

public class TestDateCountCache {
    @Test
    public void test() {
        DateCountCache cache = new DateCountCache(2019, 2019);
        assert cache.getCount(2019, 0, 1) == 0;
        assert cache.getCount(2019, 0, 2) == 0;
        assert cache.getCount(2020, 0, 2) == 0;
        assert cache.getCount(2018, 0, 2) == 0;

        cache.setCountUnsafe(2019, 0, 1, 2);
        assert cache.getCount(2019, 0, 1) == 2;
        assert cache.getCount(2019, 0) == 2;

        cache.addCountUnsafe(2019, 0, 2);
        assert cache.getCount(2019, 0, 2) == 1;
        assert cache.getCount(2019, 0) == 3;

        boolean exception = false;
        try {

            cache.setCountUnsafe(2020, 0, 1, 2);
        } catch (IndexOutOfBoundsException e) {
            exception = true;
        }
        assert exception;
        cache.expandIfNeed(null, 2020);
        cache.setCountUnsafe(2020, 0, 1, 2);
        assert cache.getCount(2020, 0, 1) == 2;
        assert cache.getCount(2019, 0, 2) == 1;
        assert cache.getCount(2019, 0) == 3;
    }
}
