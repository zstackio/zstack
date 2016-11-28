package org.zstack.test.multinodes;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.job.Job;
import org.zstack.core.job.RestartableJob;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
@RestartableJob
public class RestartableSilentJob extends SilentJob implements Job {
}
