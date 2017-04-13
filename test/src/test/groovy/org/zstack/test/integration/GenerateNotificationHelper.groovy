package org.zstack.test.integration

import org.junit.Test
import org.zstack.testlib.NotificationGenerator

/**
 * Created by xing5 on 2017/3/15.
 */
class GenerateNotificationHelper {
    @Test
    void test() {
        new NotificationGenerator().generate()
    }
}
