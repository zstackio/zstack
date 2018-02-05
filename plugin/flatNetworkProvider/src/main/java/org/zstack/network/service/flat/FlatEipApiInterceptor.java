package org.zstack.network.service.flat;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.APIAttachEipMsg;
import org.zstack.network.service.eip.APICreateEipMsg;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by MaJin on 2017/12/21.
 */
public class FlatEipApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private NetworkServiceManager nwServiceMgr;

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICreateEipMsg.class, APIAttachEipMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        }

        return msg;
    }

    @Transactional(readOnly = true)
    protected void validate(APICreateEipMsg msg) {
        if (msg.getVmNicUuid() == null) {
            return;
        }

        String privateL3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, msg.getVmNicUuid()).findValue();
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(privateL3Uuid, EipConstant.EIP_TYPE);
        if (!providerType.toString().equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            return;
        }

        String pubL3Uuid = Q.New(VipVO.class).select(VipVO_.l3NetworkUuid).eq(VipVO_.uuid, msg.getVipUuid()).findValue();
        checkVipPublicL3Network(msg.getVmNicUuid(), pubL3Uuid);
    }

    @Transactional(readOnly = true)
    protected void validate(APIAttachEipMsg msg) {
        String privateL3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, msg.getVmNicUuid()).findValue();
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(privateL3Uuid, EipConstant.EIP_TYPE);
        if (!providerType.toString().equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            return;
        }

        String pubL3Uuid = SQL.New("select vip.l3NetworkUuid from EipVO eip, VipVO vip" +
                " where eip.uuid = :eipUuid" +
                " and eip.vipUuid = vip.uuid", String.class)
                .param("eipUuid", msg.getEipUuid())
                .find();
        checkVipPublicL3Network(msg.getVmNicUuid(), pubL3Uuid);
    }

    @Transactional(readOnly = true)
    private void checkVipPublicL3Network(String vmNicUuid, String pubL3Uuid){
        boolean isPublicL2NetworkAttachedVmCluster = (Long) SQL.New("select count(l3)" +
                " from VmInstanceVO vm, VmNicVO nic, L2NetworkClusterRefVO ref, L3NetworkVO l3" +
                " where nic.uuid = :nicUuid" +
                " and vm.uuid = nic.vmInstanceUuid" +
                " and ref.clusterUuid = vm.clusterUuid" +
                " and ref.l2NetworkUuid = l3.l2NetworkUuid" +
                " and l3.uuid = :publicL3Uuid", Long.class)
                .param("nicUuid", vmNicUuid)
                .param("publicL3Uuid", pubL3Uuid)
                .find() > 0;
        if (!isPublicL2NetworkAttachedVmCluster){
            throw new ApiMessageInterceptionException(argerr("L2Network where vip's L3Network based hasn't attached" +
                    " the cluster where vmNic[uuid:%s] located", vmNicUuid));
        }
    }
}
