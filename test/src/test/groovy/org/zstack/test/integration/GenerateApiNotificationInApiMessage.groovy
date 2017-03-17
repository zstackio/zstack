package org.zstack.test.integration

import org.junit.Test
import org.zstack.testlib.ApiNotificationGenerator

/**
 * Created by xing5 on 2017/3/17.
 */
class GenerateApiNotificationInApiMessage {
    @Test
    void test() {
        new ApiNotificationGenerator().generate()
    }
}
