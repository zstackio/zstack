package org.zstack.test.integration.core.job;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.test.core.job.FakeJobConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by Administrator on 2017-03-24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class FakeJob implements Job{
    CLogger logger = Utils.getLogger(FakeJob.class);

    @JobContext
    protected long index ;
    @Autowired
    protected FakeJobConfig conf;
    @Autowired
    protected ErrorFacade errf;

    public FakeJob(){
    }

    public FakeJob(long i){
        index = i;
    }
}
