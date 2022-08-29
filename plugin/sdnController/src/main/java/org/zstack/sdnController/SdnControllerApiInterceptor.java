package org.zstack.sdnController;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.sdnController.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by shixin.ruan on 09/17/2019
 */
public class SdnControllerApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(SdnControllerApiInterceptor.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof SdnControllerMessage) {
            SdnControllerMessage smsg = (SdnControllerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SdnControllerConstant.SERVICE_ID, smsg.getSdnControllerUuid());
        }
    }

    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APICreateL2VxlanNetworkMsg.class);
        ret.add(APIAttachL2NetworkToClusterMsg.class);
        ret.add(APIAddSdnControllerMsg.class);
        ret.add(APIRemoveSdnControllerMsg.class);

        return ret;
    }

    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateL2VxlanNetworkMsg) {
            handle((APICreateL2VxlanNetworkMsg)msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg){
            handle((APIAttachL2NetworkToClusterMsg)msg);
        } else if (msg instanceof APIAddSdnControllerMsg) {
            handle((APIAddSdnControllerMsg)msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void handle(APICreateL2VxlanNetworkMsg msg) {
    }

    private void handle(APIAttachL2NetworkToClusterMsg msg) {
    }

    private void handle(APIAddSdnControllerMsg msg) {
        if (!SdnControllerType.getAllTypeNames().contains(msg.getVendorType())) {
            throw new ApiMessageInterceptionException(argerr("Sdn controller type: %s in not in the supported list: %s ", msg.getVendorType(), SdnControllerType.getAllTypeNames()));
        }
    }
}
