package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DetachEipFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(DetachEipFlow.class);
    @Autowired
    private EipManager eipMgr;

    public void run(final FlowTrigger trigger, final Map data) {
        final String providerType = (String) data.get(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString());

        EipBackend bkd = eipMgr.getEipBackend(providerType);
        final EipStruct struct = (EipStruct) data.get(EipConstant.Params.EIP_STRUCT.toString());
        bkd.revokeEip(struct, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO
                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                trigger.next();
            }
        });
    }
}
