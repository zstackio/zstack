package org.zstack.mediator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.APIAddIpRangeMsg;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.IpRangeVO_;
import org.zstack.header.vm.*;
import org.zstack.network.service.eip.APIAttachEipMsg;
import org.zstack.network.service.eip.APICreateEipMsg;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleMsg;
import org.zstack.network.service.portforwarding.APICreatePortForwardingRuleMsg;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 */
public class ApiValidator implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APICreateEipMsg.class);
        ret.add(APIAttachEipMsg.class);
        ret.add(APICreatePortForwardingRuleMsg.class);
        ret.add(APIAttachPortForwardingRuleMsg.class);
        ret.add(APIAttachL3NetworkToVmMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        } else if (msg instanceof APICreatePortForwardingRuleMsg) {
            validate((APICreatePortForwardingRuleMsg) msg);
        } else if (msg instanceof APIAttachPortForwardingRuleMsg) {
            validate((APIAttachPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        }

        return msg;
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (vmInstanceVO.getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)){
            validateIpRangeOverlapWithVm(msg.getL3NetworkUuid(), msg.getVmInstanceUuid());
        }
    }

    private void validateIpRangeOverlapWithVm(String l3NetworkUuid, String vmInstanceUuid) {
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid).list();
        List<IpRangeVO> newIpRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, l3NetworkUuid).list();
        DebugUtils.Assert(newIpRangeVOS != null && !newIpRangeVOS.isEmpty(), String.format("the l3 network[%s] to attach must has ip range", l3NetworkUuid));

        for (VmNicVO vmNicVO: vmNicVOS) {
            List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, vmNicVO.getL3NetworkUuid()).limit(1).list();
            if (ipRangeVOS != null && !ipRangeVOS.isEmpty()) {
                if (NetworkUtils.isCidrOverlap(ipRangeVOS.get(0).getNetworkCidr(), newIpRangeVOS.get(0).getNetworkCidr())) {
                    throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The cidr of l3[%s] to attach overlapped with l3[%s] already attached to vm", l3NetworkUuid, vmNicVO.getL3NetworkUuid()));
                }
            }
        }
    }

    private void validate(APIAttachPortForwardingRuleMsg msg) {
        isVmNicUsedByEip(msg.getVmNicUuid());
    }

    private void validate(APICreatePortForwardingRuleMsg msg) {
        if (msg.getVmNicUuid() != null) {
            isVmNicUsedByEip(msg.getVmNicUuid());
        }
    }

    @Transactional(readOnly = true)
    private void isVmNicUsedByPortForwarding(String vmNicUuid) {
        String sql = "select pf from PortForwardingRuleVO pf, VmInstanceVO vm, VmNicVO nic where pf.vmNicUuid = nic.uuid" +
                " and nic.vmInstanceUuid = vm.uuid and vm.uuid = (select n.vmInstanceUuid from VmNicVO n where n.uuid = :nicUuid)";
        TypedQuery<PortForwardingRuleVO> q = dbf.getEntityManager().createQuery(sql, PortForwardingRuleVO.class);
        q.setParameter("nicUuid", vmNicUuid);
        List<PortForwardingRuleVO> pfs = q.getResultList();

        if (!pfs.isEmpty()) {
            sql = "select vm from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :nicUuid";
            TypedQuery<VmInstanceVO> vq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vq.setParameter("nicUuid", vmNicUuid);
            VmInstanceVO vm = vq.getSingleResult();

            List<String> pfStr = pfs.stream().map(pf -> String.format("(name:%s, ip:%s)", pf.getName(), pf.getVipIp())).collect(Collectors.toList());

            throw new ApiMessageInterceptionException(operr("the vm[name:%s, uuid:%s] already has some port forwarding rules%s attached", vm.getName(), vm.getUuid(),
                            StringUtils.join(pfStr, ",")));
        }
    }

    @Transactional(readOnly = true)
    private void isVmNicUsedByEip(String vmNicUuid) {
        String sql = "select eip from EipVO eip, VmInstanceVO vm, VmNicVO nic where eip.vmNicUuid = nic.uuid" +
                " and nic.vmInstanceUuid = vm.uuid and vm.uuid = (select n.vmInstanceUuid from VmNicVO n where n.uuid = :nicUuid)";
        TypedQuery<EipVO> q = dbf.getEntityManager().createQuery(sql, EipVO.class);
        q.setParameter("nicUuid", vmNicUuid);
        List<EipVO> eips = q.getResultList();

        if (!eips.isEmpty()) {
            sql = "select vm from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :nicUuid";
            TypedQuery<VmInstanceVO> vq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vq.setParameter("nicUuid", vmNicUuid);
            VmInstanceVO vm = vq.getSingleResult();

            List<String> eipStr = eips.stream().map(eip -> String.format("(name:%s, ip:%s)", eip.getName(), eip.getVipIp())).collect(Collectors.toList());

            throw new ApiMessageInterceptionException(operr("the vm[name:%s, uuid:%s] already has some EIPs%s attached", vm.getName(), vm.getUuid(),
                            StringUtils.join(eipStr, ",")));
        }
    }

    private void validate(APIAttachEipMsg msg) {
        isVmNicUsedByPortForwarding(msg.getVmNicUuid());
    }

    private void validate(APICreateEipMsg msg) {
        if (msg.getVmNicUuid() != null) {
            isVmNicUsedByPortForwarding(msg.getVmNicUuid());
        }
    }
}
