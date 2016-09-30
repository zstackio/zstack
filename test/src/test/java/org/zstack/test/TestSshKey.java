package org.zstack.test;

import org.junit.Test;
import org.zstack.utils.ShellUtils;

/**
 */
public class TestSshKey {

    @Test
    public void test() {
        ShellUtils.run("ssh-copy-id 192.168.0.204 -i ~/.ssh/id_ras.pub");
    }
}
