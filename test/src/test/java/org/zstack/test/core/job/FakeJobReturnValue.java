package org.zstack.test.core.job;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FakeJobReturnValue implements Job {
    CLogger logger = Utils.getLogger(FakeJobReturnValue.class);
    
    @JobContext
    private long index;
    @Autowired
    private FakeJobConfig fl;
    
    private FakeJobReturnValue() {
    }
    
    public FakeJobReturnValue(long index) {
        this.index = index;
    }
    
    @Override
    public void run(ReturnValueCompletion<Object> complete) {
        try {
            logger.debug(String.format("job %s is executing", index));
        } finally {
            complete.success(index);
        }
    }
}
