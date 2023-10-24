package org.zstack.sugonSdnController.header;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.sdnController.header.APIRemoveSdnControllerEvent;
import org.zstack.sdnController.header.APIRemoveSdnControllerMsg;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;


public class SugonApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            validate((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIDeleteL3NetworkMsg) {
            validate((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveSdnControllerMsg) {
            validate((APIRemoveSdnControllerMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteL2NetworkMsg msg) {
        APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
        if(Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .eq(L3NetworkVO_.type, SugonSdnControllerConstant.L3_TF_NETWORK_TYPE).count() > 0){
            String error = String.format("L2Network[%s] still has some L3Networks, please delete L3Networks first.",
                    msg.getL2NetworkUuid());
            evt.setError(operr(error));
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIDeleteL3NetworkMsg msg) {
        APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());
        if(Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .eq(VmNicVO_.type, VmInstanceConstant.TF_VIRTUAL_NIC_TYPE).count() > 0){
            String error = String.format("L3Network[%s] still has some Nics, please delete all Nics first.",
                    msg.getId());
            evt.setError(operr(error));
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIRemoveSdnControllerMsg msg) {
        APIRemoveSdnControllerEvent evt = new APIRemoveSdnControllerEvent(msg.getId());
        if(Q.New(L2NetworkVO.class).eq(L2NetworkVO_.type, SugonSdnControllerConstant.L2_TF_NETWORK_TYPE).count() > 0){
            String error = String.format("There are some TfL2Networks exists, please delete all TfL2Networks first.",
                    msg.getId());
            evt.setError(operr(error));
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIDeleteL2NetworkMsg.class);
        ret.add(APIDeleteL3NetworkMsg.class);
        ret.add(APIRemoveSdnControllerMsg.class);
        return ret;
    }

    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
