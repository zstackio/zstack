package org.zstack.core.webhook;

import org.apache.commons.validator.routines.UrlValidator;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.core.webhooks.APICreateWebhookMsg;
import org.zstack.header.core.webhooks.APIUpdateWebhookMsg;
import org.zstack.header.message.APIMessage;
import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created by xing5 on 2017/5/7.
 */
public class WebhookApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateWebhookMsg) {
            validate((APICreateWebhookMsg) msg);
        } else if (msg instanceof APIUpdateWebhookMsg) {
            validate((APIUpdateWebhookMsg) msg);
        }
        
        return msg;
    }

    private void validateUrl(String url) {
        if (!new UrlValidator().isValid(url)) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_CORE_WEBHOOK_10000, "Invalid url[%s]", url));
        }
    }

    private void validate(APIUpdateWebhookMsg msg) {
        if (msg.getUrl() != null) {
            validateUrl(msg.getUrl());
        }
    }

    private void validate(APICreateWebhookMsg msg) {
        validateUrl(msg.getUrl());
    }
}
