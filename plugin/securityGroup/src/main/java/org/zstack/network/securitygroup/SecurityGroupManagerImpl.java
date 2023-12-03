package org.zstack.network.securitygroup;

import com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
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
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.query.QueryFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.network.securitygroup.SecurityGroupMembersTO.ACTION_CODE_DELETE_GROUP;
import static org.zstack.utils.CollectionDSL.list;

public class SecurityGroupManagerImpl extends AbstractService implements SecurityGroupManager, ManagementNodeReadyExtensionPoint,
        VmInstanceMigrateExtensionPoint, AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, ValidateL3SecurityGroupExtensionPoint {
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
        Quota quota = new Quota();
        quota.defineQuota(new SecurityGroupNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateSecurityGroupMsg.class)
                .addCounterQuota(SecurityGroupQuotaConstant.SG_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(SecurityGroupVO.class)
                        .eq(SecurityGroupVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(SecurityGroupQuotaConstant.SG_NUM));

        return list(quota);
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        startFailureHostCopingThread();
    }

    @Override
    public void validateSystemtagL3SecurityGroup(String l3Uuid, List<String> securityGroupUuids) {
        for(String uuid : securityGroupUuids) {
            if (!Q.New(SecurityGroupL3NetworkRefVO.class)
                    .eq(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, l3Uuid)
                    .eq(SecurityGroupL3NetworkRefVO_.securityGroupUuid, uuid)
                    .isExists()) {
                throw new ApiMessageInterceptionException(argerr(
                        "l3NetWorkVO[uuid:%s] is not attach SecurityGroupVO[uuid:%s]", l3Uuid, uuid));
            }
        }
    }

    private class RuleCalculator {
        private List<String> vmNicUuids;
        private List<String> l3NetworkUuids;
        private List<String> securityGroupUuids;
        private List<String> hostUuids;
        private List<VmInstanceState> vmStates;
        private List<SecurityGroupState> sgStates;

        List<HostRuleTO> calculate() {
            if (sgStates == null) {
                sgStates = new ArrayList<SecurityGroupState>();
                sgStates.add(SecurityGroupState.Enabled);
            }
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
            gto.setSecurityGroupVmIps(getVmIpsBySecurityGroup(sgUuid, IPv6Constants.IPv4));
            gto.setSecurityGroupVmIp6s(getVmIpsBySecurityGroup(sgUuid, IPv6Constants.IPv6));
            gto.setSecurityGroupUuid(sgUuid);
            hto.setGroupMembersTO(gto);
            Set<String> hostUuids = new HashSet<>();

            List<Tuple> ts = SQL.New("select distinct vm.hostUuid, vm.hypervisorType" +
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
            String sql = "select nic.uuid from VmNicVO nic, VmInstanceVO vm, VmNicSecurityGroupRefVO ref, SecurityGroupVO sg" +
                    " where nic.uuid = ref.vmNicUuid and nic.vmInstanceUuid = vm.uuid"+
                    " and ref.securityGroupUuid = sg.uuid and sg.state in (:sgState)" +
                    " and vm.hostUuid in (:hostUuids) and vm.state in (:vmStates)";
            TypedQuery<String> insgQuery = dbf.getEntityManager().createQuery(sql, String.class);
            insgQuery.setParameter("hostUuids", hostUuids);
            insgQuery.setParameter("vmStates", vmStates);
            insgQuery.setParameter("sgState", sgStates);
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
                vmNicUuids = nicsInSg.stream().distinct().collect(Collectors.toList());
                ret.addAll(calculateByVmNic());
            }
            if (!nicsOutSg.isEmpty()) {
                Collection<HostRuleTO> toRemove = createRulePlaceHolder(nicsOutSg, null);
                for (HostRuleTO hto : toRemove) {
                    hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
                }
                //ret.addAll(toRemove);
                ret = mergeMultiHostRuleTO(ret, toRemove);
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
                        old.getIpv6Rules().addAll(hto.getIpv6Rules());
                    }
                }
            }

            List<HostRuleTO> ret = new ArrayList<HostRuleTO>(hostRuleTOMap.size());
            ret.addAll(hostRuleTOMap.values());
            return ret;
        }

        private List<HostRuleTO> calculateByL3NetworkAndSecurityGroup() {
            String sql = "select ref.vmNicUuid from VmNicSecurityGroupRefVO ref, SecurityGroupL3NetworkRefVO l3ref, VmNicVO nic, SecurityGroupVO sg" +
                    " where l3ref.securityGroupUuid = ref.securityGroupUuid and nic.l3NetworkUuid = l3ref.l3NetworkUuid" +
                    " and ref.securityGroupUuid in (:sgUuids) and l3ref.l3NetworkUuid in (:l3Uuids)" +
                    " and ref.securityGroupUuid = sg.uuid and sg.state in (:sgStates)";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("sgUuids", securityGroupUuids);
            q.setParameter("l3Uuids", l3NetworkUuids);
            q.setParameter("sgStates", sgStates);
            vmNicUuids = q.getResultList().stream().distinct().collect(Collectors.toList());

            return calculateByVmNic();
        }

        private List<RuleTO> calculateRuleTOBySecurityGroup(List<String> sgUuids, List<String> l3Uuids, int ipVersion) {
            List<RuleTO> ret = new ArrayList<>();

            for (String sgUuid : sgUuids) {
                if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).in(SecurityGroupVO_.state, sgStates).isExists()) {
                    continue;
                }
                List<SecurityGroupRuleVO> rules = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid)
                        .eq(SecurityGroupRuleVO_.ipVersion, ipVersion)
                        .isNull(SecurityGroupRuleVO_.remoteSecurityGroupUuid).list();
                if (rules.isEmpty()) {
                    continue;
                }

                for (SecurityGroupRuleVO r : rules) {
                    if ( r.getRemoteSecurityGroupUuid() != null) {
                        SecurityGroupVO remoteSg = Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, r.getRemoteSecurityGroupUuid()).in(SecurityGroupVO_.state, sgStates).find();
                        if (remoteSg == null) {
                            continue;
                        }
                    }
                    RuleTO rto = new RuleTO();
                    rto.setIpVersion(r.getIpVersion());
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
                sb.append(String.format("\ninput security group uuids: %s for ipv%d", sgUuids, ipVersion));
                sb.append(String.format("\nresult: %s", JSONObjectUtil.toJsonString(ret)));
                sb.append(String.format("\n-------------- end calculateRuleTOBySecurityGroupUuid ---------------------"));
                logger.trace(sb.toString());
            }

            return ret;
        }

        /* base rule is the rule with remote security group */
        private List<RuleTO> calculateSecurityGroupBaseRule(List<String> sgUuids, List<String> l3Uuids, int ipVersion){
            List<RuleTO> rules = new ArrayList<>();
            for(String sgUuid : sgUuids){
                String sql = "select r from SecurityGroupRuleVO r,SecurityGroupVO sg  where r.securityGroupUuid = :sgUuid and r.ipVersion = :ipVersion" +
                        " and r.remoteSecurityGroupUuid is not null and r.remoteSecurityGroupUuid = sg.uuid and sg.state in (:sgStates)";
                TypedQuery<SecurityGroupRuleVO> q = dbf.getEntityManager().createQuery(sql, SecurityGroupRuleVO.class);
                q.setParameter("sgUuid", sgUuid);
                q.setParameter("sgStates", sgStates);
                q.setParameter("ipVersion", ipVersion);
                List<SecurityGroupRuleVO> remoteRules = q.getResultList();

                for(SecurityGroupRuleVO r : remoteRules){
                    RuleTO rule = new RuleTO();
                    rule.setIpVersion(r.getIpVersion());
                    rule.setStartPort(r.getStartPort());
                    rule.setEndPort(r.getEndPort());
                    rule.setProtocol(r.getProtocol().toString());
                    rule.setType(r.getType().toString());
                    rule.setAllowedCidr(r.getAllowedCidr());
                    rule.setSecurityGroupUuid(sgUuid);
                    rule.setRemoteGroupUuid(r.getRemoteSecurityGroupUuid());
                    // TODO: the same group only transport once
                    rule.setRemoteGroupVmIps(getVmIpsBySecurityGroup(r.getRemoteSecurityGroupUuid(), r.getIpVersion()));
                    rules.add(rule);
                }
            }
            return rules;

        }

        private List<String> getVmIpsBySecurityGroup(String sgUuid, int ipVersion){
            List<String> ret = new ArrayList<>();
            // TODO: if two L3 network which have same ip segment attached same sg, it might has a problem
            String sql = "select ip.ip" +
                    " from VmNicVO nic, VmNicSecurityGroupRefVO ref, SecurityGroupVO sg, UsedIpVO ip" +
                    " where sg.uuid = ref.securityGroupUuid and ref.vmNicUuid = nic.uuid" +
                    " and ref.securityGroupUuid = :sgUuid" +
                    " and nic.uuid = ip.vmNicUuid and ip.ipVersion = :ipVersion";
            TypedQuery<String> internalIpQuery = dbf.getEntityManager().createQuery(sql, String.class);
            internalIpQuery.setParameter("sgUuid", sgUuid);
            internalIpQuery.setParameter("ipVersion", ipVersion);

            List<String> ips = internalIpQuery.getResultList();
            if (ips != null) {
                ret.addAll(ips);
            }

            /* add gateway address to group list */
            List<String> l3Uuids = Q.New(SecurityGroupL3NetworkRefVO.class).select(SecurityGroupL3NetworkRefVO_.l3NetworkUuid)
                    .eq(SecurityGroupL3NetworkRefVO_.securityGroupUuid, sgUuid).listValues();
            for (String uuid: l3Uuids) {
                L3NetworkInventory inv = L3NetworkInventory.valueOf(dbf.findByUuid(uuid, L3NetworkVO.class));
                List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(inv, ipVersion);
                if (!iprs.isEmpty()) {
                    ret.add(iprs.get(0).getGateway());
                }
            }

            for (SecurityGroupGetDefaultRuleExtensionPoint exp : pluginRgty.getExtensionList(SecurityGroupGetDefaultRuleExtensionPoint.class)) {
                ret.addAll(exp.getGroupMembers(sgUuid, ipVersion));
            }

            return ret;
        }

        /* calculate the default rules for nics which are not bound to sg */
        @Transactional(readOnly = true)
        Collection<HostRuleTO> createRulePlaceHolder(List<String> nicUuids, Integer ipVersion) {
            List<Tuple> tuples;
            if (ipVersion != null) {
                /* there are multiple ips on same nic */
                String sql = "select nic.uuid, vm.hostUuid, vm.hypervisorType, nic.internalName, nic.mac, ip.ip, ip.ipVersion" +
                        " from VmInstanceVO vm, VmNicVO nic, UsedIpVO ip" +
                        " where nic.vmInstanceUuid = vm.uuid and vm.hostUuid is not null and nic.uuid in (:nicUuids)" +
                        " and ip.vmNicUuid = nic.uuid and ip.ipVersion = :ipversion";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("nicUuids", nicUuids).setParameter("ipversion", ipVersion);
                tuples = q.getResultList();

                sql = "select nic.uuid, vm.lastHostUuid, vm.hypervisorType, nic.internalName, nic.mac, ip.ip, ip.ipVersion" +
                        " from VmInstanceVO vm, VmNicVO nic, UsedIpVO ip" +
                        " where nic.vmInstanceUuid = vm.uuid and vm.hostUuid is null and vm.lastHostUuid is not null" +
                        " and nic.uuid in (:nicUuids) and ip.vmNicUuid = nic.uuid and ip.ipVersion = :ipversion";
                q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("nicUuids", nicUuids).setParameter("ipversion", ipVersion);
                tuples.addAll(q.getResultList());
            } else {
                /* there are multiple ips on same nic */
                String sql = "select nic.uuid, vm.hostUuid, vm.hypervisorType, nic.internalName, nic.mac, ip.ip, ip.ipVersion" +
                        " from VmInstanceVO vm, VmNicVO nic, UsedIpVO ip" +
                        " where nic.vmInstanceUuid = vm.uuid and vm.hostUuid is not null and nic.uuid in (:nicUuids)" +
                        " and ip.vmNicUuid = nic.uuid";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("nicUuids", nicUuids);
                tuples = q.getResultList();

                sql = "select nic.uuid, vm.lastHostUuid, vm.hypervisorType, nic.internalName, nic.mac, ip.ip, ip.ipVersion" +
                        " from VmInstanceVO vm, VmNicVO nic, UsedIpVO ip" +
                        " where nic.vmInstanceUuid = vm.uuid and vm.hostUuid is null and vm.lastHostUuid is not null" +
                        " and nic.uuid in (:nicUuids) and ip.vmNicUuid = nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("nicUuids", nicUuids);
                tuples.addAll(q.getResultList());
            }

            Map<String, HostRuleTO> hostRuleTOMap = new HashMap<String, HostRuleTO>();
            for (Tuple t : tuples) {
                String nicUuid = t.get(0, String.class);
                String hostUuid = t.get(1, String.class);
                String hvType = t.get(2, String.class);
                String nicName = t.get(3, String.class);
                String mac = t.get(4, String.class);
                String ip = t.get(5, String.class);
                Integer version = t.get(6, Integer.class);

                HostRuleTO hto = hostRuleTOMap.get(hostUuid);
                if (hto == null) {
                    hto = new HostRuleTO();
                    hto.setHostUuid(hostUuid);
                    hto.setHypervisorType(hvType);
                    hostRuleTOMap.put(hto.getHostUuid(), hto);
                }

                Optional<SecurityGroupRuleTO> sgRule;
                if (version == IPv6Constants.IPv4) {
                    sgRule = hto.getRules().stream().filter(r -> r.getVmNicUuid().equals(nicUuid)).findFirst();
                } else {
                    sgRule = hto.getIpv6Rules().stream().filter(r -> r.getVmNicUuid().equals(nicUuid)).findFirst();
                }

                if (sgRule.isPresent()) {
                    sgRule.get().getVmNicIp().add(ip);
                } else {
                    SecurityGroupRuleTO sgto = new SecurityGroupRuleTO();
                    sgto.setEgressDefaultPolicy(SecurityGroupGlobalConfig.EGRESS_RULE_DEFAULT_POLICY.value(String.class));
                    sgto.setIngressDefaultPolicy(SecurityGroupGlobalConfig.INGRESS_RULE_DEFAULT_POLICY.value(String.class));
                    sgto.setRules(new ArrayList<RuleTO>());
                    sgto.setSecurityGroupBaseRules(new ArrayList<RuleTO>());
                    sgto.setVmNicUuid(nicUuid);
                    sgto.setVmNicInternalName(nicName);
                    sgto.setVmNicMac(mac);
                    sgto.setVmNicIp(new ArrayList<>());
                    sgto.getVmNicIp().add(ip);
                    if (version == IPv6Constants.IPv4) {
                        hto.getRules().add(sgto);
                    } else {
                        hto.getIpv6Rules().add(sgto);
                    }
                }
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
                    String sql = "select ref.securityGroupUuid, vm.hostUuid, vm.hypervisorType, nic.internalName, ip.l3NetworkUuid, nic.mac, ip.ip, ip.ipVersion" +
                                     " from VmNicSecurityGroupRefVO ref, VmInstanceVO vm, VmNicVO nic, SecurityGroupVO sg, UsedIpVO ip" +
                                     " where ref.vmNicUuid = nic.uuid and nic.vmInstanceUuid = vm.uuid and ref.vmNicUuid = :nicUuid " +
                                     " and vm.state in (:vmStates) and ref.securityGroupUuid = sg.uuid and sg.state in (:sgStates) " +
                                     " and nic.uuid = ip.vmNicUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("nicUuid", nicUuid);
                    q.setParameter("vmStates", vmStates);
                    q.setParameter("sgStates", sgStates);
                    tuples = q.getResultList();
                } else {
                    String sql = "select ref.securityGroupUuid, vm.hostUuid, vm.hypervisorType, nic.internalName, ip.l3NetworkUuid, nic.mac, ip.ip, ip.ipVersion" +
                            " from VmNicSecurityGroupRefVO ref, VmInstanceVO vm, VmNicVO nic, SecurityGroupVO sg, UsedIpVO ip" +
                            " where ref.vmNicUuid = nic.uuid and nic.vmInstanceUuid = vm.uuid and ref.vmNicUuid = :nicUuid " +
                            " and ref.securityGroupUuid = sg.uuid and sg.state in (:sgStates) and nic.uuid = ip.vmNicUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("nicUuid", nicUuid);
                    q.setParameter("sgStates", sgStates);
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
                List<String> l3Uuids = new ArrayList<String>();
                String mac = null;
                List<String> ips = new ArrayList<String>();
                List<String> ip6s = new ArrayList<String>();
                for (Tuple t : tuples) {
                    sgUuids.add(t.get(0, String.class));
                    hostUuid = t.get(1, String.class);
                    hypervisorType = t.get(2, String.class);
                    nicName = t.get(3, String.class);
                    l3Uuids.add(t.get(4, String.class));
                    mac = t.get(5, String.class);
                    Integer version = t.get(7, Integer.class);
                    if (version == IPv6Constants.IPv4) {
                        ips.add(t.get(6, String.class));
                    } else {
                        ip6s.add(t.get(6, String.class));
                    }
                }

                /* calculate all sg rules for a single nic, including ipv4 rules and ipv6 rules */
                sgUuids = sgUuids.stream().distinct().collect(Collectors.toList());
                l3Uuids = l3Uuids.stream().distinct().collect(Collectors.toList());
                if (!sgUuids.isEmpty()) {
                    HostRuleTO hto = hostRuleMap.get(hostUuid);
                    if (hto == null) {
                        hto = new HostRuleTO();
                        hto.setHostUuid(hostUuid);
                        hto.setHypervisorType(hypervisorType);
                        hostRuleMap.put(hto.getHostUuid(), hto);
                    }

                    if (!ips.isEmpty()) {
                        List<RuleTO> rtos = calculateRuleTOBySecurityGroup(sgUuids, l3Uuids, IPv6Constants.IPv4);
                        List<RuleTO> securityGroupBaseRules = calculateSecurityGroupBaseRule(sgUuids, l3Uuids, IPv6Constants.IPv4);
                        SecurityGroupRuleTO sgto = new SecurityGroupRuleTO();
                        sgto.setEgressDefaultPolicy(SecurityGroupGlobalConfig.EGRESS_RULE_DEFAULT_POLICY.value(String.class));
                        sgto.setIngressDefaultPolicy(SecurityGroupGlobalConfig.INGRESS_RULE_DEFAULT_POLICY.value(String.class));
                        sgto.setRules(rtos);
                        sgto.setVmNicUuid(nicUuid);
                        sgto.setVmNicInternalName(nicName);
                        sgto.setVmNicMac(mac);
                        sgto.setVmNicIp(ips);
                        sgto.setSecurityGroupBaseRules(securityGroupBaseRules);
                        sgto.setIpVersion(IPv6Constants.IPv4);
                        hto.getRules().add(sgto);
                    }

                    /* caculate ipv6 rules */
                    if (!ip6s.isEmpty()) {
                        List<RuleTO> rtos6 = calculateRuleTOBySecurityGroup(sgUuids, l3Uuids, IPv6Constants.IPv6);
                        List<RuleTO> securityGroupBaseRules6 = calculateSecurityGroupBaseRule(sgUuids, l3Uuids, IPv6Constants.IPv6);
                        SecurityGroupRuleTO sgto6 = new SecurityGroupRuleTO();
                        sgto6.setEgressDefaultPolicy(SecurityGroupGlobalConfig.EGRESS_RULE_DEFAULT_POLICY.value(String.class));
                        sgto6.setIngressDefaultPolicy(SecurityGroupGlobalConfig.INGRESS_RULE_DEFAULT_POLICY.value(String.class));
                        sgto6.setRules(rtos6);
                        sgto6.setVmNicUuid(nicUuid);
                        sgto6.setVmNicInternalName(nicName);
                        sgto6.setVmNicMac(mac);
                        sgto6.setVmNicIp(ip6s);
                        sgto6.setSecurityGroupBaseRules(securityGroupBaseRules6);
                        sgto6.setIpVersion(IPv6Constants.IPv6);

                        hto.getIpv6Rules().add(sgto6);
                    }
                }
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
        } else if (msg instanceof AddSecurityGroupRuleMsg) {
            handle((AddSecurityGroupRuleMsg) msg);
        } else if (msg instanceof CreateSecurityGroupMsg) {
            handle((CreateSecurityGroupMsg) msg);
        } else if (msg instanceof RefreshSecurityGroupRulesOnVmMsg) {
            handle((RefreshSecurityGroupRulesOnVmMsg) msg);
        } else if (msg instanceof RemoveVmNicFromSecurityGroupMsg) {
            handle((RemoveVmNicFromSecurityGroupMsg) msg);
        } else if (msg instanceof SecurityGroupDeletionMsg) {
            handle((SecurityGroupDeletionMsg) msg);
        } else if (msg instanceof AddVmNicToSecurityGroupMsg) {
            handle((AddVmNicToSecurityGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CreateSecurityGroupMsg msg) {
        CreateSecurityGroupReply reply = new CreateSecurityGroupReply();
        SecurityGroupVO vo = new SecurityGroupVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(SecurityGroupState.Enabled);
        vo.setInternalId(dbf.generateSequenceNumber(SecurityGroupSequenceNumberVO.class));
        vo.setAccountUuid(msg.getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        createDefaultRule(vo.getUuid(), IPv6Constants.IPv4);
        createDefaultRule(vo.getUuid(), IPv6Constants.IPv6);

        reply.setInventory(SecurityGroupInventory.valueOf(vo));
        bus.reply(msg, reply);
    }

    private void handle(AddSecurityGroupRuleMsg msg) {
        AddSecurityGroupRuleReply reply = new AddSecurityGroupRuleReply();
        SecurityGroupVO securityGroupVO = addRuleToSecurityGroup(msg);
        reply.setInventory(SecurityGroupInventory.valueOf(securityGroupVO));
        bus.reply(msg, reply);
    }

    private void handle(SecurityGroupDeletionMsg msg) {
        SecurityGroupDeletionReply reply = new SecurityGroupDeletionReply();
        dbf.removeByPrimaryKey(msg.getUuid(), SecurityGroupVO.class);
        bus.reply(msg, reply);
    }

    private void handle(RemoveVmNicFromSecurityGroupMsg msg) {
        RemoveVmNicFromSecurityGroupReply reply = new RemoveVmNicFromSecurityGroupReply();
        removeNicFromSecurityGroup(msg.getSecurityGroupUuid(), msg.getVmNicUuids());
        bus.reply(msg, reply);
    }

    private void handle(RefreshSecurityGroupRulesOnVmMsg msg) {
        RefreshSecurityGroupRulesOnVmReply reply = new RefreshSecurityGroupRulesOnVmReply();
        List<String> nicUuids = msg.getNicUuids();
        if (nicUuids == null || nicUuids.isEmpty()) {
            SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
            q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
            q.add(VmNicSecurityGroupRefVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
            nicUuids = q.listValue();
        }

        if (nicUuids.isEmpty()) {
            checkDefaultRulesOnHost(msg.getHostUuid());
            logger.debug(String.format("no nic of vm[uuid:%s] needs to refresh security group rule", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        nicUuids = nicUuids.stream().distinct().collect(Collectors.toList());
        Collection<HostRuleTO> htos;
        RuleCalculator cal = new RuleCalculator();
        if (msg.isDeleteAllRules()) {
            htos = cal.createRulePlaceHolder(nicUuids, null);
            for (HostRuleTO hto : htos) {
                hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
            }
        } else {
            cal.vmNicUuids = nicUuids;
            htos = cal.calculate();
        }

        for (HostRuleTO hto : htos) {
            if (hto.getHostUuid() == null) {
                hto.setHostUuid(msg.getHostUuid());
            }
        }

        applyRules(htos);

        if (msg.getSgUuids() != null && !msg.getSgUuids().isEmpty()) {
            Q.New(SecurityGroupVO.class)
                .select(SecurityGroupVO_.uuid).in(SecurityGroupVO_.uuid, msg.getSgUuids())
                .eq(SecurityGroupVO_.state, SecurityGroupState.Enabled).listValues().forEach(sgUuid -> {
                    HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember((String) sgUuid);
                    if (!groupMemberTO.getHostUuids().isEmpty()) {
                        updateGroupMembers(groupMemberTO);
                    }
                });
        }

        if (htos.isEmpty()) {
            checkDefaultRulesOnHost(msg.getHostUuid());
        }

        logger.debug(String.format("refreshed security group rule for vm[uuid:%s] vNicuuids[%s]",
                msg.getVmInstanceUuid(), Joiner.on(",").join(nicUuids)));
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
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref" +
                        " where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
                        " and sg.uuid = :sgUuid and vm.type = :vmType and vm.state in (:vmStates) and nic.uuid not in (:nicUuids) group by nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
                q.setParameter("nicUuids", nicUuidsToExclued);
            }
        } else {
            // accessed by a normal account
            if (nicUuidsToExclued.isEmpty()) {
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref" +
                        " where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
                        " and sg.uuid = :sgUuid and vm.type = :vmType and vm.state in (:vmStates) and nic.uuid in (:iuuids) group by nic.uuid";
                q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
                q.setParameter("iuuids", nicUuidsToInclude);
            } else {
                sql = "select nic from VmNicVO nic, VmInstanceVO vm, SecurityGroupVO sg, SecurityGroupL3NetworkRefVO ref" +
                        " where nic.vmInstanceUuid = vm.uuid and nic.l3NetworkUuid = ref.l3NetworkUuid and ref.securityGroupUuid = sg.uuid " +
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
        String sql = "select distinct ref.uuid from VmNicSecurityGroupRefVO ref, VmNicVO nic, SecurityGroupVO sg" +
                " where nic.uuid = ref.vmNicUuid and nic.l3NetworkUuid = :l3Uuid and ref.securityGroupUuid = :sgUuid";
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
        String sql = "select distinct nic.uuid from VmNicVO nic, VmNicSecurityGroupRefVO ref, SecurityGroupVO sg" +
                " where ref.vmNicUuid = nic.uuid and nic.l3NetworkUuid = :l3Uuid and ref.securityGroupUuid = :sgUuid";
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
            List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getUuid()).list();
            for (SecurityGroupRuleVO rvo : rvos) {
                rvo.setState(SecurityGroupRuleState.Enabled);
            }
            dbf.updateCollection(rvos);
            vo = dbf.updateAndRefresh(vo);

            List<String> sgUuids = Q.New(SecurityGroupRuleVO.class).select(SecurityGroupRuleVO_.securityGroupUuid).eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, msg.getUuid()).listValues();
            sgUuids.add(msg.getUuid());
            RuleCalculator cal = new RuleCalculator();
            cal.securityGroupUuids = sgUuids;
            cal.vmStates = asList(VmInstanceState.Running);
            List<HostRuleTO> htos = cal.calculate();

            applyRules(htos);
            HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(msg.getUuid());
            if (!groupMemberTO.getHostUuids().isEmpty()) {
                updateGroupMembers(groupMemberTO);
            }
        } else {
            List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getUuid()).list();
            for (SecurityGroupRuleVO rvo : rvos) {
                rvo.setState(SecurityGroupRuleState.Disabled);
            }
            dbf.updateCollection(rvos);

            vo.setState(SecurityGroupState.Disabled);
            vo = dbf.updateAndRefresh(vo);

            disableSecurityGroup(msg.getUuid());
        }

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

    private void removeNicFromSecurityGroup(String sgUuid, List<String> vmNicUuids) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, sgUuid);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, vmNicUuids);
        List<VmNicSecurityGroupRefVO> refVOs = q.list();

        dbf.removeCollection(refVOs, VmNicSecurityGroupRefVO.class);

        SecurityGroupVO sgvo = dbf.findByUuid(sgUuid, SecurityGroupVO.class);
        if (SecurityGroupState.Disabled == sgvo.getState()) {
            return;
        }

        // nics may be in other security group
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = vmNicUuids;
        List<HostRuleTO> htos1 = cal.calculate();

        // create deleting chain action for nics no longer in any security group
        List<String> nicUuidsIn = SQL.New("select ref.vmNicUuid from VmNicSecurityGroupRefVO ref, SecurityGroupVO sg" +
                " where ref.securityGroupUuid = sg.uuid and sg.state = :sgState", String.class)
                .param("sgState", SecurityGroupState.Enabled).list();
        List<String> nicsUuidsCopy = new ArrayList<String>();
        nicsUuidsCopy.addAll(vmNicUuids);
        nicsUuidsCopy.removeAll(nicUuidsIn);
        if (!nicsUuidsCopy.isEmpty()) {
            Collection<HostRuleTO> toDeleted = cal.createRulePlaceHolder(nicsUuidsCopy, IPv6Constants.IPv4);
            for (HostRuleTO hto : toDeleted) {
                hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
            }
            htos1 = cal.mergeMultiHostRuleTO(htos1, toDeleted);

            toDeleted = cal.createRulePlaceHolder(nicsUuidsCopy, IPv6Constants.IPv6);
            for (HostRuleTO hto : toDeleted) {
                hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
            }
            htos1 = cal.mergeMultiHostRuleTO(htos1, toDeleted);
        }

        List<HostRuleTO> finalHtos = htos1;

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

    private void disableSecurityGroup(String uuid) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, uuid);
        List<String> vmNicUuids = q.listValue();

        RuleCalculator cal = new RuleCalculator();
        SecurityGroupVO sgvo = dbf.findByUuid(uuid, SecurityGroupVO.class);

        if (!vmNicUuids.isEmpty()) {
            cal.vmNicUuids = vmNicUuids;
            cal.vmStates = asList(VmInstanceState.Running);
            /* if vmnics are still in other sg, calcalue the rules */
            List<HostRuleTO> htos = cal.calculate();

            List<String> nicUuidsIn = SQL.New("select ref.vmNicUuid from VmNicSecurityGroupRefVO ref, SecurityGroupVO sg" +
                    " where ref.vmNicUuid in (:vmNicUuids) and ref.securityGroupUuid = sg.uuid and sg.state = :sgState", String.class)
                    .param("vmNicUuids", vmNicUuids).param("sgState", SecurityGroupState.Enabled).list();

            vmNicUuids.removeAll(nicUuidsIn);
            if (!vmNicUuids.isEmpty()) {
                // these vm nics are no longer in any security group, delete their chains on host
                Collection<HostRuleTO> toRemove = cal.createRulePlaceHolder(vmNicUuids, IPv6Constants.IPv4);
                for (HostRuleTO hto : toRemove) {
                    hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
                }

                htos = cal.mergeMultiHostRuleTO(htos, toRemove);

                toRemove = cal.createRulePlaceHolder(vmNicUuids, IPv6Constants.IPv6);
                for (HostRuleTO hto : toRemove) {
                    hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
                }
                htos = cal.mergeMultiHostRuleTO(htos, toRemove);
            }
            applyRules(htos);
        }

        List<String> sgUuids = Q.New(SecurityGroupRuleVO.class).select(SecurityGroupRuleVO_.securityGroupUuid).eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, uuid).listValues();
        sgUuids = sgUuids.stream().distinct().collect(Collectors.toList());
        sgUuids.remove(uuid);
        if (!sgUuids.isEmpty()) {
            RuleCalculator rcal = new RuleCalculator();
            rcal.securityGroupUuids = sgUuids;
            rcal.vmStates = asList(VmInstanceState.Running);
            List<HostRuleTO> rhtos = rcal.calculate();
            applyRules(rhtos);
        }

        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(uuid);
        if(!groupMemberTO.getHostUuids().isEmpty()){
            groupMemberTO.getGroupMembersTO().setActionCode(ACTION_CODE_DELETE_GROUP);
            updateGroupMembers(groupMemberTO);
        }
    }

    private void handle(APIDeleteSecurityGroupMsg msg) {
        SecurityGroupVO sgVo = dbf.findByUuid(msg.getUuid(), SecurityGroupVO.class);
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

            List<String> nicUuidsIn = SQL.New("select ref.vmNicUuid from VmNicSecurityGroupRefVO ref, SecurityGroupVO sg " +
                    " where ref.vmNicUuid in (:vmNicUuids) and ref.securityGroupUuid = sg.uuid and sg.state = :sgState", String.class)
                    .param("vmNicUuids", vmNicUuids).param("sgState", SecurityGroupState.Enabled).list();

            vmNicUuids.removeAll(nicUuidsIn);
            if (!vmNicUuids.isEmpty()) {
                // these vm nics are no longer in any security group, delete their chains on host
                Collection<HostRuleTO> toRemove = cal.createRulePlaceHolder(vmNicUuids, IPv6Constants.IPv4);
                for (HostRuleTO hto : toRemove) {
                    hto.setActionCodeForAllSecurityGroupRuleTOs(SecurityGroupRuleTO.ACTION_CODE_DELETE_CHAIN);
                }
                htos = cal.mergeMultiHostRuleTO(htos, toRemove);

                toRemove = cal.createRulePlaceHolder(vmNicUuids, IPv6Constants.IPv6);
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
        SecurityGroupVO sgvo = dbf.findByUuid(sgUuid, SecurityGroupVO.class);

        if (SecurityGroupState.Enabled == sgvo.getState()) {
            RuleCalculator cal = new RuleCalculator();
            cal.securityGroupUuids = asList(sgUuid);
            cal.vmStates = asList(VmInstanceState.Running);

            List<HostRuleTO> htos = cal.calculate();
            applyRules(htos);
        }

        APIDeleteSecurityGroupRuleEvent evt = new APIDeleteSecurityGroupRuleEvent(msg.getId());
        evt.setInventory(SecurityGroupInventory.valueOf(sgvo));
        bus.publish(evt);
    }

    private void validate(AddVmNicToSecurityGroupMsg msg) {
        String securityGroupUuid = msg.getSecurityGroupUuid();
        List<String> uuids = Q.New(VmNicVO.class)
                .select(VmNicVO_.uuid)
                .in(VmNicVO_.uuid, msg.getVmNicUuids())
                .listValues();
        if (!uuids.containsAll(msg.getVmNicUuids())) {
            msg.getVmNicUuids().removeAll(uuids);
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find vm nics[uuids:%s]", msg.getVmNicUuids()
            ));
        }

        List<String> nicUuids = SQL.New("select nic.uuid from SecurityGroupL3NetworkRefVO ref, VmNicVO nic, UsedIpVO ip" +
                " where ref.l3NetworkUuid = ip.l3NetworkUuid and ip.vmNicUuid = nic.uuid" +
                " and ref.securityGroupUuid = :sgUuid and nic.uuid in (:nicUuids)")
                .param("nicUuids", uuids)
                .param("sgUuid", securityGroupUuid)
                .list();

        List<String> wrongUuids = new ArrayList<>();
        for (String uuid : uuids) {
            if (!nicUuids.contains(uuid)) {
                wrongUuids.add(uuid);
            }
        }

        if (!wrongUuids.isEmpty()) {
            throw new OperationFailureException(argerr("VM nics[uuids:%s] are not on L3 networks that have been attached to the security group[uuid:%s]",
                    wrongUuids, securityGroupUuid));
        }

        msg.setVmNicUuids(uuids);
    }

    private void handle(AddVmNicToSecurityGroupMsg msg) {
        AddVmNicToSecurityGroupReply reply = new AddVmNicToSecurityGroupReply();

        validate(msg);

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

        SecurityGroupVO sgvo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
        if (SecurityGroupState.Enabled == sgvo.getState()) {
            RuleCalculator cal = new RuleCalculator();
            if (triggerApplyRules) {
                cal.vmNicUuids = msg.getVmNicUuids();
                List<HostRuleTO> htos = cal.calculate();
                applyRules(htos);
            }

            HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(msg.getSecurityGroupUuid());
            if (!groupMemberTO.getHostUuids().isEmpty()) {
                updateGroupMembers(groupMemberTO);
            }
        }

        logger.debug(String.format("successfully added vm nics%s to security group[uuid:%s]", msg.getVmNicUuids(), msg.getSecurityGroupUuid()));
        bus.reply(msg, reply);
    }

    private void handle(final APIAddVmNicToSecurityGroupMsg msg) {
        APIAddVmNicToSecurityGroupEvent evt = new APIAddVmNicToSecurityGroupEvent(msg.getId());

        AddVmNicToSecurityGroupMsg addVmNicToSecurityGroupMsg = new AddVmNicToSecurityGroupMsg();
        addVmNicToSecurityGroupMsg.setSecurityGroupUuid(msg.getSecurityGroupUuid());
        addVmNicToSecurityGroupMsg.setVmNicUuids(msg.getVmNicUuids());
        bus.makeTargetServiceIdByResourceUuid(addVmNicToSecurityGroupMsg, SecurityGroupConstant.SERVICE_ID, msg.getSecurityGroupUuid());

        bus.send(addVmNicToSecurityGroupMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
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

    private void checkDefaultRulesOnHost(String hostUuid) {
        String hypervisorType = Q.New(HostVO.class).select(HostVO_.hypervisorType).eq(HostVO_.uuid, hostUuid).findValue();
        SecurityGroupHypervisorBackend bkend = hypervisorBackends.get(hypervisorType);
        bkend.checkDefaultRules(hostUuid, new Completion(null) {
            private void copeWithFailureHost() {
                createFailureHostTask(hostUuid);
            }

            @Override
            public void success() {
                logger.debug(String.format("successfully applied security rules on host[uuid:%s]", hostUuid));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("failed to apply security rules on host[uuid:%s], because %s, will try it later", hostUuid, errorCode));
                copeWithFailureHost();
            }
        });
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
        SecurityGroupVO securityGroupVO = addRuleToSecurityGroup(msg);
        evt.setInventory(SecurityGroupInventory.valueOf(securityGroupVO));
        logger.debug(String.format("successfully add rules to security group[uuid:%s, name:%s]:\n%s", securityGroupVO.getUuid(), securityGroupVO.getName(), JSONObjectUtil.toJsonString(msg.getRules())));
        bus.publish(evt);
    }

    private SecurityGroupVO addRuleToSecurityGroup(AddSecurityGroupRuleMessage message) {
        SecurityGroupVO sgvo = dbf.findByUuid(message.getSecurityGroupUuid(), SecurityGroupVO.class);

        List<SecurityGroupRuleVO> vos = new ArrayList<SecurityGroupRuleVO>();
        for (SecurityGroupRuleAO ao : message.getRules()) {
            if (message.getRemoteSecurityGroupUuids() != null) {
                for (String remoteGroupUuid : message.getRemoteSecurityGroupUuids()) {
                    SecurityGroupRuleVO vo = new SecurityGroupRuleVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setIpVersion(ao.getIpVersion());
                    vo.setAllowedCidr(ao.getAllowedCidr());
                    vo.setEndPort(ao.getEndPort());
                    vo.setStartPort(ao.getStartPort());
                    vo.setProtocol(SecurityGroupRuleProtocolType.valueOf(ao.getProtocol()));
                    vo.setType(SecurityGroupRuleType.valueOf(ao.getType()));
                    vo.setSecurityGroupUuid(message.getSecurityGroupUuid());
                    vo.setRemoteSecurityGroupUuid(remoteGroupUuid);
                    if (SecurityGroupState.Disabled == sgvo.getState()) {
                        vo.setState(SecurityGroupRuleState.Disabled);
                    }
                    vos.add(vo);
                }
            } else {
                SecurityGroupRuleVO vo = new SecurityGroupRuleVO();
                vo.setUuid(Platform.getUuid());
                vo.setIpVersion(ao.getIpVersion());
                vo.setAllowedCidr(ao.getAllowedCidr());
                vo.setEndPort(ao.getEndPort());
                vo.setStartPort(ao.getStartPort());
                vo.setProtocol(SecurityGroupRuleProtocolType.valueOf(ao.getProtocol()));
                vo.setType(SecurityGroupRuleType.valueOf(ao.getType()));
                vo.setSecurityGroupUuid(message.getSecurityGroupUuid());
                if (SecurityGroupState.Disabled == sgvo.getState()) {
                    vo.setState(SecurityGroupRuleState.Disabled);
                }
                vos.add(vo);
            }
        }
        dbf.persistCollection(vos);

        if (SecurityGroupState.Enabled == sgvo.getState()) {
            RuleCalculator cal = new RuleCalculator();
            cal.securityGroupUuids = asList(message.getSecurityGroupUuid());
            cal.vmStates = asList(VmInstanceState.Running);
            List<HostRuleTO> htos = cal.calculate();
            applyRules(htos);
        }

        return dbf.reload(sgvo);
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
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        SecurityGroupVO finalVo = vo;
        vo = new SQLBatchWithReturn<SecurityGroupVO>() {
            @Override
            protected SecurityGroupVO scripts() {
                persist(finalVo);
                reload(finalVo);
                tagMgr.createTagsFromAPICreateMessage(msg, finalVo.getUuid(), SecurityGroupVO.class.getSimpleName());
                return finalVo;
            }
        }.execute();

        createDefaultRule(finalVo.getUuid(), IPv6Constants.IPv4);
        createDefaultRule(finalVo.getUuid(), IPv6Constants.IPv6);
        vo = dbf.reload(vo);

        SecurityGroupInventory inv = SecurityGroupInventory.valueOf(vo);
        APICreateSecurityGroupEvent evt = new APICreateSecurityGroupEvent(msg.getId());
        evt.setInventory(inv);
        logger.debug(String.format("successfully created security group[uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
        bus.publish(evt);
    }

    public String getId() {
        return bus.makeLocalServiceId(SecurityGroupConstant.SERVICE_ID);
    }

    private void createDefaultRule(String sgUuid, int ipVersion){
        List<SecurityGroupRuleVO> vos = new ArrayList<>();

        SecurityGroupRuleVO ingressRuleVo = new SecurityGroupRuleVO();
        ingressRuleVo.setUuid(Platform.getUuid());
        ingressRuleVo.setIpVersion(ipVersion);
        if (ipVersion == IPv6Constants.IPv4) {
            ingressRuleVo.setAllowedCidr("0.0.0.0/0");
        } else {
            ingressRuleVo.setAllowedCidr("::/0");
        }
        ingressRuleVo.setEndPort(-1);
        ingressRuleVo.setStartPort(-1);
        ingressRuleVo.setProtocol(SecurityGroupRuleProtocolType.ALL);
        ingressRuleVo.setType(SecurityGroupRuleType.Ingress);
        ingressRuleVo.setSecurityGroupUuid(sgUuid);
        ingressRuleVo.setRemoteSecurityGroupUuid(sgUuid);
        vos.add(ingressRuleVo);

        SecurityGroupRuleVO egressRuleVo = new SecurityGroupRuleVO();
        egressRuleVo.setUuid(Platform.getUuid());
        egressRuleVo.setIpVersion(ipVersion);
        if (ipVersion == IPv6Constants.IPv4) {
            egressRuleVo.setAllowedCidr("0.0.0.0/0");
        } else {
            egressRuleVo.setAllowedCidr("::/0");
        }
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
        RuleCalculator cal = new RuleCalculator();
        cal.vmStates = asList(VmInstanceState.Migrating);
        cal.vmNicUuids = CollectionUtils.transformToList(inv.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        });
        List<HostRuleTO> htos = cal.calculate();
        if (htos.isEmpty()) {
            return;
        }

        final HostRuleTO hto = htos.get(0);
        SecurityGroupHypervisorBackend bkd = getHypervisorBackend(inv.getHypervisorType());
        bkd.applyRules(hto, new Completion(null) {
            @Override
            public void success() {
                logger.debug(String.format("vm[uuid:%s, name:%s] migrated to host[uuid:%s], successfully apply security group rules",  inv.getUuid(), inv.getName(), destHostUuid));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("failed to apply security group rules to host[uuid:%s], because %s, try it later", destHostUuid, errorCode));
                createFailureHostTask(destHostUuid);
            }
        });
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

        // check default rules when no rules to apply
        if (htos.isEmpty()) {
            checkDefaultRulesOnHost(inv.getHostUuid());
        }

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
        if (destHostUuid == null) {
            return;
        }

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
