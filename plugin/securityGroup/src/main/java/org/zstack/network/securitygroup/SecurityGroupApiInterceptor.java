package org.zstack.network.securitygroup;

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
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SecurityGroupApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSecurityGroupRuleMsg) {
            validate((APIAddSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIAddVmNicToSecurityGroupMsg) {
            validate((APIAddVmNicToSecurityGroupMsg) msg);
        } else if (msg instanceof APIAttachSecurityGroupToL3NetworkMsg) {
            validate((APIAttachSecurityGroupToL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupMsg) {
            validate((APIDeleteSecurityGroupMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupRuleMsg) {
            validate((APIDeleteSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIDeleteVmNicFromSecurityGroupMsg) {
            validate((APIDeleteVmNicFromSecurityGroupMsg) msg);
        } else if (msg instanceof APIDetachSecurityGroupFromL3NetworkMsg) {
            validate((APIDetachSecurityGroupFromL3NetworkMsg) msg);
        }

        return msg;
    }

    private void validate(APIDetachSecurityGroupFromL3NetworkMsg msg) {
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("security group[uuid:%s] has not attached to l3Network[uuid:%s], can't detach",
                            msg.getSecurityGroupUuid(), msg.getL3NetworkUuid())
            ));
        }
    }

    private void validate(APIDeleteVmNicFromSecurityGroupMsg msg) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, msg.getVmNicUuids());
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        List<String> vmNicUuids = q.listValue();
        if (vmNicUuids.isEmpty()) {
            APIDeleteVmNicFromSecurityGroupEvent evt = new APIDeleteVmNicFromSecurityGroupEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        msg.setVmNicUuids(vmNicUuids);
    }

    private void validate(APIDeleteSecurityGroupRuleMsg msg) {
        SimpleQuery<SecurityGroupRuleVO> q = dbf.createQuery(SecurityGroupRuleVO.class);
        q.select(SecurityGroupRuleVO_.uuid);
        q.add(SecurityGroupRuleVO_.uuid, Op.IN, msg.getRuleUuids());
        List<String> uuids = q.listValue();
        uuids.retainAll(msg.getRuleUuids());
        if (uuids.isEmpty()) {
            APIDeleteSecurityGroupRuleEvent evt = new APIDeleteSecurityGroupRuleEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        msg.setRuleUuids(uuids);
    }

    private void validate(APIDeleteSecurityGroupMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SecurityGroupVO.class)) {
            APIDeleteSecurityGroupEvent evt = new APIDeleteSecurityGroupEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIAttachSecurityGroupToL3NetworkMsg msg) {
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("security group[uuid:%s] has attached to l3Network[uuid:%s], can't attach again",
                            msg.getSecurityGroupUuid(), msg.getL3NetworkUuid())
            ));
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        nq.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE);
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the L3 network[uuid:%s] doesn't have the network service type[%s] enabled", msg.getL3NetworkUuid(), SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE)
            ));
        }
    }

    private void validate(APIAddVmNicToSecurityGroupMsg msg) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.select(VmNicVO_.uuid);
        q.add(VmNicVO_.uuid, Op.IN, msg.getVmNicUuids());
        List<String> uuids = q.listValue();
        if (!uuids.containsAll(msg.getVmNicUuids())) {
            msg.getVmNicUuids().removeAll(uuids);
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find vm nics[uuids:%s]", msg.getVmNicUuids())
            ));
        }

        checkIfVmNicFromAttachedL3Networks(msg.getSecurityGroupUuid(), uuids);

        msg.setVmNicUuids(uuids);
    }

    @Transactional(readOnly = true)
    private void checkIfVmNicFromAttachedL3Networks(String securityGroupUuid, List<String> uuids) {
        String sql = "select nic.uuid from SecurityGroupL3NetworkRefVO ref, VmNicVO nic where ref.l3NetworkUuid = nic.l3NetworkUuid" +
                " and ref.securityGroupUuid = :sgUuid and nic.uuid in (:nicUuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuids", uuids);
        q.setParameter("sgUuid", securityGroupUuid);
        List<String> nicUuids = q.getResultList();

        List<String> wrongUuids = new ArrayList<String>();
        for (String uuid : uuids) {
            if (!nicUuids.contains(uuid)) {
                wrongUuids.add(uuid);
            }
        }

        if (!wrongUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("VM nics[uuids:%s] are not on L3 networks that have been attached to the security group[uuid:%s]",
                            wrongUuids, securityGroupUuid)
            ));
        }
    }

    private boolean checkSecurityGroupRuleEqual(SecurityGroupRuleAO rule1, SecurityGroupRuleAO rule2) {
        if (rule1 == rule2) {
            return true;
        }

        if (rule1.getStartPort().equals(rule2.getStartPort()) &&
                rule1.getEndPort().equals(rule2.getEndPort()) &&
                rule1.getProtocol().equals(rule2.getProtocol()) &&
                rule1.getType().equals(rule2.getType())) {
            //
            if (rule1.getAllowedCidr() == null) {
                if (rule2.getAllowedCidr() == null ||
                        rule2.getAllowedCidr().equals("") ||
                        rule2.getAllowedCidr().equals(SecurityGroupConstant.WORLD_OPEN_CIDR)) {
                    return true;
                }
            }
            if (rule2.getAllowedCidr() == null) {
                if (rule1.getAllowedCidr() == null ||
                        rule1.getAllowedCidr().equals("") ||
                        rule1.getAllowedCidr().equals(SecurityGroupConstant.WORLD_OPEN_CIDR)) {
                    return true;
                }
            }
            if (rule1.getAllowedCidr().equals(rule2.getAllowedCidr())) {
                return true;
            }
        }

        return false;
    }

    private void validate(APIAddSecurityGroupRuleMsg msg) {
        // Basic check
        for (SecurityGroupRuleAO ao : msg.getRules()) {
            if (ao.getType() == null) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("rule type can not be null. rule dump: %s", JSONObjectUtil.toJsonString(ao))
                ));
            }

            if (!ao.getType().equals(SecurityGroupRuleType.Egress.toString()) &&
                    !ao.getType().equals(SecurityGroupRuleType.Ingress.toString())) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("unknown rule type[%s], rule can only be Ingress/Egress. rule dump: %s",
                                ao.getType(), JSONObjectUtil.toJsonString(ao))
                ));
            }


            if (ao.getProtocol() == null) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("protocol can not be null. rule dump: %s", JSONObjectUtil.toJsonString(ao))
                ));
            }

            try {
                SecurityGroupRuleProtocolType.valueOf(ao.getProtocol());
            } catch (Exception e) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("invalid protocol[%s]. Valid protocols are [TCP, UDP, ICMP]. rule dump: %s",
                                ao.getProtocol(), JSONObjectUtil.toJsonString(ao))
                ));
            }

            if (ao.getStartPort() == null) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("startPort can not be null. rule dump: %s", JSONObjectUtil.toJsonString(ao))
                ));
            }

            if (SecurityGroupRuleProtocolType.ICMP.toString().equals(ao.getProtocol())) {
                if (ao.getStartPort() < -1 || ao.getStartPort() > 255) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("invalid ICMP type[%s]. Valid type is [-1, 255]. rule dump: %s",
                                    ao.getStartPort(), JSONObjectUtil.toJsonString(ao))
                    ));
                }
            } else {
                if (ao.getStartPort() < 0 || ao.getStartPort() > 65535) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("invalid startPort[%s]. Valid range is [0, 65535]. rule dump: %s",
                                    ao.getStartPort(), JSONObjectUtil.toJsonString(ao))
                    ));
                }
            }


            if (ao.getEndPort() == null) {
                ao.setEndPort(ao.getStartPort());
            }

            if (SecurityGroupRuleProtocolType.ICMP.toString().equals(ao.getProtocol())) {
                if (ao.getEndPort() < -1 || ao.getEndPort() > 3) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("invalid ICMP code[%s]. Valid range is [-1, 3]. rule dump: %s",
                                    ao.getEndPort(), JSONObjectUtil.toJsonString(ao))
                    ));
                }
            } else {
                if (ao.getEndPort() < 0 || ao.getEndPort() > 65535) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("invalid endPort[%s]. Valid range is [0, 65535]. rule dump: %s",
                                    ao.getEndPort(), JSONObjectUtil.toJsonString(ao))
                    ));
                }
            }


            if (ao.getAllowedCidr() != null && !NetworkUtils.isCidr(ao.getAllowedCidr())) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("invalid CIDR[%s]. rule dump: %s", ao.getAllowedCidr(), JSONObjectUtil.toJsonString(ao))
                ));
            }
        }

        // Deduplicate in msg
        for (int i = 0; i < msg.getRules().size() - 1; i++) {
            for (int j = msg.getRules().size() - 1; j > i; j--) {
                if (checkSecurityGroupRuleEqual(msg.getRules().get(j), msg.getRules().get(i))) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("rule should not be duplicated. rule dump: %s",
                                    JSONObjectUtil.toJsonString(msg.getRules().get(j)))
                    ));
                }
            }
        }

        // Deduplicate in database
        SimpleQuery<SecurityGroupRuleVO> lsquery = dbf.createQuery(SecurityGroupRuleVO.class);
        lsquery.add(SecurityGroupRuleVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        List<SecurityGroupRuleVO> vos = lsquery.list();
        for (SecurityGroupRuleVO svo : vos) {
            SecurityGroupRuleAO ao = new SecurityGroupRuleAO();
            ao.setType(svo.getType().toString());
            ao.setAllowedCidr(svo.getAllowedCidr());
            ao.setProtocol(svo.getProtocol().toString());
            ao.setStartPort(svo.getStartPort());
            ao.setEndPort(svo.getEndPort());

            for (SecurityGroupRuleAO sao : msg.getRules()) {
                if (checkSecurityGroupRuleEqual(ao, sao)) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("rule exist. rule dump: %s",
                                    JSONObjectUtil.toJsonString(sao))
                    ));
                }
            }

        }

        // fin
        for (SecurityGroupRuleAO ao : msg.getRules()) {
            int start = Math.min(ao.getStartPort(), ao.getEndPort());
            int end = Math.max(ao.getStartPort(), ao.getEndPort());
            ao.setStartPort(start);
            ao.setEndPort(end);

            if (ao.getAllowedCidr() == null) {
                ao.setAllowedCidr(SecurityGroupConstant.WORLD_OPEN_CIDR);
            }
        }
    }
}
