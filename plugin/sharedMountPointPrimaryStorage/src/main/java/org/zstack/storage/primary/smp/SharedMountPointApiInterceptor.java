package org.zstack.storage.primary.smp;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created by camile on 2017/4/18.
 */
public class SharedMountPointApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSharedMountPointPrimaryStorageMsg) {
            validate((APIAddSharedMountPointPrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddSharedMountPointPrimaryStorageMsg msg) {
        String url = msg.getUrl();
        if (url.startsWith("/dev") || url.startsWith("/proc") || url.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_PRIMARY_SMP_10013, " the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }
}
