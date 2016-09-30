package org.zstack.test.utils;

import org.junit.Test;
import org.zstack.utils.ShellUtils;

/**
 */
public class TestShellUtils {
    @Test
    public void test() {
        String res = ShellUtils.run("ls");
        System.out.println(res);
    }
}
