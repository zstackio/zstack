package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.quota.QuotaConstant;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.query.QueryFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.zstack.network.securitygroup.SecurityGroupMembersTO.ACTION_CODE_DELETE_GROUP;
import static org.zstack.utils.CollectionDSL.list;

public class SecurityGroupManagerImpl extends AbstractService implements SecurityGroupManager, ManagementNodeReadyExtensionPoint,
        VmInstanceMigrateExtensionPoint, AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint {
    private static CLogger logger = Utils.getLogger(SecurityGroupManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private QueryFacade qf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;

    protected Map<String, SecurityGroupHypervisorBackend> hypervisorBackends;
    private int failureHostWorkerInterval;
    private int failureHostEachTimeTake;
    private Future<Void> failureHostCopingThread;

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateSecurityGroupMsg) {
                        check((APICreateSecurityGroupMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(SecurityGroupConstant.QUOTA_SG_NUM);
                usage.setUsed(getUsedSg(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedSg(String accountUuid) {
                String sql = "select count(sg) from SecurityGroupVO sg, AccountResourceRefVO ref where ref.resourceUuid = sg.uuid" +
                        " and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", SecurityGroupVO.class.getSimpleName());
                Long sgn = q.getSingleResult();
                sgn = sgn == null ? 0 : sgn;
                return sgn;
            }

            private void check(APICreateSecurityGroupMsg msg, Map<String, QuotaPair> pairs) {
                long sgNum = pairs.get(SecurityGroupConstant.QUOTA_SG_NUM).getValue();
                long sgn = getUsedSg(msg.getSession().getAccountUuid());

                if (sgn + 1 > sgNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), SecurityGroupConstant.QUOTA_SG_NUM, sgNum)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        quota.setOperator(checker);
        quota.addMessageNeedValidation(APICreateSecurityGroupMsg.class);

        QuotaPair p = new QuotaPair();
        p.setName(SecurityGroupConstant.QUOTA_SG_NUM);
        p.setValue(QuotaConstant.QUOTA_SG_NUM);
        quota.addPair(p);

        return list(quota);
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        startFailureHostCopingThread();
    }

    private class RuleCalculator {
        private List<String> vmNicUuids;
        private List<String> l3NetworkUuids;
        private List<String> securityGroupUuids;
        private List<String> hostUuids;
        private List<VmInstanceState> vmStates;

        List<HostRuleTO> calculate() {
            if (vmNicUuids != null) {
                return calculateByVmNic();
            } else if (l3NetworkUuids != null && securityGroupUuids != null) {
                return calculateByL3NetworkAndSecurityGroup();
            } else if (l3NetworkUuids != null) {
                return calculateByL3Network();
            } else if (securityGroupUuids != null) {
                return calculateBySecurityGroup();
            } else if (hostUuids != null) {
                return calculateByHost();
            }

            throw new CloudRuntimeException("should not be here");
        }

        @Transactional(readOnly = true)
        HostSecurityGroupMembersTO returnHostSecurityGroupMember(String sgUuid){
            HostSecurityGroupMembersTO hto = new HostSecurityGroupMembersTO();
            SecurityGroupMembersTO gto = new SecurityGroupMembersTO();
            gto.setSecurityGroupVmIps(getVmIpsBySecurityGroup(sgUuid));
            gto.setSecurityGroupUuid(sgUuid);
            hto.setGroupMembersTO(gto);
            Set<String> hostUuids = new HashSet<>();

            List<Tuple> ts = SQL.New("select vm.hostUuid, vm.hypervisorType" +
                    " from VmNicVO nic, VmInstanceVO vm, VmNicSecurityGroupRefVO ref" +
                    " where vm.uuid = nic.vmInstanceUuid" +
                    " and nic.uuid = ref.vmNicUuid" +
                    " and ref.securityGroupUuid in" +
                    " (" +
                    " select rule.securityGroupUuid from SecurityGroupRuleVO rule" +
                    " where rule.remoteSecurityGroupUuid =:sgUuid" +
                    " )", Tuple.class).param("sgUuid", sgUuid).list();
            for(Tuple t : ts){
                if(t.get(0, String.class) != null){
                    hostUuids.add(t.get(0, String.class));
                }
                hto.setHypervisorType(t.get(1, String.class));
            }
            hto.setHostUuids(new ArrayList<>(hostUuids));
            return hto;
        }

        @Transactional(readOnly = true)
        private List<HostRuleTO> calculateByHost() {
            String sql = "select nic.uuid from VmNicVO nic, VmInstanceVO vm, VmNicSecurityGroupRefVO ref" +
                    " where nic.uuid = ref.vmNicUuid and nic.vmInstanceUuid = vm.uuid" +
                    " and vm.hostUuid in (:hostUuids) and vm.state in (:vmStates)";
            TypedQuery<String> insgQuery = dbf.getEntityManager().createQuery(sql, String.class);
            insgQuery.setParameter("hostUuids", hostUuids);
            insgQuery.setParameter("vmStates", vmStates);
            List<String> nicsInSg = insgQuery.getResultList();

            sql = "select nic.uuid from VmNicVO nic, VmInstanceVO vm where nic.vmInstanceUuid = vm.uuid" +
                    " and vm.hostUuid in (:hostUuids) and vm.state in (:vmStates)";
            TypedQuery<String> allq = dbf.getEntityManager().createQuery(sql, String.class);
            allq.setParameter("hostUuids", hostUuids);
            allq.setParameter("vmStates", vmStates);
            List<String> allNics = allq.getResultList();
            allNics.removeAll(nicsInSg);
            List<String> nicsOutSg = allNics;

            List<HostRuleTO> ret = new ArrayList<HostRuleTO>();
            if (!nicsInSg.isEmpty()) {
                vmNicUuids = nicsInSg;
                ret.addAll(calculateByVmNic());
            }
            if (!nicsOutSg.isEmpty()) {
                Collection<HostRuleTO> toRemove = createRulePlaceHolder(nicsOutSg);
                for (HostRuleTO hto : toRemove) {
                    hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
                }
                ret.addAll(toRemove);
            }

            return ret;
        }

        @Transactional(readOnly = true)
        private List<HostRuleTO> calculateBySecurityGroup() {
            String sql = "select ref.vmNicUuid from VmNicSecurityGroupRefVO ref where ref.securityGroupUuid in (:sgUuids)";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("sgUuids", securityGroupUuids);
            vmNicUuids = q.getResultList();
            return calculateByVmNic();
        }

        private List<HostRuleTO> calculateByL3Network() {
            return null;
        }

        List<HostRuleTO> mergeMultiHostRuleTO(Collection<HostRuleTO>... htos) {
            Map<String, HostRuleTO> hostRuleTOMap = new HashMap<String, HostRuleTO>();
            for (Collection<HostRuleTO> lst : htos) {
                for (HostRuleTO hto : lst) {
                    HostRuleTO old = hostRuleTOMap.get(hto.getHostUuid());
                    if (old == null) {
                        hostRuleTOMap.put(hto.getHostUuid(), hto);
                    } else {
                        old.getRules().addAll(hto.getRules());
                    }
                }
            }

            List<HostRuleTO> ret = new ArrayList<HostRuleTO>(hostRuleTOMap.size());
            ret.addAll(hostRuleTOMap.values());
            return ret;
        }

        private List<HostRuleTO> calculateByL3NetworkAndSecurityGroup() {
            String sql = "select ref.vmNicUuid from VmNicSecurityGroupRefVO ref, SecurityGroupL3NetworkRefVO l3ref, VmNicVO nic where l3ref.securityGroupUuid = ref.securityGroupUuid and nic.uuid = ref.vmNicUuid and nic.l3NetworkUuid = l3ref.l3NetworkUuid and ref.securityGroupUuid in (:sgUuids) and l3ref.l3NetworkUuid in (:l3Uuids)";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("sgUuids", securityGroupUuids);
            q.setParameter("l3Uuids", l3NetworkUuids);
            vmNicUuids = q.getResultList();
            return calculateByVmNic();
        }

        private List<RuleTO> calculateRuleTOBySecurityGroup(List<String> sgUuids, String l3Uuid) {
            List<RuleTO> ret = new ArrayList<>();

            for (String sgUuid : sgUuids) {
                String sql = "select r from SecurityGroupRuleVO r where r.securityGroupUuid = :sgUuid" +
                        " and r.remoteSecurityGroupUuid is null";
                TypedQuery<SecurityGroupRuleVO> q = dbf.getEntityManager().createQuery(sql, SecurityGroupRuleVO.class);
                q.setParameter("sgUuid", sgUuid);
                List<SecurityGroupRuleVO> rules = q.getResultList();
                if (rules.isEmpty()) {
                    continue;
                }

                for (SecurityGroupRuleVO r : rules) {
                    RuleTO rto = new RuleTO();
                    rto.setAllowedCidr(r.getAllowedCidr());
                    rto.setEndPort(r.getEndPort());
                    rto.setProtocol(r.getProtocol().toString());
                    rto.setStartPort(r.getStartPort());
                    rto.setType(r.getType().toString());
                    rto.setSecurityGroupUuid(r.getSecurityGroupUuid());
                    ret.add(rto);
                }
            }

            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("\n-------------- begin calculateRuleTOBySecurityGroupUuid ---------------------"));
                sb.append(String.format("\ninput security group uuids: %s", sgUuids));
                sb.append(String.format("\nresult: %s", JSONObjectUtil.toJsonString(ret)));
                sb.append(String.format("\n-------------- end calculateRuleTOBySecurityGroupUuid ---------------------"));
                logger.trace(sb.toString());
            }

            return ret;
        }

        private List<RuleTO> calculateSecurityGroupBaseRule(List<String> sgUuids, String l3Uuid){
            List<RuleTO> rules = new ArrayList<>();
            for(String sgUuid : sgUuids){
                String sql = "select r from SecurityGroupRuleVO r where r.securityGroupUuid = :sgUuid" +
                        " and r.remoteSecurityGroupUuid is not null";
                TypedQuery<SecurityGroupRuleVO> q = dbf.getEntityManager().createQuery(sql, SecurityGroupRuleVO.class);
                q.setParameter("sgUuid", sgUuid);
                List<SecurityGroupRuleVO> remoteRules = q.getResultList();

                for(SecurityGroupRuleVO r : remoteRules){
                    RuleTO rule = new RuleTO();
                    rule.setStartPort(r.getStartPort());
                    rule.setEndPort(r.getEndPort());
                    rule.setProtocol(r.getProtocol().toString());
                    rule.setType(r.getType().toString());
                    rule.setAllowedCidr(r.getAllowedCidr());
                    rule.setSecurityGroupUuid(sgUuid);
                    rule.setRemoteGroupUuid(r.getRemoteSecurityGroupUuid());
                    // TODO: the same group only transport once
                    rule.setRemoteGroupVmIps(getVmIpsBySecurityGroup(r.getRemoteSecurityGroupUuid()));
                    rules.add(rule);
                }
            }
            return rules;

        }

        private List<String> getVmIpsBySecurityGroup(String sgUuid){
            // TODO: if two L3 network which have same ip segment attached same sg, it might has a problem
            String sql = "select nic.ip" +
                    " from VmNicVO nic, VmNicSecurityGroupRefVO ref" +
                    " where ref.vmNicUuid = nic.uuid" +
                    " and ref.securityGroupUuid = :sgUuid" +
                    " and nic.ip is not null";
            TypedQuery<String> internalIpQuery = dbf.getEntityManager().createQuery(sql, String.class);
            internalIpQuery.setParameter("sgUuid", sgUuid);

            return internalIpQuery.getResultList();
        }

        @Transactional(readOnly = true)
        Collection<HostRuleTO> createRulePlaceHolder(List<String> nicUuids) {
            String sql = "select nic.uuid, vm.hostUuid, vm.hypervisorType, nic.internalName, nic.mac, nic.ip from VmInstanceVO vm, VmNicVO nic where nic.vmInstanceUuid = vm.uuid and vm.hostUuid is not null and nic.uuid in (:nicUuids) group by nic.uuid";
            TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
            q.setParameter("nicUuids", nicUuids);
            List<Tuple> tuples = q.getResultList();

            sql = "select nic.uuid, vm.lastHostUuid, vm.hypervisorType, nic.internalName, nic.mac, nic.ip from VmInstanceVO vm, VmNicVO nic where nic.vmInstanceUuid = vm.uuid and vm.hostUuid is null and vm.lastHostUuid is not null and nic.uuid in (:nicUuids) group by nic.uuid";
            q = dbf.getEntityManager().createQuery(sql, Tuple.class);
            q.setParameter("nicUuids", nicUuids);
            tuples.addAll(q.getResultList());

            Map<String, HostRuleTO> hostRuleTOMap = new HashMap<String, HostRuleTO>();
            for (Tuple t : tuples) {
                String nicUuid = t.get(0, String.class);
                String hostUuid = t.get(1, String.class);
                String hvType = t.get(2, String.class);
                String nicName = t.get(3, String.class);
                String mac = t.get(4, String.class);
                String ip = t.get(5, String.class);

                SecurityGroupRuleTO sgto = new SecurityGroupRuleTO();
                sgto.setEgressDefaultPolicy(SecurityGroupGlobalConfig.EGRESS_RULE_DEFAULT_POLICY.value(String.class));
                sgto.setIngressDefaultPolicy(SecurityGroupGlobalConfig.INGRESS_RULE_DEFAULT_POLICY.value(String.class));
                sgto.setRules(new ArrayList<RuleTO>());
                sgto.setVmNicUuid(nicUuid);
                sgto.setVmNicInternalName(nicName);
                sgto.setVmNicMac(mac);
                sgto.setVmNicIp(ip);

                HostRuleTO hto = hostRuleTOMap.get(hostUuid);
                if (hto == null) {
                    hto = new HostRuleTO();
                    hto.setHostUuid(hostUuid);
                    hto.setHypervisorType(hvType);
                    hostRuleTOMap.put(hto.getHostUuid(), hto);
                }

                hto.getRules().add(sgto);
            }

            return hostRuleTOMap.values();
        }

        @Transactional(readOnly = true)
        private List<HostRuleTO> calculateByVmNic() {
            Map<String, HostRuleTO> hostRuleMap = new HashMap<String, HostRuleTO>();
            List<HostRuleTO> htos = new ArrayList<HostRuleTO>();

            for (String nicUuid : vmNicUuids) {
                List<Tuple> tuples;
                if (vmStates != null && !vmStates.isEmpty()) {
                    String sql = "select ref.securityGroupUuid, vm.hostUuid, vm.hypervisorType, nic.internalName, nic.l3NetworkUuid, nic.mac, nic.ip from VmNicSecurityGroupRefVO ref, VmInstanceVO vm, VmNicVO nic where ref.vmNicUuid = nic.uuid and nic.vmInstanceUuid = vm.uuid and ref.vmNicUuid = :nicUuid and vm.state in (:vmStates)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("nicUuid", nicUuid);
                    q.setParameter("vmStates", vmStates);
                    tuples = q.getResultList();
                } else {
                    String sql = "select ref.securityGroupUuid, vm.hostUuid, vm.hypervisorType, nic.internalName, nic.l3NetworkUuid, nic.mac, nic.ip from VmNicSecurityGroupRefVO ref, VmInstanceVO vm, VmNicVO nic where ref.vmNicUuid = nic.uuid and nic.vmInstanceUuid = vm.uuid and ref.vmNicUuid = :nicUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("nicUuid", nicUuid);
                    tuples = q.getResultList();
                }

                if (tuples.isEmpty()) {
                    // vm is not in vmStates or not in security group
                    continue;
                }

                List<String> sgUuids = new ArrayList<String>();
                String hostUuid = null;
                String hypervisorType = null;
                String nicName = null;
                String l3Uuid = null;
                String mac = null;
                String ip = null;
                for (Tuple t : tuples) {
                    sgUuids.add(t.get(0, String.class));
                    hostUuid = t.get(1, String.class);
                    hypervisorType = t.get(2, String.class);
                    nicName = t.get(3, String.class);
                    l3Uuid = t.get(4, String.class);
                    mac = t.get(5, String.class);
                    ip = t.get(6, String.class);
                }

                List<RuleTO> rtos = calculateRuleTOBySecurityGroup(sgUuids, l3Uuid);
                List<RuleTO> securityGroupBaseRules = calculateSecurityGroupBaseRule(sgUuids, l3Uuid);

                SecurityGroupRuleTO sgto = new SecurityGroupRuleTO();
                sgto.setEgressDefaultPolicy(SecurityGroupGlobalConfig.EGRESS_RULE_DEFAULT_POLICY.value(String.class));
                sgto.setIngressDefaultPolicy(SecurityGroupGlobalConfig.INGRESS_RULE_DEFAULT_POLICY.value(String.class));
                sgto.setRules(rtos);
                sgto.setVmNicUuid(nicUuid);
                sgto.setVmNicInternalName(nicName);
                sgto.setVmNicMac(mac);
                sgto.setVmNicIp(ip);
                sgto.setSecurityGroupBaseRules(securityGroupBaseRules);

                HostRuleTO hto = hostRuleMap.get(hostUuid);
                if (hto == null) {
                    hto = new HostRuleTO();
                    hto.setHostUuid(hostUuid);
                    hto.setHypervisorType(hypervisorType);
                    hostRuleMap.put(hto.getHostUuid(), hto);
                }
                hto.getRules().add(sgto);
            }

            htos.addAll(hostRuleMap.values());

            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("\n=================== begin rulesByNicUuids ======================"));
                sb.append(String.format("\ninput vmNic uuids: %s", vmNicUuids));
                sb.append(String.format("\nresult: %s", JSONObjectUtil.toJsonString(htos)));
                sb.append(String.format("\n=================== end rulesByNicUuids ========================"));
                logger.trace(sb.toString());
            }

            return htos;
        }
    }

    @MessageSafe
    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof RefreshSecurityGroupRulesOnHostMsg) {
            handle((RefreshSecurityGroupRulesOnHostMsg) msg);
        } else if (msg instanceof RefreshSecurityGroupRulesOnVmMsg) {
            handle((RefreshSecurityGroupRulesOnVmMsg) msg);
        } else if (msg instanceof RemoveVmNicFromSecurityGroupMsg) {
            handle((RemoveVmNicFromSecurityGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(RemoveVmNicFromSecurityGroupMsg msg) {
        RemoveVmNicFromSecurityGroupReply reply = new RemoveVmNicFromSecurityGroupReply();
        removeNicFromSecurityGroup(msg.getSecurityGroupUuid(), msg.getVmNicUuids());
        bus.reply(msg, reply);
    }

    private void handle(RefreshSecurityGroupRulesOnVmMsg msg) {
        RefreshSecurityGroupRulesOnVmReply reply = new RefreshSecurityGroupRulesOnVmReply();
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        q.add(VmNicSecurityGroupRefVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
        List<String> nicUuids = q.listValue();
        if (nicUuids.isEmpty()) {
            logger.debug(String.format("no nic of vm[uuid:%s] needs to refresh security group rule", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        Collection<HostRuleTO> htos;
        if (msg.isDeleteAllRules()) {
            RuleCalculator cal = new RuleCalculator();
            htos = cal.createRulePlaceHolder(nicUuids);
            for (HostRuleTO hto : htos) {
                hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
            }
        } else {
            RuleCalculator cal = new RuleCalculator();
            cal.vmNicUuids = nicUuids;
            htos = cal.calculate();
        }

        for (HostRuleTO hto : htos) {
            if (hto.getHostUuid() == null) {
                hto.setHostUuid(msg.getHostUuid());
            }
        }

        applyRules(htos);
        logger.debug(String.format("refreshed security group rule for vm[uuid:%s]", msg.getVmInstanceUuid()));
        bus.reply(msg, reply);
    }

    private void createFailureHostTask(String huuid) {
        SecurityGroupFailureHostVO fvo = new SecurityGroupFailureHostVO();
        fvo.setHostUuid(huuid);
        dbf.persist(fvo);
    }

    private void handle(RefreshSecurityGroupRulesOnHostMsg msg) {
        RuleCalculator cal = new RuleCalculator();
        cal.hostUuids = asList(msg.getHostUuid());
        // refreshing may happen when host is reconnecting; at that time VMs' states are Unknown
        cal.vmStates = asList(VmInstanceState.Unknown, VmInstanceState.Running);
        List<HostRuleTO> htos = cal.calculate();
        for (HostRuleTO hto : htos) {
            hto.setRefreshHost(true);
        }
        logger.debug(String.format("required to refresh rules on host[uuid:%s]", msg.getHostUuid()));
        applyRules(htos);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateSecurityGroupMsg) {
            handle((APICreateSecurityGroupMsg) msg);
        } else if (msg instanceof APIListSecurityGroupMsg) {
            handle((APIListSecurityGroupMsg) msg);
        } else if (msg instanceof APIAddSecurityGroupRuleMsg) {
            handle((APIAddSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIAddVmNicToSecurityGroupMsg) {
            handle((APIAddVmNicToSecurityGroupMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupRuleMsg) {
            handle((APIDeleteSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupMsg) {
            handle((APIDeleteSecurityGroupMsg) msg);
        } else if (msg instanceof APIDeleteVmNicFromSecurityGroupMsg) {
            handle((APIDeleteVmNicFromSecurityGroupMsg) msg);
        } else if (msg instanceof APIListVmNicInSecurityGroupMsg) {
            handle((APIListVmNicInSecurityGroupMsg) msg);
        } else if (msg instanceof APIAttachSecurityGroupToL3NetworkMsg) {
            handle((APIAttachSecurityGroupToL3NetworkMsg) msg);
        }  else if (msg instanceof APIChangeSecurityGroupStateMsg) {
            handle((APIChangeSecurityGroupStateMsg) msg);
        } else if (msg instanceof APIDetachSecurityGroupFromL3NetworkMsg) {
            handle((APIDetachSecurityGroupFromL3NetworkMsg) msg);
        } else if (msg instanceof APIGetCandidateVmNicForSecurityGroupMsg) {
            handle((APIGetCandidateVmNicForSecurityGroupMsg) msg);
        } else if (msg instanceof APIUpdateSecurityGroupMsg) {
            handle((APIUpdateSecurityGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateSecurityGroupMsg msg) {
        boolean update = false;
        SecurityGroupVO vo = dbf.findByUuid(msg.getUuid(), SecurityGroupVO.class);
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }
        APIUpdateSecurityGroupEvent evt = new APIUpdateSecurityGroupEvent(msg.getId());
        evt.setInventory(SecurityGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Transactional(readOnly = true)
    private List<VmNicVO> getCandidateVmNic(String sgId, String accountUuid) {
        List<String> nicUuidsToInclude = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VmNicVO.class);
        if (nicUuidsToInclude != null && nicUuidsToInclude.isEmpty()) {
            return new ArrayList<VmNicVO>();
        }

        String sql = "select ref.vmNicUuid from VmNicSecurityGroupRefVO ref where ref.securityGroupUuid = :sgUuid";
        TypedQuery<String> nq = dbf.getEntityManager().createQuery(sql, String.class);
        nq.setParameter("sgUuid", sgId);
        List<String> nicUuidsToExclued = nq.getResultList();

        TypedQuery<VmNicVO> q;
        if (nicUuidsToInclude == null) {
            // accessed by an admin
            if (nicUuidsToExclued.isEmpty()) {
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref " +
                        "where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
                        " and sg.uuid = :sgUuid and vm.type = :vmType and vm.state in (:vmStates) group by nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
            } else {
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref " +
                        "where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
                        " and sg.uuid = :sgUuid and vm.type = :vmType and vm.state in (:vmStates) and nic.uuid not in (:nicUuids) group by nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
                q.setParameter("nicUuids", nicUuidsToExclued);
            }
        } else {
            // accessed by a normal account
            if (nicUuidsToExclued.isEmpty()) {
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref " +
                        "where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
                        " and sg.uuid = :sgUuid and vm.type = :vmType and vm.state in (:vmStates) and nic.uuid in (:iuuids) group by nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
                q.setParameter("iuuids", nicUuidsToInclude);
            } else {
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref " +
                        "where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
                        " and sg.uuid = :sgUuid and vm.type = :vmType and vm.state in (:vmStates) and nic.uuid not in (:nicUuids) and nic.uuid in (:iuuids) group by nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
                q.setParameter("nicUuids", nicUuidsToExclued);
                q.setParameter("iuuids", nicUuidsToInclude);
            }
        }


        q.setParameter("sgUuid", sgId);
        q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
        q.setParameter("vmStates", list(VmInstanceState.Running, VmInstanceState.Stopped));
        return q.getResultList();
    }

    private void handle(APIGetCandidateVmNicForSecurityGroupMsg msg) {
        APIGetCandidateVmNicForSecurityGroupReply reply = new APIGetCandidateVmNicForSecurityGroupReply();
        reply.setInventories(VmNicInventory.valueOf(getCandidateVmNic(msg.getSecurityGroupUuid(), msg.getSession().getAccountUuid())));
        bus.reply(msg, reply);
    }

    @Transactional
    private void detachSecurityGroupFromL3Network(String sgUuid, String l3Uuid) {
        String sql = "select ref.uuid from VmNicSecurityGroupRefVO ref, VmNicVO nic where nic.uuid = ref.vmNicUuid and nic.l3NetworkUuid = :l3Uuid and ref.securityGroupUuid = :sgUuid";
        TypedQuery<String> tq = dbf.getEntityManager().createQuery(sql, String.class);
        tq.setParameter("l3Uuid", l3Uuid);
        tq.setParameter("sgUuid", sgUuid);
        List<String> refUuids = tq.getResultList();
        if (!refUuids.isEmpty()) {
            sql = "delete from VmNicSecurityGroupRefVO ref where ref.uuid in (:uuids)";
            Query q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("uuids", refUuids);
            q.executeUpdate();
        }

        sql = "delete from SecurityGroupL3NetworkRefVO ref where ref.l3NetworkUuid = :l3Uuid and ref.securityGroupUuid = :sgUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("l3Uuid", l3Uuid);
        q.setParameter("sgUuid", sgUuid);
        q.executeUpdate();
    }

    @Transactional(readOnly = true)
    private List<String> getVmNicUuidsToRemoveForDetachSecurityGroup(String sgUuid, String l3Uuid) {
        String sql = "select nic.uuid from VmNicVO nic, VmNicSecurityGroupRefVO ref where ref.vmNicUuid = nic.uuid and nic.l3NetworkUuid = :l3Uuid and ref.securityGroupUuid = :sgUuid";
        TypedQuery<String> tq = dbf.getEntityManager().createQuery(sql, String.class);
        tq.setParameter("l3Uuid", l3Uuid);
        tq.setParameter("sgUuid", sgUuid);
        return tq.getResultList();
    }

    private void handle(APIDetachSecurityGroupFromL3NetworkMsg msg) {
        List<String> vmNicUuids = getVmNicUuidsToRemoveForDetachSecurityGroup(msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());

        if (!vmNicUuids.isEmpty()) {
            removeNicFromSecurityGroup(msg.getSecurityGroupUuid(), vmNicUuids);
        }

        detachSecurityGroupFromL3Network(msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());

        APIDetachSecurityGroupFromL3NetworkEvent evt = new APIDetachSecurityGroupFromL3NetworkEvent(msg.getId());
        SecurityGroupVO vo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
        evt.setInventory(SecurityGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIChangeSecurityGroupStateMsg msg) {
        SecurityGroupStateEvent sevt = SecurityGroupStateEvent.valueOf(msg.getStateEvent());
        SecurityGroupVO vo = dbf.findByUuid(msg.getUuid(), SecurityGroupVO.class);
        if (sevt == SecurityGroupStateEvent.enable) {
            vo.setState(SecurityGroupState.Enabled);
        } else {
            vo.setState(SecurityGroupState.Disabled);
        }
        vo = dbf.updateAndRefresh(vo);

        APIChangeSecurityGroupStateEvent evt = new APIChangeSecurityGroupStateEvent(msg.getId());
        evt.setInventory(SecurityGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIAttachSecurityGroupToL3NetworkMsg msg) {
        APIAttachSecurityGroupToL3NetworkEvent evt = new APIAttachSecurityGroupToL3NetworkEvent(msg.getId());
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        SecurityGroupL3NetworkRefVO ref = q.find();
        if (ref == null) {
            ref = new SecurityGroupL3NetworkRefVO();
            ref.setUuid(Platform.getUuid());
            ref.setL3NetworkUuid(msg.getL3NetworkUuid());
            ref.setSecurityGroupUuid(msg.getSecurityGroupUuid());
            dbf.persist(ref);
        }
        SecurityGroupVO sgvo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
        SecurityGroupInventory sginv = SecurityGroupInventory.valueOf(sgvo);
        evt.setInventory(sginv);
        bus.publish(evt);
    }

    private void handle(APIListVmNicInSecurityGroupMsg msg) {
        List<VmNicSecurityGroupRefVO> vos = dl.listByApiMessage(msg, VmNicSecurityGroupRefVO.class);
        List<VmNicSecurityGroupRefInventory> invs = VmNicSecurityGroupRefInventory.valueOf(vos);
        APIListVmNicInSecurityGroupReply reply = new APIListVmNicInSecurityGroupReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void removeNicFromSecurityGroup(String sgUuid, List<String> vmNicUuids) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, sgUuid);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, vmNicUuids);
        List<VmNicSecurityGroupRefVO> refVOs = q.list();

        dbf.removeCollection(refVOs, VmNicSecurityGroupRefVO.class);


        SimpleQuery<VmNicVO> l3Query = dbf.createQuery(VmNicVO.class);
        l3Query.select(VmNicVO_.l3NetworkUuid);
        l3Query.add(VmNicVO_.uuid, Op.IN, vmNicUuids);
        List<String> l3Uuids = l3Query.listValue();

        // nics may be in other security group
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = vmNicUuids;
        List<HostRuleTO> htos1 = cal.calculate();

        // create deleting chain action for nics no longer in any security group
        SimpleQuery<VmNicSecurityGroupRefVO> refq = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        refq.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        refq.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, vmNicUuids);
        List<String> nicUuidsIn = refq.listValue();
        List<String> nicsUuidsCopy = new ArrayList<String>();
        nicsUuidsCopy.addAll(vmNicUuids);
        nicsUuidsCopy.removeAll(nicUuidsIn);
        Collection<HostRuleTO> htos2 = new ArrayList<HostRuleTO>();
        if (!nicsUuidsCopy.isEmpty()) {
            htos2 = cal.createRulePlaceHolder(nicsUuidsCopy);
            for (HostRuleTO hto : htos2) {
                hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
            }
        }

        List<HostRuleTO> finalHtos = cal.mergeMultiHostRuleTO(htos1, htos1, htos2);

        applyRules(finalHtos);

        // update security group member
        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(sgUuid);
        if(!groupMemberTO.getHostUuids().isEmpty()){
            updateGroupMembers(groupMemberTO);
        }
    }

    private void handle(APIDeleteVmNicFromSecurityGroupMsg msg) {
        removeNicFromSecurityGroup(msg.getSecurityGroupUuid(), msg.getVmNicUuids());
        APIDeleteVmNicFromSecurityGroupEvent evt = new APIDeleteVmNicFromSecurityGroupEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIDeleteSecurityGroupMsg msg) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, msg.getUuid());
        List<String> vmNicUuids = q.listValue();

        RuleCalculator cal = new RuleCalculator();
        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(msg.getUuid());

        dbf.removeByPrimaryKey(msg.getUuid(), SecurityGroupVO.class);

        if (!vmNicUuids.isEmpty()) {
            cal.vmNicUuids = vmNicUuids;
            cal.vmStates = asList(VmInstanceState.Running);
            List<HostRuleTO> htos = cal.calculate();

            SimpleQuery<VmNicSecurityGroupRefVO> refq = dbf.createQuery(VmNicSecurityGroupRefVO.class);
            refq.select(VmNicSecurityGroupRefVO_.vmNicUuid);
            refq.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, vmNicUuids);
            List<String> nicUuidsIn = refq.listValue();

            vmNicUuids.removeAll(nicUuidsIn);
            if (!vmNicUuids.isEmpty()) {
                // these vm nics are no longer in any security group, delete their chains on host
                Collection<HostRuleTO> toRemove = cal.createRulePlaceHolder(vmNicUuids);
                for (HostRuleTO hto : toRemove) {
                    hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
                }

                htos = cal.mergeMultiHostRuleTO(htos, toRemove);
            }
            applyRules(htos);
        }


        if(!groupMemberTO.getHostUuids().isEmpty()){
            groupMemberTO.getGroupMembersTO().setActionCode(ACTION_CODE_DELETE_GROUP);
            updateGroupMembers(groupMemberTO);
        }

        APIDeleteSecurityGroupEvent evt = new APIDeleteSecurityGroupEvent(msg.getId());
        logger.debug(String.format("successfully deleted security group[uuid:%s]", msg.getUuid()));
        bus.publish(evt);
    }

    private void handle(APIDeleteSecurityGroupRuleMsg msg) {
        SimpleQuery<SecurityGroupRuleVO> q = dbf.createQuery(SecurityGroupRuleVO.class);
        q.select(SecurityGroupRuleVO_.securityGroupUuid);
        q.add(SecurityGroupRuleVO_.uuid, Op.EQ, msg.getRuleUuids().get(0));
        String sgUuid = q.findValue();

        dbf.removeByPrimaryKeys(msg.getRuleUuids(), SecurityGroupRuleVO.class);

        RuleCalculator cal = new RuleCalculator();
        cal.securityGroupUuids = asList(sgUuid);
        cal.vmStates = asList(VmInstanceState.Running);

        List<HostRuleTO> htos = cal.calculate();
        applyRules(htos);

        SecurityGroupVO sgvo = dbf.findByUuid(sgUuid, SecurityGroupVO.class);
        APIDeleteSecurityGroupRuleEvent evt = new APIDeleteSecurityGroupRuleEvent(msg.getId());
        evt.setInventory(SecurityGroupInventory.valueOf(sgvo));
        bus.publish(evt);
    }

    private void handle(final APIAddVmNicToSecurityGroupMsg msg) {
        APIAddVmNicToSecurityGroupEvent evt = new APIAddVmNicToSecurityGroupEvent(msg.getId());

        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.uuid, Op.IN, msg.getVmNicUuids());
        List<VmNicVO> nicvos = q.list();

        List<String> vmUuids = new ArrayList<String>();
        List<VmNicSecurityGroupRefVO> refs = new ArrayList<VmNicSecurityGroupRefVO>();
        for (VmNicVO nic : nicvos) {
            VmNicSecurityGroupRefVO vo = new VmNicSecurityGroupRefVO();
            vo.setSecurityGroupUuid(msg.getSecurityGroupUuid());
            vo.setVmInstanceUuid(nic.getVmInstanceUuid());
            vo.setVmNicUuid(nic.getUuid());
            vo.setUuid(Platform.getUuid());
            refs.add(vo);
            vmUuids.add(nic.getVmInstanceUuid());
        }
        dbf.persistCollection(refs);

        boolean triggerApplyRules = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, vmUuids)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .isExists();

        RuleCalculator cal = new RuleCalculator();
        if (triggerApplyRules) {
            cal.vmNicUuids = msg.getVmNicUuids();
            List<HostRuleTO> htos = cal.calculate();
            applyRules(htos);
        }

        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(msg.getSecurityGroupUuid());
        if(!groupMemberTO.getHostUuids().isEmpty()){
            updateGroupMembers(groupMemberTO);
        }

        logger.debug(String.format("successfully added vm nics%s to security group[uuid:%s]", msg.getVmNicUuids(), msg.getSecurityGroupUuid()));
        bus.publish(evt);
    }

    private void applyRules(Collection<HostRuleTO> htos) {
        for (final HostRuleTO h : htos) {
            SecurityGroupHypervisorBackend bkend = hypervisorBackends.get(h.getHypervisorType());
            bkend.applyRules(h, new Completion(null) {
                private void copeWithFailureHost() {
                    createFailureHostTask(h.getHostUuid());
                }

                @Override
                public void success() {
                    logger.debug(String.format("successfully applied security rules on host[uuid:%s]", h.getHostUuid()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("failed to apply security rules on host[uuid:%s], because %s, will try it later", h.getHostUuid(), errorCode));
                    copeWithFailureHost();
                }
            });
        }
    }

    private void updateGroupMembers(HostSecurityGroupMembersTO gto){
        for(String hostUuid : gto.getHostUuids()){
            SecurityGroupHypervisorBackend bkend = hypervisorBackends.get(gto.getHypervisorType());
            bkend.updateGroupMembers(gto.getGroupMembersTO(), hostUuid, new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully update security group[uuid:%s] member on host[uuid:%s]",
                            gto.getGroupMembersTO().getSecurityGroupUuid(),  hostUuid));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("fail to update security group[uuid:%s] member on host[uuid:%s]",
                            gto.getGroupMembersTO().getSecurityGroupUuid(),  hostUuid));
                }
            });
        }
    }

    private void handle(APIAddSecurityGroupRuleMsg msg) {
        APIAddSecurityGroupRuleEvent evt = new APIAddSecurityGroupRuleEvent(msg.getId());

        List<SecurityGroupRuleVO> vos = new ArrayList<SecurityGroupRuleVO>();
        for (SecurityGroupRuleAO ao : msg.getRules()) {
            if (msg.getRemoteSecurityGroupUuids() != null){
                for (String remoteGroupUuid : msg.getRemoteSecurityGroupUuids()){
                    SecurityGroupRuleVO vo = new SecurityGroupRuleVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setAllowedCidr(ao.getAllowedCidr());
                    vo.setEndPort(ao.getEndPort());
                    vo.setStartPort(ao.getStartPort());
                    vo.setProtocol(SecurityGroupRuleProtocolType.valueOf(ao.getProtocol()));
                    vo.setType(SecurityGroupRuleType.valueOf(ao.getType()));
                    vo.setSecurityGroupUuid(msg.getSecurityGroupUuid());
                    vo.setRemoteSecurityGroupUuid(remoteGroupUuid);
                    vos.add(vo);
                }
            }else {
                SecurityGroupRuleVO vo = new SecurityGroupRuleVO();
                vo.setUuid(Platform.getUuid());
                vo.setAllowedCidr(ao.getAllowedCidr());
                vo.setEndPort(ao.getEndPort());
                vo.setStartPort(ao.getStartPort());
                vo.setProtocol(SecurityGroupRuleProtocolType.valueOf(ao.getProtocol()));
                vo.setType(SecurityGroupRuleType.valueOf(ao.getType()));
                vo.setSecurityGroupUuid(msg.getSecurityGroupUuid());
                vos.add(vo);
            }
        }
        dbf.persistCollection(vos);

        RuleCalculator cal = new RuleCalculator();
        cal.securityGroupUuids = asList(msg.getSecurityGroupUuid());
        cal.vmStates = asList(VmInstanceState.Running);
        List<HostRuleTO> htos = cal.calculate();
        applyRules(htos);

        SecurityGroupVO sgvo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
        evt.setInventory(SecurityGroupInventory.valueOf(sgvo));
        logger.debug(String.format("successfully add rules to security group[uuid:%s, name:%s]:\n%s", sgvo.getUuid(), sgvo.getName(), JSONObjectUtil.toJsonString(msg.getRules())));
        bus.publish(evt);
    }

    private void handle(APIListSecurityGroupMsg msg) {
        List<SecurityGroupVO> vos = dl.listByApiMessage(msg, SecurityGroupVO.class);
        List<SecurityGroupInventory> invs = SecurityGroupInventory.valueOf(vos);
        APIListSecurityGroupReply reply = new APIListSecurityGroupReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APICreateSecurityGroupMsg msg) {
        SecurityGroupVO vo = new SecurityGroupVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(SecurityGroupState.Enabled);
        vo.setInternalId(dbf.generateSequenceNumber(SecurityGroupSequenceNumberVO.class));

        SecurityGroupVO finalVo = vo;
        vo = new SQLBatchWithReturn<SecurityGroupVO>() {
            @Override
            protected SecurityGroupVO scripts() {
                persist(finalVo);
                reload(finalVo);
                acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), finalVo.getUuid(), SecurityGroupVO.class);
                tagMgr.createTagsFromAPICreateMessage(msg, finalVo.getUuid(), SecurityGroupVO.class.getSimpleName());
                return finalVo;
            }
        }.execute();

        createDefaultRule(finalVo.getUuid());

        SecurityGroupInventory inv = SecurityGroupInventory.valueOf(vo);
        APICreateSecurityGroupEvent evt = new APICreateSecurityGroupEvent(msg.getId());
        evt.setInventory(inv);
        logger.debug(String.format("successfully created security group[uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
        bus.publish(evt);
    }

    public String getId() {
        return bus.makeLocalServiceId(SecurityGroupConstant.SERVICE_ID);
    }

    private void createDefaultRule(String sgUuid){
        List<SecurityGroupRuleVO> vos = new ArrayList<>();

        SecurityGroupRuleVO ingressRuleVo = new SecurityGroupRuleVO();
        ingressRuleVo.setUuid(Platform.getUuid());
        ingressRuleVo.setAllowedCidr("0.0.0.0/0");
        ingressRuleVo.setEndPort(-1);
        ingressRuleVo.setStartPort(-1);
        ingressRuleVo.setProtocol(SecurityGroupRuleProtocolType.ALL);
        ingressRuleVo.setType(SecurityGroupRuleType.Ingress);
        ingressRuleVo.setSecurityGroupUuid(sgUuid);
        ingressRuleVo.setRemoteSecurityGroupUuid(sgUuid);
        vos.add(ingressRuleVo);

        SecurityGroupRuleVO egressRuleVo = new SecurityGroupRuleVO();
        egressRuleVo.setUuid(Platform.getUuid());
        egressRuleVo.setAllowedCidr("0.0.0.0/0");
        egressRuleVo.setEndPort(-1);
        egressRuleVo.setStartPort(-1);
        egressRuleVo.setProtocol(SecurityGroupRuleProtocolType.ALL);
        egressRuleVo.setType(SecurityGroupRuleType.Egress);
        egressRuleVo.setSecurityGroupUuid(sgUuid);
        egressRuleVo.setRemoteSecurityGroupUuid(sgUuid);
        vos.add(egressRuleVo);

        dbf.persistCollection(vos);
    }

    private void populateExtensions() {
        hypervisorBackends = new HashMap<String, SecurityGroupHypervisorBackend>();
        for (SecurityGroupHypervisorBackend backend : pluginRgty.getExtensionList(SecurityGroupHypervisorBackend.class)) {
            SecurityGroupHypervisorBackend old = hypervisorBackends.get(backend.getSecurityGroupBackendHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate SecurityGroupHypervisorBackend[%s, %s] for type[%s]",
                        backend.getClass().getName(), old.getClass().getName(), old.getSecurityGroupBackendHypervisorType()));
            }
            hypervisorBackends.put(backend.getSecurityGroupBackendHypervisorType().toString(), backend);
        }
    }

    private void startFailureHostCopingThread() {
        failureHostCopingThread = thdf.submitPeriodicTask(new FailureHostWorker());
        logger.debug(String.format("security group failureHostCopingThread starts[failureHostEachTimeTake: %s, failureHostWorkerInterval: %ss]", failureHostEachTimeTake, failureHostWorkerInterval));
    }

    private void restartFailureHostCopingThread() {
        if (failureHostCopingThread != null) {
            failureHostCopingThread.cancel(true);
        }
        startFailureHostCopingThread();
    }

    private void prepareGlobalConfig() {
        failureHostWorkerInterval = SecurityGroupGlobalConfig.FAILURE_HOST_WORKER_INTERVAL.value(Integer.class);
        failureHostEachTimeTake = SecurityGroupGlobalConfig.FAILURE_HOST_EACH_TIME_TO_TAKE.value(Integer.class);

        GlobalConfigUpdateExtensionPoint onUpdate = new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (SecurityGroupGlobalConfig.FAILURE_HOST_EACH_TIME_TO_TAKE.isMe(newConfig)) {
                    failureHostEachTimeTake = newConfig.value(Integer.class);
                    restartFailureHostCopingThread();
                } else if (SecurityGroupGlobalConfig.FAILURE_HOST_WORKER_INTERVAL.isMe(newConfig)) {
                    failureHostWorkerInterval = newConfig.value(Integer.class);
                    restartFailureHostCopingThread();
                }
            }
        };

        SecurityGroupGlobalConfig.FAILURE_HOST_WORKER_INTERVAL.installUpdateExtension(onUpdate);
        SecurityGroupGlobalConfig.FAILURE_HOST_EACH_TIME_TO_TAKE.installUpdateExtension(onUpdate);
        SecurityGroupGlobalConfig.DELAY_REFRESH_INTERVAL.installUpdateExtension(onUpdate);
    }

    public boolean start() {
        prepareGlobalConfig();
        populateExtensions();
        return true;
    }

    public boolean stop() {
        return true;
    }

    public SecurityGroupHypervisorBackend getHypervisorBackend(String hypervisorType) {
        SecurityGroupHypervisorBackend backend = hypervisorBackends.get(hypervisorType);
        if (backend == null) {
            throw new CloudRuntimeException(String.format("cannot get security group hypervisor backend[hypervisorType:%s]", hypervisorType));
        }
        return backend;
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void afterMigrateVm(final VmInstanceInventory inv, final String srcHostUuid) {
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = CollectionUtils.transformToList(inv.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        });
        cal.vmStates = asList(VmInstanceState.Running);
        List<HostRuleTO> htos = cal.calculate();
        applyRules(htos);

        SecurityGroupHypervisorBackend bkd = getHypervisorBackend(inv.getHypervisorType());
        bkd.cleanUpUnusedRuleOnHost(inv.getLastHostUuid(), new Completion(null) {
            @Override
            public void success() {
                logger.debug(String.format("vm[uuid:%s, name:%s] migrated to host[uuid:%s], cleanup its old rules on host[uuid:%s] if needed",
                        inv.getUuid(), inv.getName(), inv.getHostUuid(), srcHostUuid));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("vm[uuid:%s, name:%s] migrated to host[uuid:%s], failed to cleanup its old rules on host[uuid:%s] if needed",
                        inv.getUuid(), inv.getName(), inv.getHostUuid(), srcHostUuid));
                createFailureHostTask(inv.getLastHostUuid());
            }
        });
    }

    @Override
    public void failedToMigrateVm(final VmInstanceInventory inv, final String destHostUuid, ErrorCode reason) {
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = CollectionUtils.transformToList(inv.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        });
        cal.vmStates = asList(VmInstanceState.Unknown);
        List<HostRuleTO> htos = cal.calculate();

        logger.debug(String.format("vm[uuid:%s, name:%s] failed to migrate to host[uuid:%s], recover its rules on previous host[uuid:%s]",
                inv.getUuid(), inv.getName(), destHostUuid, inv.getHostUuid()));
        applyRules(htos);
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct struct = new ExpandedQueryStruct();
        struct.setExpandedField("securityGroupRef");
        struct.setHidden(true);
        struct.setExpandedInventoryKey("vmNicUuid");
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VmNicSecurityGroupRefInventory.class);
        struct.setInventoryClassToExpand(VmNicInventory.class);

        structs.add(struct);
        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        List<ExpandedQueryAliasStruct> aliases = new ArrayList<ExpandedQueryAliasStruct>();

        ExpandedQueryAliasStruct as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(VmNicInventory.class);
        as.setAlias("securityGroup");
        as.setExpandedField("securityGroupRef.securityGroup");
        aliases.add(as);
        return aliases;
    }

    private class FailureHostWorker implements PeriodicTask {
        @Transactional
        private List<SecurityGroupFailureHostVO> takeFailureHosts() {
            String sql = "select sgf from SecurityGroupFailureHostVO sgf, HostVO host where host.uuid = sgf.hostUuid and host.status = :hostConnectionState and sgf.managementNodeId is NULL group by sgf.hostUuid order by sgf.lastOpDate ASC";
            TypedQuery<SecurityGroupFailureHostVO> q = dbf.getEntityManager().createQuery(sql, SecurityGroupFailureHostVO.class);
            q.setLockMode(LockModeType.PESSIMISTIC_READ);
            q.setParameter("hostConnectionState", HostStatus.Connected);
            q.setMaxResults(failureHostEachTimeTake);
            List<SecurityGroupFailureHostVO> lst = q.getResultList();
            if (lst.isEmpty()) {
                return lst;
            }

            List<Long> ids = CollectionUtils.transformToList(lst, new Function<Long, SecurityGroupFailureHostVO>() {
                @Override
                public Long call(SecurityGroupFailureHostVO arg) {
                    return arg.getId();
                }
            });

            sql = "update SecurityGroupFailureHostVO f set f.managementNodeId = :mgmtId where f.id in (:ids)";
            Query uq = dbf.getEntityManager().createQuery(sql);
            uq.setParameter("mgmtId", Platform.getManagementServerId());
            uq.setParameter("ids", ids);
            uq.executeUpdate();
            return lst;
        }

        private void copeWithFailureHost(SecurityGroupFailureHostVO fvo) {
            fvo.setManagementNodeId(null);
            dbf.update(fvo);
        }

        @Override
        public void run() {
            List<SecurityGroupFailureHostVO> vos = takeFailureHosts();
            if (vos.isEmpty()) {
                return;
            }

            for (final SecurityGroupFailureHostVO vo : vos) {
                RuleCalculator cal = new RuleCalculator();
                cal.hostUuids = asList(vo.getHostUuid());
                cal.vmStates = asList(VmInstanceState.Running);
                List<HostRuleTO> htos = cal.calculate();
                if (htos.isEmpty()) {
                    logger.debug(String.format("no security rules needs to be applied to the host[uuid:%s], clean up it" +
                            " from SecurityGroupFailureHostVO", vo.getHostUuid()));
                    dbf.remove(vo);
                    continue;
                }

                final HostRuleTO hto = htos.get(0);
                hto.setRefreshHost(true);
                SecurityGroupHypervisorBackend bd = getHypervisorBackend(hto.getHypervisorType());
                bd.applyRules(hto, new Completion(null) {
                    @Override
                    public void success() {
                        logger.debug(String.format("successfully re-apply security group rules to host[uuid:%s]", hto.getHostUuid()));
                        dbf.remove(vo);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to re-apply security group rules to host[uuid:%s], because %s, try it later", hto.getHostUuid(), errorCode));
                        copeWithFailureHost(vo);
                    }
                });
            }
        }

        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.SECONDS;
        }

        @Override
        public long getInterval() {
            return failureHostWorkerInterval;
        }

        @Override
        public String getName() {
            return FailureHostWorker.class.getName();
        }
    }
}
