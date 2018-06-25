package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.APIDetachL3NetworkFromVmMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionDSL;

import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 */
public class KVMApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddKVMHostMsg) {
            validate((APIAddKVMHostMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            validate((APIDetachL3NetworkFromVmMsg) msg);
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

    private void validate(APIDetachL3NetworkFromVmMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (KVMConstant.KVM_HYPERVISOR_TYPE.equals(vo.getHypervisorType()) && !ImagePlatform.valueOf(vo.getPlatform()).isParaVirtualization()) {
            throw new ApiMessageInterceptionException(operr("unable to detach a L3 network on kvm host because platform of vm[uuid: %s] is %s",
                    msg.getVmInstanceUuid(), vo.getPlatform()));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(APIDetachL3NetworkFromVmMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
