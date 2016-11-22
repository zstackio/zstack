package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

/**
 */
public class PortForwardingApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeletePortForwardingRuleMsg) {
            validate((APIDeletePortForwardingRuleMsg) msg);
        } else if (msg instanceof APICreatePortForwardingRuleMsg) {
            validate((APICreatePortForwardingRuleMsg)msg);
        } else if (msg instanceof APIAttachPortForwardingRuleMsg) {
            validate((APIAttachPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIDetachPortForwardingRuleMsg) {
            validate((APIDetachPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIGetPortForwardingAttachableVmNicsMsg) {
            validate((APIGetPortForwardingAttachableVmNicsMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetPortForwardingAttachableVmNicsMsg msg) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.select(PortForwardingRuleVO_.state, PortForwardingRuleVO_.vmNicUuid);
        q.add(PortForwardingRuleVO_.uuid, Op.EQ, msg.getRuleUuid());
        Tuple t = q.findTuple();
        PortForwardingRuleState state = t.get(0, PortForwardingRuleState.class);

        if (state != PortForwardingRuleState.Enabled) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("Port forwarding rule[uuid:%s] is not in state of Enabled, current state is %s", msg.getRuleUuid(), state)
            ));
        }

        String vmNicUuid = t.get(1, String.class);
        if (vmNicUuid != null) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("Port forwarding rule[uuid:%s] has been attached to vm nic[uuid:%s] already", msg.getRuleUuid(), vmNicUuid)
            ));
        }
    }

    private void validate(APIDetachPortForwardingRuleMsg msg) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.select(PortForwardingRuleVO_.vmNicUuid);
        q.add(PortForwardingRuleVO_.uuid, Op.EQ, msg.getUuid());
        String vmNicUuid = q.findValue();
        if (vmNicUuid == null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("port forwarding rule rule[uuid:%s] has not been attached to any vm nic, can't detach", msg.getUuid())
            ));
        }
    }

    private void validate(final APIAttachPortForwardingRuleMsg msg) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.select(PortForwardingRuleVO_.vmNicUuid, PortForwardingRuleVO_.state);
        q.add(PortForwardingRuleVO_.uuid, Op.EQ, msg.getRuleUuid());
        Tuple t = q.findTuple();

        String vmNicUuid = t.get(0, String.class);
        if (vmNicUuid != null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("port forwarding rule[uuid:%s] has been attached to vm nic[uuid:%s], can't attach again", msg.getRuleUuid(), vmNicUuid)
            ));
        }

        PortForwardingRuleState state = t.get(1, PortForwardingRuleState.class);
        if (state != PortForwardingRuleState.Enabled) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("port forwarding rule[uuid:%s] is not in state of Enabled,  current state is %s. A rule can only be attached when its state is Enabled", msg.getRuleUuid(), state)
            ));
        }

        String vipL3Uuid = new Callable<String>() {
            @Override
            @Transactional(readOnly = true)
            public String call() {
                String sql = "select vip.uuid from VipVO vip, PortForwardingRuleVO pf where vip.uuid = pf.vipUuid and pf.uuid = :pfUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("pfUuid", msg.getRuleUuid());
                return q.getSingleResult();
            }
        }.call();

        SimpleQuery<VmNicVO> vq = dbf.createQuery(VmNicVO.class);
        vq.select(VmNicVO_.l3NetworkUuid);
        vq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
        String guestL3Uuid = vq.findValue();
        if (guestL3Uuid.equals(vipL3Uuid)) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("guest l3Network of vm nic[uuid:%s] and vip l3Network of port forwarding rule[uuid:%s] are the same network",
                            msg.getVmNicUuid(), msg.getRuleUuid())
            ));
        }
    }

    private boolean rangeOverlap(int s1, int e1, int s2, int e2) {
        return (s1 >= s2 && s1 <= e2) || (s1 <= s2 && s2 <= e1);
    }

    private void validate(APICreatePortForwardingRuleMsg msg) {
        if (msg.getVipPortEnd() == null) {
            msg.setVipPortEnd(msg.getVipPortStart());
        }
        if (msg.getPrivatePortStart() == null) {
            msg.setPrivatePortStart(msg.getVipPortStart());
        }
        if (msg.getPrivatePortEnd() == null) {
            msg.setPrivatePortEnd(msg.getVipPortEnd());
        }

        int vipStart = Math.min(msg.getVipPortStart(), msg.getVipPortEnd());
        int vipEnd = Math.max(msg.getVipPortStart(), msg.getVipPortEnd());
        msg.setVipPortStart(vipStart);
        msg.setVipPortEnd(vipEnd);

        int privateStart = Math.min(msg.getPrivatePortStart(), msg.getPrivatePortEnd());
        int privateEnd = Math.max(msg.getPrivatePortStart(), msg.getPrivatePortEnd());
        msg.setPrivatePortStart(privateStart);
        msg.setPrivatePortEnd(privateEnd);

        if (!msg.getVipPortStart().equals(msg.getVipPortEnd())) {
            // it's a port range
            if (msg.getVipPortEnd() - msg.getVipPortStart() != msg.getPrivatePortEnd() - msg.getPrivatePortStart()) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("for range port forwarding, the port range size must match; vip range[%s, %s]'s size doesn't match range[%s, %s]'s size",
                                msg.getVipPortStart(), msg.getVipPortEnd(), msg.getPrivatePortStart(), msg.getPrivatePortEnd())
                ));
            }
        }

        if (msg.getAllowedCidr() != null) {
            if (!NetworkUtils.isCidr(msg.getAllowedCidr())) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("invalid CIDR[%s]", msg.getAllowedCidr())
                ));
            }
        }

        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.add(PortForwardingRuleVO_.vipUuid, Op.EQ, msg.getVipUuid());
        List<PortForwardingRuleVO> vos = q.list();
        for (PortForwardingRuleVO vo : vos) {
            if (vo.getProtocolType().toString().equals(msg.getProtocolType())) {
                if (rangeOverlap(vipStart, vipEnd, vo.getVipPortStart(), vo.getVipPortEnd())) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("vip port range[vipStartPort:%s, vipEndPort:%s] overlaps with rule[uuid:%s, vipStartPort:%s, vipEndPort:%s]",
                                    vipStart, vipEnd, vo.getUuid(), vo.getVipPortStart(), vo.getVipPortEnd())
                    ));
                }
            }
        }

        if (msg.getVmNicUuid() != null) {
            SimpleQuery<VipVO> vq = dbf.createQuery(VipVO.class);
            vq.select(VipVO_.l3NetworkUuid);
            vq.add(VipVO_.uuid, Op.EQ, msg.getVipUuid());
            String vipL3Uuid = vq.findValue();

            SimpleQuery<VmNicVO> nicq = dbf.createQuery(VmNicVO.class);
            nicq.select(VmNicVO_.l3NetworkUuid);
            nicq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
            String nicL3Uuid = nicq.findValue();
            if (nicL3Uuid.equals(vipL3Uuid)) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("guest l3Network of vm nic[uuid:%s] and vip l3Network of vip[uuid: %s] are the same network", msg.getVmNicUuid(), msg.getVipUuid())
                ));
            }
        }
    }

    private void validate(APIDeletePortForwardingRuleMsg msg) {
        if (!dbf.isExist(msg.getUuid(), PortForwardingRuleVO.class)) {
            bus.publish(new APIDeletePortForwardingRuleEvent(msg.getId()));
            throw new StopRoutingException();
        }
    }
}
