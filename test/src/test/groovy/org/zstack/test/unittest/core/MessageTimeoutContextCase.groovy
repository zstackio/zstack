package org.zstack.test.unittest.core

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.zstack.core.timeout.ApiTimeoutManagerImpl
import org.zstack.header.message.Message
import org.zstack.header.message.NeedReplyMessage
import org.zstack.utils.TaskContext

class MessageTimeoutContextCase {
    ApiTimeoutManagerImpl timeoutManager = new ApiTimeoutManagerImpl()

    @Before
    void setUp() {
        TaskContext.removeTaskContext()
    }

    @After
    void tearDown() {
        TaskContext.removeTaskContext()
    }

    @Test
    void testPlainMessageDoesNotCreateTimeoutContext() {
        timeoutManager.beforeDeliveryMessage(new Message() {})

        def timeout = TaskContext.getTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_TIMEOUT)
        def deadline = TaskContext.getTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_DEADLINE)
        assert timeout == null : "Plain Message must not create timeout: expected=null actual=${timeout}"
        assert deadline == null : "Plain Message must not create deadline: expected=null actual=${deadline}"
    }

    @Test
    void testPlainMessagePreservesInheritedTimeoutContext() {
        String inheritedTimeout = "300000"
        String inheritedDeadline = "4102444800000"
        TaskContext.putTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_TIMEOUT, inheritedTimeout)
        TaskContext.putTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_DEADLINE, inheritedDeadline)

        timeoutManager.beforeDeliveryMessage(new Message() {})

        def timeout = TaskContext.getTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_TIMEOUT)
        def deadline = TaskContext.getTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_DEADLINE)
        assert timeout == inheritedTimeout :
                "Plain Message must preserve inherited timeout: expected=${inheritedTimeout} actual=${timeout}"
        assert deadline == inheritedDeadline :
                "Plain Message must preserve inherited deadline: expected=${inheritedDeadline} actual=${deadline}"
    }

    @Test
    void testNeedReplyMessageSetsOwnTimeoutContext() {
        NeedReplyMessage msg = new NeedReplyMessage() {}
        msg.setTimeout(60000L)
        msg.setMessageDeadline(4102444800000L)

        timeoutManager.beforeDeliveryMessage(msg)

        def timeout = TaskContext.getTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_TIMEOUT)
        def deadline = TaskContext.getTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_DEADLINE)
        assert timeout == "60000" : "NeedReplyMessage must set its own timeout: expected=60000 actual=${timeout}"
        assert deadline == "4102444800000" :
                "NeedReplyMessage must set its own deadline: expected=4102444800000 actual=${deadline}"
    }
}
