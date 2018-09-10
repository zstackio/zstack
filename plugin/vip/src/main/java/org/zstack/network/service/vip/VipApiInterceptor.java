package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.IpAllocatorType;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 */
public class VipApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVipMsg) {
            validate((APICreateVipMsg) msg);
        } else if (msg instanceof APIDeleteVipMsg) {
            validate((APIDeleteVipMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteVipMsg msg) {

    }

    private void validate(APICreateVipMsg msg) {
        if (msg.getAllocatorStrategy() != null && !IpAllocatorType.hasType(msg.getAllocatorStrategy())) {
            throw new ApiMessageInterceptionException(argerr("unsupported ip allocation strategy[%s]", msg.getAllocatorStrategy()));
        }

        if (msg.getRequiredIp() != null) {
            L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
            if (l3NetworkVO.getIpVersion() == IPv6Constants.IPv4) {
                if (!NetworkUtils.isIpv4Address(msg.getRequiredIp())) {
                    throw new ApiMessageInterceptionException(argerr("requiredIp[%s] is not in valid IPv4 mediaType", msg.getRequiredIp()));
                }
            } else {
                if (!IPv6NetworkUtils.isIpv6UnicastAddress(msg.getRequiredIp())) {
                    if (!NetworkUtils.isIpv4Address(msg.getRequiredIp())) {
                        throw new ApiMessageInterceptionException(argerr("requiredIp[%s] is not in valid IPv6 mediaType", msg.getRequiredIp()));
                    }
                }
            }

            SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
            q.add(VipVO_.ip, Op.EQ, msg.getRequiredIp());
            q.add(VipVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            if (q.isExists()) {
                throw new ApiMessageInterceptionException(operr("there is already a vip[%s] on l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid()));
            }
        }
    }
}
