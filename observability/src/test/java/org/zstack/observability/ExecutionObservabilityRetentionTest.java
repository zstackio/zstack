package org.zstack.observability;

import org.junit.Assert;
import org.junit.Test;
import org.apache.logging.log4j.ThreadContext;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.zone.APICreateZoneMsg;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class ExecutionObservabilityRetentionTest {
    @Test
    public void recordsAreRetainedByTriggerType() throws Exception {
        Assert.assertEquals(5000, limit("MAX_API_RECORDS"));
        Assert.assertEquals(3000, limit("MAX_SCHEDULED_TASK_RECORDS"));
        Assert.assertEquals(3000, limit("MAX_MESSAGE_RECORDS"));
        Assert.assertEquals(11000, limit("MAX_RECORDS"));

        boolean originalEnabled = ExecutionObservabilityGlobalProperty.ENABLED;
        try {
            ExecutionObservabilityGlobalProperty.ENABLED = true;
            assertApiRecordsAreTrimmed();
            assertScheduledTaskRecordsAreTrimmed();
            assertMessageRecordsAreTrimmed();
        } finally {
            ExecutionObservabilityGlobalProperty.ENABLED = originalEnabled;
            ThreadContext.clearAll();
        }
    }

    private void assertApiRecordsAreTrimmed() throws Exception {
        ExecutionObservabilityFacadeImpl recorder = new ExecutionObservabilityFacadeImpl();
        APICreateZoneMsg oldest = new APICreateZoneMsg();
        APICreateZoneMsg middle = new APICreateZoneMsg();
        APICreateZoneMsg newest = new APICreateZoneMsg();
        recorder.recordApiRequest(oldest);
        recorder.recordApiRequest(middle);
        recorder.recordApiRequest(newest);

        trim(recorder, "API", 2);
        assertRetained(recorder, oldest.getId(), middle.getId(), newest.getId());
    }

    private void assertScheduledTaskRecordsAreTrimmed() throws Exception {
        ExecutionObservabilityFacadeImpl recorder = new ExecutionObservabilityFacadeImpl();
        String oldest = recorder.recordScheduledTaskStarted((Runnable) () -> { });
        ThreadContext.clearAll();
        String middle = recorder.recordScheduledTaskStarted((Runnable) () -> { });
        ThreadContext.clearAll();
        String newest = recorder.recordScheduledTaskStarted((Runnable) () -> { });
        ThreadContext.clearAll();

        trim(recorder, "SCHEDULED_TASK", 2);
        assertRetained(recorder, oldest, middle, newest);
    }

    private void assertMessageRecordsAreTrimmed() throws Exception {
        ExecutionObservabilityFacadeImpl recorder = new ExecutionObservabilityFacadeImpl();
        NeedReplyMessage oldest = new NeedReplyMessage() { };
        NeedReplyMessage middle = new NeedReplyMessage() { };
        NeedReplyMessage newest = new NeedReplyMessage() { };
        recorder.recordMessageDelivery(oldest);
        recorder.recordMessageDelivery(middle);
        recorder.recordMessageDelivery(newest);

        trim(recorder, "MESSAGE", 2);
        assertRetained(recorder, oldest.getId(), middle.getId(), newest.getId());
    }

    private void trim(ExecutionObservabilityFacadeImpl recorder, String triggerType, int maxRecords)
            throws Exception {
        Method trim = ExecutionObservabilityFacadeImpl.class
                .getDeclaredMethod("trimExecutionType", String.class, int.class);
        trim.setAccessible(true);
        trim.invoke(recorder, triggerType, maxRecords);
    }

    private void assertRetained(ExecutionObservabilityFacadeImpl recorder,
                                String oldest, String middle, String newest) throws Exception {
        Field executionsField = ExecutionObservabilityFacadeImpl.class.getDeclaredField("executions");
        executionsField.setAccessible(true);
        Map<?, ?> executions = (Map<?, ?>) executionsField.get(recorder);
        Assert.assertEquals(2, executions.size());
        Assert.assertFalse(executions.containsKey(oldest));
        Assert.assertTrue(executions.containsKey(middle));
        Assert.assertTrue(executions.containsKey(newest));
    }

    private int limit(String fieldName) throws Exception {
        Field field = ExecutionObservabilityFacadeImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }
}
