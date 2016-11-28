package org.zstack.test.utils;

import org.junit.Test;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

public class TestSshCheckToolFailure {
    @Test(expected = SshException.class)
    public void test() {
        SshResult res = new Ssh().setHostname("localhost").setUsername("root").setPassword("password")
                .checkTool("ls", "this_tool_is_not_existing", "cp", "mv", "not_existing_too").run();
        res.raiseExceptionIfFailed();
    }

    @Test(expected = SshException.class)
    public void test1() {
        SshResult res = new Ssh().setHostname("localhost").setUsername("root").setPassword("password")
                .checkTool("ls", "cp", "mv", "this_tool_is_not_existing", "rm").run();
        res.raiseExceptionIfFailed();
    }
}
