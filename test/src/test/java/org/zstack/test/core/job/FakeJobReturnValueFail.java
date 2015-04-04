package org.zstack.test.core.job;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FakeJobReturnValueFail implements Job {
    CLogger logger = Utils.getLogger(FakeJobReturnValueFail.class);

    @JobContext
    private long index;
    @Autowired
    private FakeJobConfig fl;
    @Autowired
    private ErrorFacade errf;

    private FakeJobReturnValueFail() {
    }

    public FakeJobReturnValueFail(long index) {
        this.index = index;
    }

    @Override
    public void run(ReturnValueCompletion<Object> complete) {
        logger.debug(String.format("job %s is executing", index));
        complete.fail(errf.stringToOperationError("fail on purpose"));
    }
}
