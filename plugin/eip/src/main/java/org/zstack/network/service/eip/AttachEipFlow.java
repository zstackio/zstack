package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachEipFlow implements Flow {
    @Autowired
    private EipManager eipMgr;

    private String SUCCESS = AttachEipFlow.class.getName();

    public void run(final FlowTrigger trigger, final Map data) {
        String providerType = (String) data.get(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString());

        EipBackend bkd = eipMgr.getEipBackend(providerType);
        final EipStruct struct = (EipStruct) data.get(EipConstant.Params.EIP_STRUCT.toString());
        bkd.applyEip(struct, new Completion(trigger) {
            @Override
            public void success() {
                data.put(SUCCESS, true);
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        if (data.get(SUCCESS) == null) {
            trigger.rollback();
            return;
        }

        String providerType = (String) data.get(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString());
        EipBackend bkd = eipMgr.getEipBackend(providerType);
        EipStruct struct = (EipStruct) data.get(EipConstant.Params.EIP_STRUCT.toString());
        bkd.revokeEip(struct, new NopeCompletion(trigger));
        trigger.rollback();
    }
}
