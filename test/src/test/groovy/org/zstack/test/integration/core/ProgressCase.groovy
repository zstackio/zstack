package org.zstack.test.integration.core

import org.apache.logging.log4j.ThreadContext
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.progress.ProgressReportService
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

import static org.zstack.core.progress.ProgressReportService.reportProgress
import static org.zstack.header.Constants.THREAD_CONTEXT_API
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME

class ProgressCase extends SubCase {
    @Override
    void clean() {
        SQL.New(TaskProgressVO.class).delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        spring {
            include("Progress.xml")
        }
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        ProgressReportService progRpt = bean(ProgressReportService.class)
        def apiId = Platform.getUuid()
        ThreadContext.put(THREAD_CONTEXT_API, apiId)
        ThreadContext.put(THREAD_CONTEXT_TASK_NAME, "testTaskName")

        reportProgress("5")
        progRpt.reportProgressUntil("10", 10, TimeUnit.MILLISECONDS)
        retryInSecs {
            assert Q.New(TaskProgressVO.class).count() == 10 - 5 + 2
        }

        progRpt.reportProgressUntil("10", 10, TimeUnit.MILLISECONDS)
        sleep(500)
        assert Q.New(TaskProgressVO.class).count() == 10 - 5 + 3
        SQL.New(TaskProgressVO.class).delete()


        progRpt.reportProgressUntil("10", 10, TimeUnit.MILLISECONDS)
        sleep(1000)
        assert Q.New(TaskProgressVO.class).count() == 10 + 1
    }
}
