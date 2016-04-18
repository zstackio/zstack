package org.zstack.network.service.eip;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.vip.VipState;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.concurrent.Callable;

/**
 */
public class EipApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof EipMessage) {
            EipMessage emsg = (EipMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, EipConstant.SERVICE_ID, emsg.getEipUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIDeleteEipMsg) {
            validate((APIDeleteEipMsg) msg);
        } else if (msg instanceof APIDetachEipMsg) {
            validate((APIDetachEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        } else if (msg instanceof APIGetEipAttachableVmNicsMsg) {
            validate((APIGetEipAttachableVmNicsMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIGetEipAttachableVmNicsMsg msg) {
        if (msg.getVipUuid() == null && msg.getEipUuid() == null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("either eipUuid or vipUuid must be set")
            ));
        }

        if (msg.getEipUuid() != null) {
            SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
            q.select(EipVO_.state, EipVO_.vmNicUuid);
            q.add(EipVO_.uuid, Op.EQ, msg.getEipUuid());
            Tuple t = q.findTuple();

            EipState state = t.get(0, EipState.class);
            if (state != EipState.Enabled) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("eip[uuid:%s] is not in state of Enabled, cannot get attachable vm nic", msg.getEipUuid())
                ));
            }

            String vmNicUuid = t.get(1, String.class);
            if (vmNicUuid != null) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("eip[uuid:%s] has attached to vm nic[uuid:%s], cannot get attachable vm nic", msg.getEipUuid(), vmNicUuid)
                ));
            }
        }
    }

    private void validate(final APIAttachEipMsg msg) {
        isVmNicUsed(msg.getVmNicUuid());

        SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
        q.select(EipVO_.state, EipVO_.vmNicUuid, EipVO_.vipIp);
        q.add(EipVO_.uuid, Op.EQ, msg.getEipUuid());
        Tuple t = q.findTuple();
        String vmNicUuid = t.get(1, String.class);
        if (vmNicUuid != null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("eip[uuid:%s] has attached to another vm nic[uuid:%s], can't attach again", msg.getEipUuid(), vmNicUuid)
            ));
        }

        EipState state = t.get(0, EipState.class);
        if (state != EipState.Enabled) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("eip[uuid: %s] can only be attached when state is %s, current state is %s", msg.getEipUuid(), EipState.Enabled, state)
            ));
        }

        String vipIp = t.get(2, String.class);
        isVipInVmNicSubnet(vipIp, msg.getVmNicUuid());

        String vipL3Uuid = new Callable<String>() {
            @Override
            @Transactional(readOnly = true)
            public String call() {
                String sql = "select vip.uuid from VipVO vip, EipVO eip where vip.uuid = eip.vipUuid and eip.uuid = :eipUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("eipUuid", msg.getEipUuid());
                return q.getSingleResult();
            }
        }.call();

        SimpleQuery<VmNicVO> vq = dbf.createQuery(VmNicVO.class);
        vq.select(VmNicVO_.l3NetworkUuid);
        vq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
        String guestL3Uuid = vq.findValue();
        if (guestL3Uuid.equals(vipL3Uuid)) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("guest l3Network of vm nic[uuid:%s] and vip l3Network of EIP[uuid:%s] are the same network",
                            msg.getVmNicUuid(), msg.getEipUuid())
            ));
        }
    }

    private void validate(APIDetachEipMsg msg) {
        SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
        q.select(EipVO_.vmNicUuid);
        q.add(EipVO_.uuid, Op.EQ, msg.getUuid());
        String vmNicUuid = q.findValue();
        if (vmNicUuid == null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("eip[uuid:%s] has not attached to any vm nic", msg.getUuid())
            ));
        }
    }

    private void validate(APIDeleteEipMsg msg) {
        if (!dbf.isExist(msg.getUuid(), EipVO.class)) {
            APIDeleteEipEvent evt = new APIDeleteEipEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void isVmNicUsed(String vmNicUuid) {
        SimpleQuery<EipVO> eq = dbf.createQuery(EipVO.class);
        eq.select(EipVO_.uuid);
        eq.add(EipVO_.vmNicUuid, Op.EQ, vmNicUuid);
        String eipUuid = eq.findValue();
        if (eipUuid != null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("vm nic[uuid:%s] has attached to another eip[uuid:%s]", vmNicUuid, eipUuid)
            ));
        }
    }

    private void isVipInVmNicSubnet(String eipIp, String vmNicUuid) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.select(VmNicVO_.gateway, VmNicVO_.netmask);
        q.add(VmNicVO_.uuid, Op.EQ, vmNicUuid);
        Tuple t = q.findTuple();
        String gw = t.get(0, String.class);
        String netmask = t.get(1, String.class);
        SubnetUtils sub = new SubnetUtils(gw, netmask);
        if (sub.getInfo().isInRange(eipIp)) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("overlap public and private subnets. The subnet of EIP[%s] is an overlap with the subnet[%s/%s]" +
                            " of the VM nic[uuid:%s].", eipIp, gw, netmask, vmNicUuid)
            ));
        }
    }

    private void validate(APICreateEipMsg msg) {
        if (msg.getVmNicUuid() != null) {
            isVmNicUsed(msg.getVmNicUuid());
        }

        VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        if (vip.getUseFor() != null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("vip[uuid:%s] has been occupied other network service entity[%s]", msg.getVipUuid(), vip.getUseFor())
            ));
        }

        if (vip.getState() != VipState.Enabled) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("vip[uuid:%s] is not in state[%s], current state is %s", msg.getVipUuid(), VipState.Enabled, vip.getState())
            ));
        }

        if (msg.getVmNicUuid() != null) {
            isVipInVmNicSubnet(vip.getIp(), msg.getVmNicUuid());

            SimpleQuery<VmNicVO> nicq = dbf.createQuery(VmNicVO.class);
            nicq.select(VmNicVO_.l3NetworkUuid);
            nicq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
            String nicL3Uuid = nicq.findValue();
            if (nicL3Uuid.equals(vip.getL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("guest l3Network of vm nic[uuid:%s] and vip l3Network of vip[uuid: %s] are the same network", msg.getVmNicUuid(), msg.getVipUuid())
                ));
            }
        }
    }
}
