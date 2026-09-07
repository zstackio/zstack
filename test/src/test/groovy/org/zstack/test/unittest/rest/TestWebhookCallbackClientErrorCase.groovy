package org.zstack.test.unittest.rest

import org.junit.Test
import org.mockito.Mockito
import org.zstack.core.rest.webhook.WebhookCallbackClient
import org.zstack.core.rest.webhook.WebhookProtocol
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.rest.RESTFacade

import java.util.concurrent.TimeUnit

class TestWebhookCallbackClientErrorCase {
    private WebhookCallbackClient<Map> client() {
        def protocol = [getCallbackPath: { '/test/callback' }, getCallbackClass: { Map },
                        extractTaskId: { Map cmd -> cmd.taskId }, isSuccess: { Map cmd -> cmd.success },
                        extractError: { Map cmd -> cmd.error }] as WebhookProtocol<Map>
        new WebhookCallbackClient<Map>(protocol, Mockito.mock(RESTFacade), Mockito.mock(ThreadFacade))
    }

    @Test
    void failedCallbackKeepsReasonAndCompletesOnlyOnce() {
        def callbackClient = client()
        List<ErrorCode> errors = []
        callbackClient.submit('task-id', new ReturnValueCompletion<Map>(null) {
            @Override
            void success(Map data) { assert false }
            @Override
            void fail(ErrorCode error) { errors.add(error) }
        }, TimeUnit.SECONDS, 30)
        Map response = [taskId: 'task-id', success: false, error: 'host placement rejected']
        callbackClient.deliverCallback(response)
        callbackClient.deliverCallback(response)
        assert errors.size() == 1
        assert errors[0].globalErrorCode == 'ORG_ZSTACK_CORE_WEBHOOK_10001'
        assert errors[0].details == 'webhook callback failed for taskId[task-id], path[/test/callback], error: host placement rejected'
    }

    @Test
    void successfulCallbackStillReturnsOriginalDataOnlyOnce() {
        def callbackClient = client()
        List<Map> results = []
        callbackClient.submit('successful-task', new ReturnValueCompletion<Map>(null) {
            @Override
            void success(Map data) { results.add(data) }
            @Override
            void fail(ErrorCode error) { assert false }
        }, TimeUnit.SECONDS, 30)
        Map response = [taskId: 'successful-task', success: true, data: [uuid: 'port']]
        callbackClient.deliverCallback(response)
        callbackClient.deliverCallback(response)
        assert results == [response]
    }
}
