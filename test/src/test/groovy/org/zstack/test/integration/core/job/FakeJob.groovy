package org.zstack.test.integration.core.job

import org.zstack.core.job.Job
import org.zstack.header.core.ReturnValueCompletion
/**
 * Created by Administrator on 2017-03-24.
 */
abstract class FakeJob implements Job{

    FakeJob(){

    }

    @Override
    abstract void run(ReturnValueCompletion<Object> completion)
}
