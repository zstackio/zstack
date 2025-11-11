package org.zstack.test;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/**
 * Created by frank on 4/22/2015.
 */
public class TestScriptRunner {
    @Test
    public void test() throws Exception {
        String scriptPath = PathUtil.findFileOnClassPath("scripts/check-public-dns-name.sh", true).getAbsolutePath();
        String contents = FileUtils.readFileToString(new File(scriptPath), StandardCharsets.UTF_8);
        String scriptContent = s(contents).formatByMap(map(e("dnsCheckList", "google.com")));

        SshResult ret = new Ssh().setHostname("localhost")
                .setUsername("root").setPassword("password")
                .shell(scriptContent).runAndClose();
        ret.raiseExceptionIfFailed();
    }
}
