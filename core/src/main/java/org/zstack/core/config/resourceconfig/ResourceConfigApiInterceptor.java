package org.zstack.core.config.resourceconfig;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.argerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceConfigApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof ResourceConfigMessage) {
            validate((ResourceConfigMessage) msg);
        }
        return msg;
    }

    private void validate(ResourceConfigMessage msg) {
        GlobalConfig gc = gcf.getAllConfig().get(msg.getIdentity());
        if (gc == null) {
            throw new ApiMessageInterceptionException(argerr("no global config[category:%s, name:%s] found",
                    msg.getCategory(), msg.getName()));
        }

        ResourceConfig rc = rcf.getResourceConfig(gc.getIdentity());
        if (rc == null) {
            throw new ApiMessageInterceptionException(argerr("global config[category:%s, name:%s] cannot bind resource",
                    msg.getCategory(), msg.getName()));
        }
    }
}
