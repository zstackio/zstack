package org.zstack.test.multinodes;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SilentJob implements Job {
    private static final CLogger logger = Utils.getLogger(SilentJob.class);
    public static String RESULT_FILE = "/tmp/zstacksilentjobresult";
    public static String PASS_FOLDER = "/tmp/zstacksilentjobpassfolder";


    @JobContext
    private String uuid;

    private static File resultFile = new File(RESULT_FILE);

    static {
        try {
            resultFile.delete();
            if (resultFile.exists()) {
                FileUtils.deleteDirectory(resultFile);
            }
            FileUtils.forceMkdir(resultFile);
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }


    private void writeResult(String uuid) {
        try {
            File f = new File(PathUtil.join(resultFile.getAbsolutePath(), uuid));
            f.createNewFile();
            logger.debug(String.format("create result file: %s", f.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(final ReturnValueCompletion<Object> completion) {
        int timeout = 120;
        int count = 0;
        while (count < timeout) {
            File pass = new File(PathUtil.join(PASS_FOLDER, uuid));
            if (pass.exists()) {
                logger.debug(String.format("silent job[uuid:%s] is ready to return", uuid));
                writeResult(uuid);
                completion.success(null);
                return;
            } else {
                logger.debug(String.format("silent job[uuid:%s] is not ready to return", uuid));
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            count++;
        }
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
