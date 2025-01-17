package org.zstack.core.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.core.external.plugin.APIDeletePluginDriversMsg;
import org.zstack.header.core.external.plugin.PluginDriversExtensionPoint;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;

class PluginApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeletePluginDriversMsg) {
            validate((APIDeletePluginDriversMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeletePluginDriversMsg msg) {
        for (PluginDriversExtensionPoint ext : pluginRgty.getExtensionList(PluginDriversExtensionPoint.class)) {
            ErrorCode result = ext.validateDeletePluginDrivers(msg.getUuid());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
    }
}
