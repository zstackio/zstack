package org.zstack.test.utils;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;

import java.io.File;
import java.io.IOException;

public class TestSshPut {
    CLogger logger = Utils.getLogger(TestSshPut.class);

    @Test
    public void test() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        String destPath = PathUtil.join(tmpDir, "TestSshPut");
        File destDirFile = new File(destPath);
        try {
            destDirFile.delete();
            destDirFile.mkdirs();
            File tmp = File.createTempFile("TestSshPut", "zstack");
            new Ssh().setHostname("localhost").setUsername("root").setPassword("password")
                    .scp(tmp.getAbsolutePath(), destPath).runErrorByException();
            File destFile = new File(PathUtil.join(destPath, tmp.getName()));
            Assert.assertTrue(destFile.exists());

            String srcDir = PathUtil.join(tmpDir, "testDir");
            File srcDirFile = new File(srcDir);
            srcDirFile.delete();
            srcDirFile.mkdirs();
            new Ssh().setHostname("localhost").setUsername("root").setPassword("password")
                    .scp(srcDir, destPath).runErrorByException();
            File destDir = new File(PathUtil.join(destPath, "testDir"));
            logger.debug(destDir.getAbsolutePath());
            Assert.assertTrue(destDir.isDirectory());
        } finally {
            destDirFile.delete();
        }
    }
}
