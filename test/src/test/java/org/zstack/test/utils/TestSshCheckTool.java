package org.zstack.test.utils;

import org.junit.Test;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

public class TestSshCheckTool {
    @Test
    public void test() {
        SshResult res = new Ssh().setHostname("localhost").setUsername("root").setPassword("password")
                .checkTool("ls", "cp", "mv", "rm").run();
        res.raiseExceptionIfFailed();
    }
}
