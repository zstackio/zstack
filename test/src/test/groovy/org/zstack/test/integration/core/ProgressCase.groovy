package org.zstack.test.integration.core

import org.apache.logging.log4j.ThreadContext
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.core.progress.ProgressReportService
import org.zstack.header.core.progress.TaskProgressRange
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.core.progress.TaskProgressVO_
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

import static org.zstack.core.progress.ProgressReportService.*
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
        testReportUntil()
        testParallelTaskStage()
    }

    void testReportUntil() {
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

    void testParallelTaskStage() {
        SQL.New(TaskProgressVO.class).delete()
        def apiId = Platform.getUuid()
        ThreadContext.put(THREAD_CONTEXT_API, apiId)
        ThreadContext.put(THREAD_CONTEXT_TASK_NAME, "testTaskName")

        def stage = markParallelTaskStage(getTaskStage(), new TaskProgressRange(0, 90), [2, 3, 4])

        List<Thread> threads = []
        threads.add(Thread.start {
            ThreadContext.put(THREAD_CONTEXT_API, apiId)
            ThreadContext.put(THREAD_CONTEXT_TASK_NAME, "testTaskName")
            stage.markSubStage()
            sleep(1000)

            reportProgress("0")
            reportProgress("10")
            sleep(3)
            reportProgress("20")
        })
        threads.add(Thread.start {
            ThreadContext.put(THREAD_CONTEXT_API, apiId)
            ThreadContext.put(THREAD_CONTEXT_TASK_NAME, "testTaskName")
            stage.markSubStage()
            sleep(1000)

            reportProgress("20")
            sleep(1)
            reportProgress("30")
            sleep(2)
            reportProgress("40")
            sleep(1)
            reportProgress("50")
        })
        threads.add(Thread.start {
            ThreadContext.put(THREAD_CONTEXT_API, apiId)
            ThreadContext.put(THREAD_CONTEXT_TASK_NAME, "testTaskName")
            stage.markSubStage()
            sleep(1000)

            reportProgress("50")
            sleep(1)
            reportProgress("60")
            sleep(1)
            reportProgress("70")
            sleep(1)
            reportProgress("80")
            sleep(1)
            reportProgress("90")
        })

        threads.forEach({t -> t.join()})
        reportProgress("100")
        List<TaskProgressVO> progresses = Q.New(TaskProgressVO.class)
                .orderBy(TaskProgressVO_.id, SimpleQuery.Od.ASC) // DO NOT SORT BY TIME! time value may repeat
                .list()
        // TODO: solve the boundary condition
        assert progresses.content.unique() == ["0", "10", "20", "30", "40", "50", "60", "70", "100"]
    }
}

