package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;

import static org.zstack.core.Platform.*;

/**
 */
public class KVMHostAsyncHttpCallReply extends MessageReply {
    @NoJsonSchema
    private LinkedHashMap response;

    public LinkedHashMap getResponse() {
        return response;
    }

    public void setResponse(LinkedHashMap response) {
        this.response = response;
    }

    public <T> T toResponse(Class<T> clz) {
        return JSONObjectUtil.rehashObject(response, clz);
    }

    public static ErrorableValue<AgentResponse> unwrap(MessageReply reply) {
        return unwrap(reply, AgentResponse.class);
    }

    public static <T extends AgentResponse> ErrorableValue<T> unwrap(MessageReply reply, Class<T> responseClass) {
        if (!reply.isSuccess()) {
            return ErrorableValue.ofErrorCode(reply.getError());
        }

        if (!(reply instanceof KVMHostAsyncHttpCallReply)) {
            return ErrorableValue.ofErrorCode(
                    operr("reply[%s] is not a KVMHostAsyncHttpCallReply", reply.getClass().getSimpleName()));
        }

        final KVMHostAsyncHttpCallReply castReply = (KVMHostAsyncHttpCallReply) reply;
        if (castReply.response == null) {
            return ErrorableValue.ofErrorCode(
                    operr("reply[%s] return with empty response", reply.getClass().getSimpleName()));
        }

        final T response = castReply.toResponse(responseClass);
        if (!response.isSuccess()) {
            return ErrorableValue.ofErrorCode(
                    operr("%s operation failed: %s", response.getClass().getSimpleName(), response.getError()));
        }
        return ErrorableValue.of(response);
    }
}
