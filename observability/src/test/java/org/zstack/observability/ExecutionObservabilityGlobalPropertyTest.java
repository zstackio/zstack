package org.zstack.observability;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.GlobalProperty;
import org.zstack.header.core.execution.APIQueryExecutionMsg;
import org.zstack.header.zone.APICreateZoneMsg;

import java.lang.reflect.Field;

public class ExecutionObservabilityGlobalPropertyTest {
    @Test
    public void globalPropertyIsDisabledByDefault() throws NoSuchFieldException {
        Field enabled = ExecutionObservabilityGlobalProperty.class.getField("ENABLED");
        GlobalProperty property = enabled.getAnnotation(GlobalProperty.class);

        Assert.assertNotNull(property);
        Assert.assertEquals("ExecutionObservability.enabled", property.name());
        Assert.assertEquals("false", property.defaultValue());
    }

    @Test
    public void disabledObservationDoesNotCreateExecutions() {
        boolean original = ExecutionObservabilityGlobalProperty.ENABLED;
        ExecutionObservabilityFacadeImpl recorder = new ExecutionObservabilityFacadeImpl();

        try {
            ExecutionObservabilityGlobalProperty.ENABLED = false;
            recorder.recordApiRequest(new APICreateZoneMsg());

            Assert.assertNull(recorder.recordScheduledTaskStarted((Runnable) () -> { }));
            Assert.assertTrue(recorder.queryLocal(new APIQueryExecutionMsg()).isEmpty());
        } finally {
            ExecutionObservabilityGlobalProperty.ENABLED = original;
            ThreadContext.clearAll();
        }
    }

    @Test
    public void scheduledExecutionCompletesWhenDisabledAfterStart() {
        boolean original = ExecutionObservabilityGlobalProperty.ENABLED;
        ExecutionObservabilityFacadeImpl recorder = new ExecutionObservabilityFacadeImpl();

        try {
            ExecutionObservabilityGlobalProperty.ENABLED = true;
            String executionUuid = recorder.recordScheduledTaskStarted((Runnable) () -> { });
            Assert.assertNotNull(executionUuid);

            ThreadContext.clearAll();
            ExecutionObservabilityGlobalProperty.ENABLED = false;
            recorder.recordScheduledTaskCompleted(executionUuid, null, null);

            ExecutionObservabilityGlobalProperty.ENABLED = true;
            APIQueryExecutionMsg query = new APIQueryExecutionMsg();
            query.setExecutionUuid(executionUuid);
            Assert.assertEquals(1, recorder.queryLocal(query).size());
            Assert.assertEquals("SUCCEEDED", recorder.queryLocal(query).get(0).getState());
        } finally {
            ExecutionObservabilityGlobalProperty.ENABLED = original;
            ThreadContext.clearAll();
        }
    }

}
