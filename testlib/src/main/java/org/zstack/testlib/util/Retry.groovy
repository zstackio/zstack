package org.zstack.testlib.util

import org.zstack.testlib.StopTestSuiteException
import org.zstack.utils.Utils

import java.util.concurrent.TimeUnit

trait Retry {
    boolean retryInSecs(int total = 8, int interval = 1, Closure c) {
        int count = 0
        def ret = false

        while (count < total) {
            try {
                def r = c()
                ret = r == null || (r != null && r instanceof Boolean && r)
            } catch (StopTestSuiteException e) {
                throw e
            } catch (Throwable t) {
                Utils.getLogger(Retry.class).debug("[retryInSecs:${count + 1}/${total}]", t)
                if (total - count == 1) {
                    throw t
                }
            }

            if (ret) {
                return ret
            }
            TimeUnit.SECONDS.sleep(interval)
            count ++
        }

        return false
    }
}