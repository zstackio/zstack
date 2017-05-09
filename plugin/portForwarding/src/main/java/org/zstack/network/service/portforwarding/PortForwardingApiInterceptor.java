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
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

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
            throw new ApiMessageInterceptionException(operr("Port forwarding rule[uuid:%s] is not in state of Enabled, current state is %s", msg.getRuleUuid(), state));
        }

        String vmNicUuid = t.get(1, String.class);
        if (vmNicUuid != null) {
            return ;
        }
    }

    private void validate(APIDetachPortForwardingRuleMsg msg) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.select(PortForwardingRuleVO_.vmNicUuid);
        q.add(PortForwardingRuleVO_.uuid, Op.EQ, msg.getUuid());
        String vmNicUuid = q.findValue();
        if (vmNicUuid == null) {
            throw new ApiMessageInterceptionException(operr("port forwarding rule rule[uuid:%s] has not been attached to any vm nic, can't detach", msg.getUuid()));
        }
    }

    private void validate(final APIAttachPortForwardingRuleMsg msg) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.select(PortForwardingRuleVO_.vmNicUuid, PortForwardingRuleVO_.state);
        q.add(PortForwardingRuleVO_.uuid, Op.EQ, msg.getRuleUuid());
        Tuple t = q.findTuple();

        String vmNicUuid = t.get(0, String.class);
        if (vmNicUuid != null) {
            throw new ApiMessageInterceptionException(operr("port forwarding rule[uuid:%s] has been attached to vm nic[uuid:%s], can't attach again", msg.getRuleUuid(), vmNicUuid));
        }

        PortForwardingRuleState state = t.get(1, PortForwardingRuleState.class);
        if (state != PortForwardingRuleState.Enabled) {
            throw new ApiMessageInterceptionException(operr("port forwarding rule[uuid:%s] is not in state of Enabled,  current state is %s. A rule can only be attached when its state is Enabled", msg.getRuleUuid(), state));
        }

        VipVO vip = new Callable<VipVO>() {
            @Override
            @Transactional(readOnly = true)
            public VipVO call() {
                String sql = "select vip from VipVO vip, PortForwardingRuleVO pf where vip.uuid = pf.vipUuid and pf.uuid = :pfUuid";
                TypedQuery<VipVO> q = dbf.getEntityManager().createQuery(sql, VipVO.class);
                q.setParameter("pfUuid", msg.getRuleUuid());
                return q.getSingleResult();
            }
        }.call();

        SimpleQuery<VmNicVO> vq = dbf.createQuery(VmNicVO.class);
        vq.select(VmNicVO_.l3NetworkUuid);
        vq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
        String guestL3Uuid = vq.findValue();
        if (guestL3Uuid.equals(vip.getL3NetworkUuid())) {
            throw new ApiMessageInterceptionException(argerr("guest l3Network of vm nic[uuid:%s] and vip l3Network of port forwarding rule[uuid:%s] are the same network",
                            msg.getVmNicUuid(), msg.getRuleUuid()));
        }

        if (vip.getPeerL3NetworkUuid() != null && !vip.getPeerL3NetworkUuid().equals(guestL3Uuid)) {
            throw new ApiMessageInterceptionException(argerr("the VIP[uuid:%s] is already bound the a guest L3 network[uuid:%s], but the VM nic[uuid:%s]" +
                            " is on another guest L3 network[uuid:%s]", vip.getUuid(), vip.getPeerL3NetworkUuid(),
                            msg.getVmNicUuid(), guestL3Uuid));
        }

        checkIfAnotherVip(vip.getUuid(), msg.getVmNicUuid());
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
                throw new ApiMessageInterceptionException(argerr("for range port forwarding, the port range size must match; vip range[%s, %s]'s size doesn't match range[%s, %s]'s size",
                                msg.getVipPortStart(), msg.getVipPortEnd(), msg.getPrivatePortStart(), msg.getPrivatePortEnd()));
            }
        }

        if (msg.getAllowedCidr() != null) {
            if (!NetworkUtils.isCidr(msg.getAllowedCidr())) {
                throw new ApiMessageInterceptionException(argerr("invalid CIDR[%s]", msg.getAllowedCidr()));
            }
        }

        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.add(PortForwardingRuleVO_.vipUuid, Op.EQ, msg.getVipUuid());
        List<PortForwardingRuleVO> vos = q.list();
        for (PortForwardingRuleVO vo : vos) {
            if (vo.getProtocolType().toString().equals(msg.getProtocolType())) {
                if (rangeOverlap(vipStart, vipEnd, vo.getVipPortStart(), vo.getVipPortEnd())) {
                    throw new ApiMessageInterceptionException(argerr("vip port range[vipStartPort:%s, vipEndPort:%s] overlaps with rule[uuid:%s, vipStartPort:%s, vipEndPort:%s]",
                                    vipStart, vipEnd, vo.getUuid(), vo.getVipPortStart(), vo.getVipPortEnd()));
                }
            }
        }

        if (msg.getVmNicUuid() != null) {
            SimpleQuery<VipVO> vq = dbf.createQuery(VipVO.class);
            vq.select(VipVO_.l3NetworkUuid, VipVO_.peerL3NetworkUuid);
            vq.add(VipVO_.uuid, Op.EQ, msg.getVipUuid());
            Tuple t = vq.findTuple();
            String vipL3Uuid = t.get(0, String.class);
            String peerL3Uuid = t.get(1, String.class);

            SimpleQuery<VmNicVO> nicq = dbf.createQuery(VmNicVO.class);
            nicq.select(VmNicVO_.l3NetworkUuid);
            nicq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
            String nicL3Uuid = nicq.findValue();
            if (nicL3Uuid.equals(vipL3Uuid)) {
                throw new ApiMessageInterceptionException(argerr("guest l3Network of vm nic[uuid:%s] and vip l3Network of vip[uuid: %s] are the same network", msg.getVmNicUuid(), msg.getVipUuid()));
            }

            if (peerL3Uuid != null && !peerL3Uuid.equals(nicL3Uuid)) {
                throw new ApiMessageInterceptionException(argerr("the VIP[uuid:%s] is already bound the a guest L3 network[uuid:%s], but the VM nic[uuid:%s]" +
                                " is on another guest L3 network[uuid:%s]", msg.getVipUuid(), peerL3Uuid, msg.getVmNicUuid(), nicL3Uuid));
            }

            checkIfAnotherVip(msg.getVipUuid(), msg.getVmNicUuid());
        }
    }

    @Transactional(readOnly = true)
    private void checkIfAnotherVip(String vipUuid, String vmNicUuid) {
        String sql = "select nic.uuid from VmNicVO nic where nic.vmInstanceUuid = (select n.vmInstanceUuid from VmNicVO n where" +
                " n.uuid = :nicUuid)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuid", vmNicUuid);
        List<String> nicUuids = q.getResultList();

        sql = "select count(*) from VmNicVO nic, PortForwardingRuleVO pf where nic.uuid = pf.vmNicUuid and pf.vipUuid != :vipUuid and nic.uuid in (:nicUuids)";
        TypedQuery<Long> lq = dbf.getEntityManager().createQuery(sql, Long.class);
        lq.setParameter("vipUuid", vipUuid);
        lq.setParameter("nicUuids", nicUuids);
        long count = lq.getSingleResult();

        if (count > 0) {
            sql = "select vm from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :nicUuid";
            TypedQuery<VmInstanceVO> vq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vq.setParameter("nicUuid", vmNicUuid);
            VmInstanceVO vm = vq.getSingleResult();

            throw new ApiMessageInterceptionException(operr("the VM[name:%s uuid:%s] already has port forwarding rules that have different VIPs than the one[uuid:%s]",
                            vm.getName(), vm.getUuid(), vipUuid));
        }
    }

    private void validate(APIDeletePortForwardingRuleMsg msg) {
        if (!dbf.isExist(msg.getUuid(), PortForwardingRuleVO.class)) {
            bus.publish(new APIDeletePortForwardingRuleEvent(msg.getId()));
            throw new StopRoutingException();
        }
    }
}
