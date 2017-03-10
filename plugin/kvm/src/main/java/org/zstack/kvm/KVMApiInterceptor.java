package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import static org.zstack.core.Platform.argerr;

/**
 */
public class KVMApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddKVMHostMsg) {
            validate((APIAddKVMHostMsg) msg);
        }

        return msg;
    }


    private void validate(APIAddKVMHostMsg msg) {
        SimpleQuery<KVMHostVO> q = dbf.createQuery(KVMHostVO.class);
        q.add(KVMHostVO_.managementIp, Op.EQ, msg.getManagementIp());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("there has been a kvm host having management ip[%s]", msg.getManagementIp()));
        }
    }


}
