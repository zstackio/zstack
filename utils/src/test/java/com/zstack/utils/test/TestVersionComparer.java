package com.zstack.utils.test;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.VersionComparer;

import java.io.IOException;

/**
 * Created by frank on 8/11/2015.
 */
public class TestVersionComparer {
    @Test
    public void test() throws IOException {
        Assert.assertTrue(new VersionComparer("1.0").compare("0.1") > 0);
        Assert.assertTrue(new VersionComparer("1.0").compare("1.0") == 0);
        Assert.assertTrue(new VersionComparer("1.0").compare("1.2") < 0);

        Assert.assertTrue(new VersionComparer("1.0").compare("1") == 0);
        Assert.assertTrue(new VersionComparer("1.0.1").compare("1.1") < 0);
        Assert.assertTrue(new VersionComparer("1.2").compare("1.1.3") > 0);

        Assert.assertTrue(new VersionComparer("1.0.0.0.0").compare("1") == 0);
    }
}
