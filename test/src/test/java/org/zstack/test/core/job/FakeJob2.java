package org.zstack.test.core.job;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Random;
import java.util.concurrent.TimeUnit;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FakeJob2 implements Job {
    CLogger logger = Utils.getLogger(FakeJob2.class);
    
    @JobContext
    private long index;
    @Autowired
    private FakeJobConfig fl;
    
    public FakeJob2() {
    }
    
    @Override
    public void run(ReturnValueCompletion<Object> complete) {
        try {
            fl.flag = new Random().nextInt();
            int v = fl.flag;
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }
            if (v != fl.flag) {
                fl.success = false;
            }
        } finally {
            complete.success(null);
        }
    }
}
