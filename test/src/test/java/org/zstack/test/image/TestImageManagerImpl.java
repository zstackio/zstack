package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.image.ImageGlobalConfig;

/**
 * Created by mingjian.deng on 16/11/2.
 */
public class TestImageManagerImpl {
    @Test
    public void test() {
//        boolean enableInject = ImageGlobalConfig.DELETION_POLICY.value();
        String test = ImageGlobalConfig.DELETION_POLICY.value();
        System.out.println(test);
        Assert.assertTrue(test == "Delay");
//        Assert.assertNotNull(enableInject);
//        Assert.assertTrue("need true", enableInject);
    }
}
