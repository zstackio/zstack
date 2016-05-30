package org.zstack.test.mevoco.logging;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/2.
 */
public class LogLabelTest {
    @LogLabel(messages = {
            "en_US = test1",
            "zh_CN = 测试1"
    })
    public static String TEST1 = "test1";
}
