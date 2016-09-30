package org.zstack.test;

import org.junit.Test;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 4/22/2015.
 */
public class TestScriptRunner {
    @Test
    public void test() {
        SshResult ret = new Ssh().setHostname("localhost")
                .setUsername("root").setPassword("password")
                .script("scripts/check-public-dns-name.sh", map(e("dnsCheckList", "google.com"))).runAndClose();
        ret.raiseExceptionIfFailed();
    }
}
