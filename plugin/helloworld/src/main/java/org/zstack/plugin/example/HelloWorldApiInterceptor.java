package org.zstack.plugin.example;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import static org.zstack.core.Platform.argerr;

public class HelloWorldApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIHelloWorldMsg) {
            validate((APIHelloWorldMsg) msg);
        }

        return msg;
    }

    private void validate(APIHelloWorldMsg msg) {
        if (msg.getGreeting() == null || msg.getGreeting().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("greeting cannot be null or empty"));
        }
    }
}
