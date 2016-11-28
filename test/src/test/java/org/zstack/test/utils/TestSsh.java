package org.zstack.test.utils;

import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;

public class TestSsh {
    CLogger logger = Utils.getLogger(TestSsh.class);

    @Test
    public void test() {
        Ssh ssh = new Ssh().setHostname("192.168.0.203").setUsername("root").setPassword("password");
        ssh.reset().checkTool("scp").run();
        ssh.reset().checkTool("salt-minion").run();
        ssh.reset().command(String.format("md5sum /etc/salt/minion")).run();
        ssh.reset().command("service salt-minion start").run();
        ssh.close();
    }
}
