package org.zstack.network.securitygroup;

import com.google.common.base.Joiner;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import static org.zstack.core.Platform.operr;

import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.SdnControllerDeleteExtensionPoint;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.vm.AddVmNicToSecurityGroupMsg;
import org.zstack.header.vm.AddVmNicToSecurityGroupReply;
import org.zstack.header.vm.ValidateL3SecurityGroupExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.l3.L3NetworkHelper;
import org.zstack.network.securitygroup.APIUpdateSecurityGroupRulePriorityMsg.SecurityGroupRulePriorityAO;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO;
import org.zstack.query.QueryFacade;
import org.zstack.tag.SystemTagCreator;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.network.securitygroup.SecurityGroupConstant.Param.*;
import static org.zstack.network.securitygroup.SecurityGroupMembersTO.ACTION_CODE_DELETE_GROUP;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class SecurityGroupManagerImpl extends AbstractService implements SecurityGroupManager, ManagementNodeReadyExtensionPoint,
        VmInstanceMigrateExtensionPoint, AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, ValidateL3SecurityGroupExtensionPoint,
        SdnControllerDeleteExtensionPoint {
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
    @Autowired
    protected CascadeFacade casf;

    protected Map<String, SecurityGroupHypervisorBackend> hypervisorBackends;
    private int failureHostWorkerInterval;
    private int failureHostEachTimeTake;
    private Future<Void> failureHostCopingThread;

    private String getSecurityGroupSyncThreadName(String securityGroupUuid) {
        return String.format("SecurityGroup-%s", securityGroupUuid);
    }

    private String getVmNicSecurityGroupRefSyncThreadName() {
        return String.format("SecurityGroup-VmNicSecurityGroupRef");
    }

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
        if (!Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3Uuid)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE).isExists()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10124, "the netwotk service[type:%s] not enabled on the l3Network[uuid:%s]", SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE, l3Uuid));
        }
    }

    private List<String> getVmIpsBySecurityGroup(String sgUuid, int ipVersion){
        List<String> ret = new ArrayList<>();
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
        List<String> attachedL3Uuids = Q.New(SecurityGroupL3NetworkRefVO.class).select(SecurityGroupL3NetworkRefVO_.l3NetworkUuid)
                .eq(SecurityGroupL3NetworkRefVO_.securityGroupUuid, sgUuid).listValues();

        List<String> nicL3Uuids = SQL.New("select distinct l3.uuid from VmNicVO nic, VmNicSecurityGroupRefVO ref, L3NetworkVO l3" +
                        " where ref.securityGroupUuid = :sgUuid" +
                        " and ref.vmNicUuid = nic.uuid" +
                        " and l3.uuid = nic.l3NetworkUuid", String.class)
                .param("sgUuid", sgUuid)
                .list();

        List<String> resultL3Uuids = Stream.concat(attachedL3Uuids.stream(), nicL3Uuids.stream()).distinct().collect(Collectors.toList());

        for (String uuid: resultL3Uuids) {
            L3NetworkInventory inv = L3NetworkInventory.valueOf(dbf.findByUuid(uuid, L3NetworkVO.class));
            List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(inv, ipVersion);
            if (!iprs.isEmpty()) {
                ret.add(iprs.get(0).getGateway());
            }
        }

        for (SecurityGroupGetDefaultRuleExtensionPoint exp : pluginRgty.getExtensionList(SecurityGroupGetDefaultRuleExtensionPoint.class)) {
            ret.addAll(exp.getGroupMembers(sgUuid, ipVersion));
        }

        return ret.stream().distinct().collect(Collectors.toList());
    }

    private class RuleCalculator {
        private List<String> vmNicUuids;
        private List<String> l3NetworkUuids;
        private List<String> securityGroupUuids;
        private List<String> hostUuids;
        private List<VmInstanceState> vmStates;
        private List<SecurityGroupState> sgStates;
        private boolean isDelete = false;

        List<HostRuleTO> calculate() {
            if (sgStates == null) {
                sgStates = asList(SecurityGroupState.Enabled);
            }
            if (vmStates == null) {
                vmStates = asList(VmInstanceState.Running);
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
                    " and sg.vSwitchType = :vSwitchType" +
                    " and vm.hostUuid in (:hostUuids) and vm.state in (:vmStates)";
            TypedQuery<String> insgQuery = dbf.getEntityManager().createQuery(sql, String.class);
            insgQuery.setParameter("hostUuids", hostUuids);
            insgQuery.setParameter("vmStates", vmStates);
            insgQuery.setParameter("sgState", sgStates);
            insgQuery.setParameter("vSwitchType", L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);
            List<String> nicsInSg = insgQuery.getResultList();

            sql = "select nic.uuid from VmNicVO nic, VmInstanceVO vm where nic.vmInstanceUuid = vm.uuid" +
                    " and vm.hostUuid in (:hostUuids) and vm.state in (:vmStates)";
            TypedQuery<String> allq = dbf.getEntityManager().createQuery(sql, String.class);
            allq.setParameter("hostUuids", hostUuids);
            allq.setParameter("vmStates", vmStates);
            List<String> allNics = allq.getResultList();
            allNics.removeAll(nicsInSg);

            List<HostRuleTO> ret = new ArrayList<HostRuleTO>();
            if (!nicsInSg.isEmpty()) {
                vmNicUuids = nicsInSg.stream().distinct().collect(Collectors.toList());
                ret.addAll(calculateByVmNic());
            }

            return ret;
        }

        @Transactional(readOnly = true)
        private List<HostRuleTO> calculateBySecurityGroup() {
            vmNicUuids = Q.New(VmNicSecurityGroupRefVO.class)
                            .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                            .in(VmNicSecurityGroupRefVO_.securityGroupUuid, securityGroupUuids)
                            .listValues();
            vmNicUuids = vmNicUuids.stream().distinct().collect(Collectors.toList());
            return calculateByVmNic();
        }

        private List<HostRuleTO> calculateByL3Network() {
            return null;
        }

        private List<HostRuleTO> calculateByL3NetworkAndSecurityGroup() {
            List<String> targetNicUuids = SQL.New("select ref.vmNicUuid from VmNicSecurityGroupRefVO ref, VmNicVO nic, SecurityGroupVO sg" +
                    " where nic.l3NetworkUuid in (:l3Uuids)" +
                    " and ref.vmNicUuid = nic.uuid" +
                    " and ref.securityGroupUuid in (:sgUuids)" +
                    " and sg.state in (:sgStates)", String.class)
                    .param("l3Uuids", l3NetworkUuids)
                    .param("sgUuids", securityGroupUuids)
                    .param("sgStates", sgStates)
                    .list();
            vmNicUuids = targetNicUuids.stream().distinct().collect(Collectors.toList());

            return calculateByVmNic();
        }

        private List<RuleTO> calculateRuleTOBySecurityGroup(String sgUuid, String l3Uuid, int ipVersion) {
            List<RuleTO> ret = new ArrayList<>();
            List<SecurityGroupRuleVO> rules = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid)
                    .eq(SecurityGroupRuleVO_.ipVersion, ipVersion)
                    .eq(SecurityGroupRuleVO_.state, SecurityGroupRuleState.Enabled)
                    .list();

            if (rules.isEmpty()) {
                return ret;
            }

            for (SecurityGroupRuleVO r : rules) {
                if (r.getRemoteSecurityGroupUuid() != null) {
                    if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, r.getRemoteSecurityGroupUuid()).in(SecurityGroupVO_.state, sgStates).isExists()) {
                        continue;
                    }
                }
                RuleTO rto = new RuleTO();
                rto.setIpVersion(r.getIpVersion());
                rto.setPriority(r.getPriority());
                rto.setRuleType(r.getType().toString());
                rto.setState(r.getState().toString());
                rto.setRemoteGroupUuid(r.getRemoteSecurityGroupUuid());
                rto.setRemoteGroupVmIps(getVmIpsBySecurityGroup(r.getRemoteSecurityGroupUuid(), r.getIpVersion()));
                rto.setProtocol(r.getProtocol().toString());
                rto.setSrcIpRange(r.getSrcIpRange());
                rto.setDstIpRange(r.getDstIpRange());
                rto.setDstPortRange(r.getDstPortRange());
                rto.setAction(r.getAction());
                ret.add(rto);
            }

            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("\n-------------- begin calculateRuleTOBySecurityGroupUuid ---------------------"));
                sb.append(String.format("\ninput security group uuid: %s for ipv%d", sgUuid, ipVersion));
                sb.append(String.format("\nresult: %s", JSONObjectUtil.toJsonString(ret)));
                sb.append(String.format("\n-------------- end calculateRuleTOBySecurityGroupUuid ---------------------"));
                logger.trace(sb.toString());
            }

            return ret;
        }

        @Transactional(readOnly = true)
        public VmNicSecurityGroupTo calculateVmNicSecurityGroupTO() {
            if (sgStates == null) {
                sgStates = asList(SecurityGroupState.Enabled);
            }

            VmNicSecurityGroupTo to = new VmNicSecurityGroupTo();
            if (vmNicUuids != null && !vmNicUuids.isEmpty()) {
                // calculate nic security group priority
                List<Tuple> ts = SQL.New("select nic.uuid, nic.internalName, nic.mac" +
                                " from VmInstanceVO vm, VmNicVO nic" +
                                " where nic.uuid in (:vmNicUuids) and nic.vmInstanceUuid = vm.uuid", Tuple.class)
                        .param("vmNicUuids", vmNicUuids)
                        .list();
                if (ts.isEmpty()) {
                    logger.debug(String.format("security group calculateVmNicSecurityGroupTO: no match nics[%s] ", vmNicUuids));
                    return to;
                }

                List<UsedIpVO> usedIps = Q.New(UsedIpVO.class).in(UsedIpVO_.vmNicUuid, vmNicUuids).list();
                List<VmNicSecurityPolicyVO> policies = Q.New(VmNicSecurityPolicyVO.class).in(VmNicSecurityPolicyVO_.vmNicUuid, vmNicUuids).list();
                List<Tuple> refs = SQL.New("select ref.vmNicUuid, ref.priority, sg.uuid, sg.state" +
                                " from VmNicSecurityGroupRefVO ref, SecurityGroupVO sg" +
                                " where ref.vmNicUuid in (:vmNicUuids)" +
                                " and ref.securityGroupUuid = sg.uuid" +
                                " and sg.state in (:sgStates)", Tuple.class)
                        .param("vmNicUuids", vmNicUuids)
                        .param("sgStates", sgStates)
                        .list();
                ;

                for (Tuple t : ts) {
                    String nicUuid = t.get(0, String.class);
                    String nicName = t.get(1, String.class);
                    String mac = t.get(2, String.class);

                    VmNicSecurityPolicyVO policy = policies.stream().filter(p -> p.getVmNicUuid().equals(nicUuid)).findFirst().orElse(null);
                    if (policy == null) {
                        continue;
                    }

                    VmNicSecurityTO nicTo = new VmNicSecurityTO();
                    nicTo.setVmNicUuid(nicUuid);
                    nicTo.setInternalName(nicName);
                    nicTo.setMac(mac);
                    nicTo.setIngressPolicy(policy.getIngressPolicy());
                    nicTo.setEgressPolicy(policy.getEgressPolicy());

                    List<UsedIpVO> ips = usedIps.stream().filter(i -> i.getVmNicUuid().equals(nicUuid)).collect(Collectors.toList());
                    for (UsedIpVO ip : ips) {
                        String ipAddr = ip.getIp();
                        nicTo.getVmNicIps().add(ipAddr);
                    }
                    List<Tuple> sgRefs = refs.stream()
                            .filter(r -> r.get(0, String.class).equals(nicUuid) &&
                                    r.get(3, SecurityGroupState.class) == SecurityGroupState.Enabled)
                            .collect(Collectors.toList());
                    if (sgRefs.isEmpty()) {
                        nicTo.setActionCode(VmNicSecurityTO.ACTION_CODE_DELETE_CHAIN);
                    }
                    for (Tuple sgRef : sgRefs) {
                        int priority = sgRef.get(1, Integer.class);
                        String sgUuid = sgRef.get(2, String.class);
                        if (securityGroupUuids != null && securityGroupUuids.contains(sgUuid)) {
                            nicTo.getSecurityGroupRefs().put(sgUuid, priority);
                        }
                    }

                    to.getVmNics().add(nicTo);
                }
            }

            // calculate security group rules
            if (securityGroupUuids != null && !securityGroupUuids.isEmpty()) {
                for (String uuid : securityGroupUuids) {
                    SecurityGroupTo group = new SecurityGroupTo();

                    SecurityGroupVO vo = dbf.findByUuid(uuid, SecurityGroupVO.class);
                    group.setSecurityGroupUuid(uuid);
                    group.setSecurityGroupName(vo.getName());
                    int internalId = (int)vo.getInternalId();
                    group.setInternalId(internalId);
                    group.setSecurityGroupVmIps(getVmIpsBySecurityGroup(uuid, IPv6Constants.IPv4));
                    group.setSecurityGroupVmIp6s(getVmIpsBySecurityGroup(uuid, IPv6Constants.IPv6));

                    List<SecurityGroupRuleVO> rules = vo.getRules().stream()
                            .filter(r -> r.getState() == SecurityGroupRuleState.Enabled)
                            .collect(Collectors.toList());
                    for (SecurityGroupRuleVO r : rules) {
                        RuleTO rto = new RuleTO();
                        rto.setIpVersion(r.getIpVersion());
                        rto.setPriority(r.getPriority());
                        rto.setRuleType(r.getType().toString());
                        rto.setState(r.getState().toString());
                        rto.setRemoteGroupUuid(r.getRemoteSecurityGroupUuid());
                        rto.setProtocol(r.getProtocol().toString());
                        rto.setSrcIpRange(r.getSrcIpRange());
                        rto.setDstIpRange(r.getDstIpRange());
                        rto.setDstPortRange(r.getDstPortRange());
                        rto.setAction(r.getAction());
                        group.getRules().add(rto);
                    }

                    to.getGroups().add(group);
                }
            }

            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("\n=================== begin calculateVmNicSecurityGroupTO ======================"));
                sb.append(String.format("\ninput vmNic uuids: %s", vmNicUuids));
                sb.append(String.format("\nresult: %s", JSONObjectUtil.toJsonString(to)));
                sb.append(String.format("\n=================== end calculateVmNicSecurityGroupTO ========================"));
                logger.trace(sb.toString());
            }

            return to;
        }

        @Transactional(readOnly = true)
        private List<HostRuleTO> calculateByVmNic() {
            Map<String, HostRuleTO> htoMap = new HashMap<String, HostRuleTO>();

            if (vmNicUuids == null || vmNicUuids.isEmpty()) {
                return new ArrayList<>(htoMap.values());
            }

            List<Tuple> ts = SQL.New("select vm.hostUuid, vm.hypervisorType, nic.uuid, nic.internalName, nic.mac" +
                    " from VmInstanceVO vm, VmNicVO nic" +
                    " where nic.uuid in (:vmNicUuids) and nic.vmInstanceUuid = vm.uuid and vm.state in (:vmStates)", Tuple.class)
                    .param("vmNicUuids", vmNicUuids)
                    .param("vmStates", vmStates)
                    .list();

            if (ts.isEmpty()) {
                logger.debug(String.format("security group calcuateByVmNic: no match nics[%s] ", vmNicUuids));
                return new ArrayList<>(htoMap.values());
            }

            List<UsedIpVO> usedIps = Q.New(UsedIpVO.class).in(UsedIpVO_.vmNicUuid, vmNicUuids).list();
            List<VmNicSecurityPolicyVO> policies = Q.New(VmNicSecurityPolicyVO.class).in(VmNicSecurityPolicyVO_.vmNicUuid, vmNicUuids).list();

            List<Tuple> refs = SQL.New("select ref.vmNicUuid, ref.priority, sg.uuid" +
                    " from VmNicSecurityGroupRefVO ref, SecurityGroupVO sg" +
                    " where ref.vmNicUuid in (:vmNicUuids)" +
                    " and ref.securityGroupUuid = sg.uuid" +
                    " and sg.state in (:sgStates)", Tuple.class)
                    .param("vmNicUuids", vmNicUuids)
                    .param("sgStates", sgStates)
                    .list();

            for (Tuple t : ts) {
                String hostUuid = t.get(0, String.class);
                String hvType = t.get(1, String.class);
                String nicUuid = t.get(2, String.class);
                String nicName = t.get(3, String.class);
                String mac = t.get(4, String.class);

                VmNicSecurityPolicyVO policy = policies.stream().filter(p -> p.getVmNicUuid().equals(nicUuid)).findFirst().orElse(null);
                if (policy == null) {
                    continue;
                }

                HostRuleTO hto = htoMap.get(hostUuid);
                if (hto == null) {
                    hto = new HostRuleTO();
                    hto.setHypervisorType(hvType);
                    hto.setHostUuid(hostUuid);
                    htoMap.put(hostUuid, hto);
                }

                VmNicSecurityTO nicTo = new VmNicSecurityTO();
                nicTo = new VmNicSecurityTO();
                nicTo.setVmNicUuid(nicUuid);
                nicTo.setInternalName(nicName);
                nicTo.setMac(mac);
                nicTo.setIngressPolicy(policy.getIngressPolicy());
                nicTo.setEgressPolicy(policy.getEgressPolicy());
                if (isDelete) {
                    nicTo.setActionCode(VmNicSecurityTO.ACTION_CODE_DELETE_CHAIN);
                    hto.getVmNics().add(nicTo);
                    continue;
                }
                nicTo.setActionCode(VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN);
                hto.getVmNics().add(nicTo);

                List<UsedIpVO> ips = usedIps.stream().filter(i -> i.getVmNicUuid().equals(nicUuid)).collect(Collectors.toList());
                List<Tuple> sgRefs = refs.stream().filter(r -> r.get(0, String.class).equals(nicUuid)).collect(Collectors.toList());
                if (ips.isEmpty() || sgRefs.isEmpty()) {
                    continue;
                }

                for (UsedIpVO ip : ips) {
                    String l3Uuid = ip.getL3NetworkUuid();
                    String ipAddr = ip.getIp();
                    int ipVersion = ip.getIpVersion();

                    nicTo.getVmNicIps().add(ipAddr);
                    // get security group rules if actionCode == "applyChain"
                    Map<String, List<RuleTO>> sgRules = ipVersion == IPv6Constants.IPv4 ? hto.getRules() : hto.getIp6Rules();
                    for (Tuple sgRef : sgRefs) {
                        int priority = sgRef.get(1, Integer.class);
                        String sgUuid = sgRef.get(2, String.class);

                        nicTo.getSecurityGroupRefs().put(sgUuid, priority);

                        if (!sgRules.containsKey(sgUuid)) {
                            List<RuleTO> rule = calculateRuleTOBySecurityGroup(sgUuid, l3Uuid, ipVersion);
                            sgRules.put(sgUuid, rule);
                        }
                    }
                }
            }

            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("\n=================== begin rulesByNicUuids ======================"));
                sb.append(String.format("\ninput vmNic uuids: %s", vmNicUuids));
                sb.append(String.format("\nresult: %s", JSONObjectUtil.toJsonString(htoMap.values())));
                sb.append(String.format("\n=================== end rulesByNicUuids ========================"));
                logger.trace(sb.toString());
            }

            return htoMap.values().stream().collect(Collectors.toList());
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

    @Override
    public VmNicSecurityGroupTo getVmNicSecurityGroupRules(List<String> sgUuids) {
        RuleCalculator cal = new RuleCalculator();
        cal.sgStates = Collections.singletonList(SecurityGroupState.Enabled);
        cal.securityGroupUuids = sgUuids;
        cal.vmNicUuids = Q.New(VmNicSecurityGroupRefVO.class)
                .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                .in(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuids)
                .listValues();
        cal.vmNicUuids = cal.vmNicUuids.stream().distinct().collect(Collectors.toList());
        if (cal.vmNicUuids.isEmpty()) {
            VmNicSecurityGroupTo nicTo = cal.calculateVmNicSecurityGroupTO();
            return nicTo;
        }

        return cal.calculateVmNicSecurityGroupTO();
    }

    private void sdnRefreshVmNicsDefaultRule(SecurityGroupSdnBackend sdnBackend, List<String> vmNicUuids, Completion completion) {
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = vmNicUuids;

        VmNicSecurityGroupTo nicTo = cal.calculateVmNicSecurityGroupTO();
        sdnBackend.updateSecurityGroup(nicTo, completion);
    }

    private void sdnRefreshVmNics(SecurityGroupSdnBackend sdnBackend, List<String> vmNicUuids, Completion completion) {
        RuleCalculator cal = new RuleCalculator();
        cal.sgStates = Collections.singletonList(SecurityGroupState.Enabled);;
        cal.securityGroupUuids = Q.New(VmNicSecurityGroupRefVO.class)
                .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids)
                .listValues();
        if (cal.securityGroupUuids.isEmpty()) {
            completion.success();
            return;
        }
        cal.securityGroupUuids = cal.securityGroupUuids.stream().distinct().collect(Collectors.toList());
        cal.vmNicUuids = vmNicUuids;

        VmNicSecurityGroupTo nicTo = cal.calculateVmNicSecurityGroupTO();
        for (VmNicSecurityTO to : nicTo.vmNics) {
            to.setSync(true);
        }

        sdnBackend.updateSecurityGroup(nicTo, completion);
    }

    private void sdnRemoveSecurityGroupFromVmNic(SecurityGroupSdnBackend sdnBackend,
                                                 List<String> sgUuids, List<String> vmNicUuids, Completion completion) {
        RuleCalculator cal = new RuleCalculator();
        cal.securityGroupUuids = Q.New(VmNicSecurityGroupRefVO.class)
                .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids)
                .listValues();
        cal.securityGroupUuids.addAll(sgUuids);
        cal.securityGroupUuids = cal.securityGroupUuids.stream().distinct().collect(Collectors.toList());
        cal.vmNicUuids = vmNicUuids;

        VmNicSecurityGroupTo nicTo = cal.calculateVmNicSecurityGroupTO();
        for (VmNicSecurityTO to : nicTo.vmNics) {
            to.setSync(true);
        }
        sdnBackend.updateSecurityGroup(nicTo, completion);
    }

    private void sdnDeleteSecurityGroup(SecurityGroupSdnBackend sdnBackend, List<String> vmNicUuids,
                                        String sgUuid,Completion completion) {
        RuleCalculator cal = new RuleCalculator();
        if (!vmNicUuids.isEmpty()) {
            cal.securityGroupUuids = Q.New(VmNicSecurityGroupRefVO.class)
                    .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                    .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids)
                    .listValues();
        } else {
            cal.securityGroupUuids = new ArrayList<>();
        }
        cal.securityGroupUuids.add(sgUuid);
        cal.securityGroupUuids = cal.securityGroupUuids.stream().distinct().collect(Collectors.toList());
        cal.vmNicUuids = vmNicUuids;

        VmNicSecurityGroupTo nicTo = cal.calculateVmNicSecurityGroupTO();
        for (SecurityGroupTo group : nicTo.groups) {
            if (group.getSecurityGroupUuid().equals(sgUuid)) {
                group.setActionCode(SecurityGroupTo.ACTION_CODE_DELETE_CHAIN);
            }
        }
        for (VmNicSecurityTO nic : nicTo.vmNics) {
            nic.setSync(true);
        }
        sdnBackend.updateSecurityGroup(nicTo, completion);
    }

    private void sdnRefreshSecurityGroup(SecurityGroupSdnBackend sdnBackend, String sgUuid,Completion completion) {
        SecurityGroupVO groupVO = dbf.findByUuid(sgUuid, SecurityGroupVO.class);
        if (groupVO.getState() == SecurityGroupState.Disabled) {
            completion.success();
            return;
        }

        List<String> vmNicUuids = Q.New(VmNicSecurityGroupRefVO.class)
                .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                .eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid)
                .listValues();
        if (vmNicUuids.isEmpty()) {
            completion.success();
            return;
        }

        RuleCalculator cal = new RuleCalculator();
        cal.securityGroupUuids = Collections.singletonList(sgUuid);
        cal.vmNicUuids = vmNicUuids;

        VmNicSecurityGroupTo nicTo = cal.calculateVmNicSecurityGroupTO();
        //security group is not attached to vm
        if (nicTo.getVmNics().isEmpty()) {
            completion.success();
            return;
        }

        sdnBackend.updateSecurityGroup(nicTo, completion);
    }

    private void handle(CreateSecurityGroupMsg msg) {
        CreateSecurityGroupReply reply = new CreateSecurityGroupReply();
        SecurityGroupVO vo = new SecurityGroupVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(SecurityGroupState.Enabled);
        vo.setvSwitchType(msg.getvSwitchType());
        vo.setInternalId(dbf.generateSequenceNumber(SecurityGroupSequenceNumberVO.class));
        vo.setAccountUuid(msg.getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        createDefaultRule(vo.getUuid(), IPv6Constants.IPv4);
        createDefaultRule(vo.getUuid(), IPv6Constants.IPv6);

        SecurityGroupSdnBackend sdnBackend = getSdnBackend(msg.getSdnControllerUuid());
        if (sdnBackend == null) {
            reply.setInventory(SecurityGroupInventory.valueOf(vo));
            bus.reply(msg, reply);
            return;
        }

        final SecurityGroupInventory inv = SecurityGroupInventory.valueOf(vo);
        sdnBackend.createSecurityGroup(inv, new Completion(msg) {
            @Override
            public void success() {
                reply.setInventory(SecurityGroupInventory.valueOf(
                        dbf.findByUuid(inv.getUuid(), SecurityGroupVO.class)));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(AddSecurityGroupRuleMsg msg) {
        for (APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao : msg.getRules()) {
            if (ao.getAllowedCidr() == null) {
                ao.setAllowedCidr(ao.getIpVersion() == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
            }
            if (ao.getStartPort() == null || ao.getEndPort() == null) {
                ao.setStartPort(-1);
                ao.setEndPort(-1);
            }
            if (!SecurityGroupConstant.WORLD_OPEN_CIDR.equals(ao.getAllowedCidr()) && !SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6.equals(ao.getAllowedCidr())) {
                if (ao.getType().equals(SecurityGroupRuleType.Egress.toString())) {
                    ao.setDstIpRange(ao.getAllowedCidr());
                } else {
                    ao.setSrcIpRange(ao.getAllowedCidr());
                }
            }
            if (ao.getStartPort() != -1) {
                if (ao.getStartPort().equals(ao.getEndPort())) {
                    ao.setDstPortRange(String.valueOf(ao.getStartPort()));
                } else {
                    ao.setDstPortRange(String.format("%s-%s", ao.getStartPort(), ao.getEndPort()));
                }
            }
        }

        AddSecurityGroupRuleReply reply = new AddSecurityGroupRuleReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doAddSecurityGroupRule(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        reply.setInventory(SecurityGroupInventory.valueOf(dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class)));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("add-security-group-%s-rule", msg.getSecurityGroupUuid());
            }
        });
    }

    private void handle(SecurityGroupDeletionMsg msg) {
        SecurityGroupDeletionReply reply = new SecurityGroupDeletionReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteSecurityGroup(msg.getUuid(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-security-group-%s", msg.getUuid());
            }
        });
    }

    private void handle(RemoveVmNicFromSecurityGroupMsg msg) {
        RemoveVmNicFromSecurityGroupReply reply = new RemoveVmNicFromSecurityGroupReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getVmNicSecurityGroupRefSyncThreadName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                removeNicFromSecurityGroup(msg.getSecurityGroupUuid(), msg.getVmNicUuids(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("remove-vm-nic-from-security-group-%s", msg.getSecurityGroupUuid());
            }
        });
    }

    private void refreshVmSecurityGroupRulesBySdn(Map<SecurityGroupSdnBackend, List<String>> sdnNicUuidsMap,
                                                  RefreshSecurityGroupRulesOnVmMsg msg, Completion completion) {
        if (msg.getOperation() != VmInstanceConstant.VmOperation.NewCreate
                && msg.getOperation() != VmInstanceConstant.VmOperation.Destroy
                && msg.getOperation() != VmInstanceConstant.VmOperation.AttachNic
                && msg.getOperation() != VmInstanceConstant.VmOperation.DetachNic
                && msg.getOperation() != VmInstanceConstant.VmOperation.ChangeNicNetwork) {
            completion.success();
            return;
        }

        new While<>(sdnNicUuidsMap.entrySet()).each((entry, wcomp) -> {
            SecurityGroupSdnBackend backend = entry.getKey();
            List<String> vmNicUuids = entry.getValue();
            if (vmNicUuids.isEmpty()) {
                wcomp.done();
                return;
            }

            vmNicUuids = Q.New(VmNicSecurityGroupRefVO.class)
                    .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                    .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids)
                    .listValues();
            if (vmNicUuids.isEmpty()) {
                wcomp.done();
                return;
            }

            if (msg.isDeleteAllRules()) {
                List<String> sgUuids = Q.New(VmNicSecurityGroupRefVO.class)
                        .select(VmNicSecurityGroupRefVO_.securityGroupUuid)
                        .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids)
                        .listValues();
                SQL.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids).delete();
                sdnRemoveSecurityGroupFromVmNic(backend, sgUuids,vmNicUuids, new Completion(wcomp) {
                    @Override
                    public void success() {
                        wcomp.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        wcomp.addError(errorCode);
                        wcomp.allDone();
                    }
                });
            } else {
                sdnRefreshVmNics(backend, vmNicUuids, new Completion(wcomp) {
                    @Override
                    public void success() {
                        wcomp.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        wcomp.addError(errorCode);
                        wcomp.allDone();
                    }
                });
            }
        }).run(new WhileDoneCompletion(new NopeCompletion()) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList);
                } else {
                    completion.success();
                }
            }
        });
    }

    private void refreshVmSecurityGroupRules(List<String> nicUuids, RefreshSecurityGroupRulesOnVmMsg msg) {
        nicUuids = nicUuids.stream().distinct().collect(Collectors.toList());
        Collection<HostRuleTO> htos;
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = nicUuids;
        cal.vmStates = asList(VmInstanceState.values());
        cal.isDelete = msg.isDeleteAllRules();
        htos = cal.calculate();

        applyRules(htos);

        if (msg.getSgUuids() != null && !msg.getSgUuids().isEmpty()) {
            Q.New(SecurityGroupVO.class)
                    .select(SecurityGroupVO_.uuid)
                    .eq(SecurityGroupVO_.vSwitchType, L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE)
                    .in(SecurityGroupVO_.uuid, msg.getSgUuids())
                    .eq(SecurityGroupVO_.state, SecurityGroupState.Enabled)
                    .listValues().forEach(sgUuid -> {
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
        if (msg.getHostUuid() == null ||msg.getHostUuid().isEmpty()) {
            String HostUuid = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .select(VmInstanceVO_.hostUuid)
                    .findValue();

            msg.setHostUuid(HostUuid);
        }

        if (nicUuids.isEmpty()) {
            checkDefaultRulesOnHost(msg.getHostUuid());
            logger.debug(String.format("no nic of vm[uuid:%s] needs to refresh security group rule", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        List<String> otherNicUuids = new ArrayList<>();
        Map<SecurityGroupSdnBackend, List<String>> sdnNicUuidsMap = new HashMap<>();
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).in(VmNicVO_.uuid, nicUuids).list();
        for (VmNicVO nicvo : vmNicVOS) {
            SecurityGroupSdnBackend backend = getSdnBackendFroL3Uuid(nicvo.getL3NetworkUuid());
            if (backend == null) {
                otherNicUuids.add(nicvo.getUuid());
            } else {
                sdnNicUuidsMap.computeIfAbsent(backend, k -> new ArrayList<>());
                sdnNicUuidsMap.get(backend).add(nicvo.getUuid());
            }
        }

        refreshVmSecurityGroupRulesBySdn(sdnNicUuidsMap, msg, new Completion(new NopeCompletion()) {
            @Override
            public void success() {
                if (otherNicUuids.isEmpty()) {
                    bus.reply(msg, reply);
                    return;
                }
                refreshVmSecurityGroupRules(otherNicUuids, msg);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void createFailureHostTask(String huuid) {
        SecurityGroupFailureHostVO fvo = new SecurityGroupFailureHostVO();
        fvo.setHostUuid(huuid);
        dbf.persist(fvo);
    }

    private void handle(RefreshSecurityGroupRulesOnHostMsg msg) {
        // this message is sent after host reconnected, sdn controller will not handle it
        RuleCalculator cal = new RuleCalculator();
        cal.hostUuids = Collections.singletonList(msg.getHostUuid());
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
        } else if (msg instanceof APIChangeSecurityGroupRuleMsg) {
            handle((APIChangeSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIUpdateSecurityGroupRulePriorityMsg) {
            handle((APIUpdateSecurityGroupRulePriorityMsg) msg);
        } else if (msg instanceof APIChangeVmNicSecurityPolicyMsg) {
            handle((APIChangeVmNicSecurityPolicyMsg) msg);
        } else if (msg instanceof APIChangeSecurityGroupRuleStateMsg) {
            handle((APIChangeSecurityGroupRuleStateMsg) msg);
        } else if (msg instanceof APISetVmNicSecurityGroupMsg) {
            handle((APISetVmNicSecurityGroupMsg) msg);
        } else if (msg instanceof APIValidateSecurityGroupRuleMsg) {
            handle((APIValidateSecurityGroupRuleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIValidateSecurityGroupRuleMsg msg) {
        APIValidateSecurityGroupRuleReply reply = new APIValidateSecurityGroupRuleReply();
        reply.setAvailable(true);
        reply.setCode(SecurityGroupErrors.RULE_CHECK_OK.toString());
        bus.reply(msg, reply);
    }


    private void setVmNicSecurityGroup(APISetVmNicSecurityGroupMsg msg, Completion completion) {
        VmNicVO nic = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        SecurityGroupSdnBackend backend = getSdnBackendFroL3Uuid(nic.getL3NetworkUuid());
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in setVmNicSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "set-vm-nic-security-group-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VmNicSecurityGroupRefVO> toCreate = new ArrayList<>();
                        List<VmNicSecurityGroupRefVO> toDelete = new ArrayList<>();
                        List<VmNicSecurityGroupRefVO> toUpdate = new ArrayList<>();
                        List<String> sgUuids = new ArrayList<>();
                        Map<String, VmNicSecurityGroupRefVO> refMap = new HashMap<>();
                        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, msg.getVmNicUuid()).list();
                        refs.forEach(ref -> {
                            refMap.put(ref.getSecurityGroupUuid(), ref);
                        });

                        for (VmNicSecurityGroupRefAO ao : msg.getRefs()) {
                            if (!refMap.containsKey(ao.getSecurityGroupUuid())) {
                                // to create
                                VmNicSecurityGroupRefVO vo = new VmNicSecurityGroupRefVO();
                                vo.setUuid(Platform.getUuid());
                                vo.setVmNicUuid(nic.getUuid());
                                vo.setPriority(ao.getPriority());
                                vo.setVmInstanceUuid(nic.getVmInstanceUuid());
                                vo.setSecurityGroupUuid(ao.getSecurityGroupUuid());
                                toCreate.add(vo);
                                sgUuids.add(ao.getSecurityGroupUuid());
                            } else {
                                // to update
                                VmNicSecurityGroupRefVO vo = refMap.get(ao.getSecurityGroupUuid());
                                vo.setPriority(ao.getPriority());
                                toUpdate.add(vo);
                                refMap.remove(ao.getSecurityGroupUuid());
                            }
                        }

                        // to delete
                        toDelete.addAll(refMap.values());
                        refMap.values().forEach(ref -> sgUuids.add(ref.getSecurityGroupUuid()));

                        if (!toCreate.isEmpty()) {
                            dbf.persistCollection(toCreate);
                        }
                        if (!toDelete.isEmpty()) {
                            dbf.removeCollection(toDelete, VmNicSecurityGroupRefVO.class);
                        }
                        if (!toUpdate.isEmpty()) {
                            dbf.updateCollection(toUpdate);
                        }

                        if (!toCreate.isEmpty() || !toUpdate.isEmpty()) {
                            if (!Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, msg.getVmNicUuid()).isExists()) {
                                VmNicSecurityPolicyVO vo = new VmNicSecurityPolicyVO();
                                vo.setUuid(Platform.getUuid());
                                vo.setVmNicUuid(msg.getVmNicUuid());
                                vo.setIngressPolicy(VmNicSecurityPolicy.DENY.toString());
                                vo.setEgressPolicy(VmNicSecurityPolicy.ALLOW.toString());
                                dbf.persist(vo);
                            }
                        }

                        data.put(SecurityGroupConstant.Param.SECURITY_GROUP_UUIDS, sgUuids);
                        data.put(SecurityGroupConstant.Param.SECURITY_GROUP_REFS, toDelete);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return backend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.vmNicUuids = asList(msg.getVmNicUuid());
                        List<HostRuleTO> rhtos = cal.calculate();
                        applyRules(rhtos);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "update-group-members";

                    @Override
                    public boolean skip(Map data) {
                        return backend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> sgUuids = (List<String>)data.get(SecurityGroupConstant.Param.SECURITY_GROUP_UUIDS);
                        for (String sgUuid : sgUuids) {
                            RuleCalculator cal = new RuleCalculator();
                            HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(sgUuid);
                            if(!groupMemberTO.getHostUuids().isEmpty()){
                                if (Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).eq(SecurityGroupVO_.state, SecurityGroupState.Disabled).isExists()) {
                                    groupMemberTO.getGroupMembersTO().setActionCode(ACTION_CODE_DELETE_GROUP);
                                }
                                updateGroupMembers(groupMemberTO);
                            }
                        }

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "update-sdn-controller";

                    @Override
                    public boolean skip(Map data) {
                        return backend == null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VmNicSecurityGroupRefVO> toDelete = (List<VmNicSecurityGroupRefVO>) data.get(SecurityGroupConstant.Param.SECURITY_GROUP_REFS);
                        List<String> sgUuids = toDelete.stream().map(VmNicSecurityGroupRefVO::getSecurityGroupUuid).distinct().collect(Collectors.toList());
                        sdnRemoveSecurityGroupFromVmNic(backend, sgUuids, Collections.singletonList(msg.getVmNicUuid()), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });

            }
        }).start();
    }

    private void handle(APISetVmNicSecurityGroupMsg msg) {
        APISetVmNicSecurityGroupEvent evt = new APISetVmNicSecurityGroupEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getVmNicSecurityGroupRefSyncThreadName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                setVmNicSecurityGroup(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, msg.getVmNicUuid()).list();
                        evt.setInventory(VmNicSecurityGroupRefInventory.valueOf(refs));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("set-vm-nic-%s-security-group", msg.getVmNicUuid());
            }
        });
    }

    private void doChangeSecurityGroupRuleState(APIChangeSecurityGroupRuleStateMsg msg, Completion completion) {
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(msg.getSecurityGroupUuid());
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("change-security-group-%s-rule-state", msg.getSecurityGroupUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in doChangeSecurityGroupRuleState
                flow(new NoRollbackFlow() {
                    String __name__ = "change-security-group-rule-state-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class)
                                .eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid())
                                .in(SecurityGroupRuleVO_.uuid, msg.getRuleUuids())
                                .list();

                        rvos.forEach(rvo -> {
                            rvo.setState(SecurityGroupRuleState.valueOf(msg.getState()));
                        });
                        dbf.updateCollection(rvos);
                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in doChangeSecurityGroupRuleState
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.securityGroupUuids = asList(msg.getSecurityGroupUuid());
                        List<HostRuleTO> htos = cal.calculate();
                        applyRules(htos);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in doChangeSecurityGroupRuleState
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnBackend == null) {
                            trigger.next();
                            return;
                        }

                        sdnRefreshSecurityGroup(sdnBackend, msg.getSecurityGroupUuid(),
                                new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });

            }
        }).start();
    }

    private void handle(APIChangeSecurityGroupRuleStateMsg msg) {
        APIChangeSecurityGroupRuleStateEvent evt = new APIChangeSecurityGroupRuleStateEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doChangeSecurityGroupRuleState(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SecurityGroupVO vo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
                        evt.setInventory(SecurityGroupInventory.valueOf(vo));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("change-security-group-%s-rule-state", msg.getSecurityGroupUuid());
            }
        });
    }

    private void handle(APIChangeVmNicSecurityPolicyMsg msg) {
        APIChangeVmNicSecurityPolicyEvent evt = new APIChangeVmNicSecurityPolicyEvent(msg.getId());
        VmNicSecurityPolicyVO pvo = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, msg.getVmNicUuid()).find();

        if (msg.getIngressPolicy() == null && msg.getEgressPolicy() == null) {
            logger.debug(String.format("vm nic[uuid:%s] security policy not change", msg.getVmNicUuid()));
            evt.setInventory(VmNicSecurityPolicyInventory.valueOf(pvo));
            bus.publish(evt);

            return;
        }


        if (msg.getIngressPolicy() != null) {
            pvo.setIngressPolicy(msg.getIngressPolicy());
        }

        if (msg.getEgressPolicy() != null) {
            pvo.setEgressPolicy(msg.getEgressPolicy());
        }

        pvo = dbf.updateAndRefresh(pvo);
        VmNicSecurityPolicyInventory pinv = VmNicSecurityPolicyInventory.valueOf(pvo);
        VmNicVO nicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        SecurityGroupSdnBackend backend = getSdnBackendFroL3Uuid(nicVO.getL3NetworkUuid());
        if (backend == null) {
            RuleCalculator cal = new RuleCalculator();
            cal.vmNicUuids = asList(msg.getVmNicUuid());
            List<HostRuleTO> htos = cal.calculate();
            applyRules(htos);

            evt.setInventory(pinv);
            bus.publish(evt);
            return;
        }

        sdnRefreshVmNicsDefaultRule(backend, Collections.singletonList(msg.getVmNicUuid()), new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(pinv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void doUpdateSecurityGroupRulePriority(APIUpdateSecurityGroupRulePriorityMsg msg, Completion completion) {
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(msg.getSecurityGroupUuid());
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("update-security-group-%s-rule-priority", msg.getSecurityGroupUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in doUpdateSecurityGroupRulePriority
                flow(new NoRollbackFlow() {
                    String __name__ = "update-security-group-rule-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        List<SecurityGroupRuleVO> toUpdate = new ArrayList<SecurityGroupRuleVO>();

                        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class)
                                .eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid())
                                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.valueOf(msg.getType()))
                                .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                                .list();
                        for (SecurityGroupRulePriorityAO ao : msg.getRules()) {
                            SecurityGroupRuleVO vo = rvos.stream().filter(r -> r.getUuid().equals(ao.getRuleUuid())).findFirst().orElse(null);
                            if (vo == null) {
                                throw new OperationFailureException(operr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10125, "failed to chenge rule[uuid:%s] priority, beacuse it's not found", ao.getRuleUuid()));
                            }
                            if (ao.getPriority() != vo.getPriority()) {
                                vo.setPriority(ao.getPriority());
                                toUpdate.add(vo);
                            }
                        }

                        dbf.updateCollection(toUpdate);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in doUpdateSecurityGroupRulePriority
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.securityGroupUuids = asList(msg.getSecurityGroupUuid());
                        cal.vmStates = asList(VmInstanceState.Running);
                        List<HostRuleTO> rhtos = cal.calculate();
                        applyRules(rhtos);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-sdn-controller";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnBackend == null) {
                            trigger.next();
                            return;
                        }

                        sdnRefreshSecurityGroup(sdnBackend, msg.getSecurityGroupUuid(),
                                new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });

            }
        }).start();
    }

    private void handle(APIUpdateSecurityGroupRulePriorityMsg msg) {
        APIUpdateSecurityGroupRulePriorityEvent evt = new APIUpdateSecurityGroupRulePriorityEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doUpdateSecurityGroupRulePriority(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SecurityGroupVO vo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
                        evt.setInventory(SecurityGroupInventory.valueOf(vo));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-security-group-%s-%s-rule-priority", msg.getSecurityGroupUuid(), msg.getType());
            }
        });
    }

    private void handle(APIChangeSecurityGroupRuleMsg msg) {
        APIChangeSecurityGroupRuleEvent evt = new APIChangeSecurityGroupRuleEvent(msg.getId());

        String sgUuid = Q.New(SecurityGroupRuleVO.class).select(SecurityGroupRuleVO_.securityGroupUuid).eq(SecurityGroupRuleVO_.uuid, msg.getUuid()).findValue();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(sgUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doChangeSecurityGroupRule(msg, sgUuid, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SecurityGroupRuleVO vo = dbf.findByUuid(msg.getUuid(), SecurityGroupRuleVO.class);
                        evt.setInventory(SecurityGroupRuleInventory.valueOf(vo));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("change-security-group-%s-rule-%s", sgUuid, msg.getUuid());
            }
        });
    }

    private void doChangeSecurityGroupRule(APIChangeSecurityGroupRuleMsg msg, String sgUuid, Completion completion) {
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(sgUuid);
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in doChangeSecurityGroupRule
                flow(new NoRollbackFlow() {
                    String __name__ = "change-security-group-rule-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SecurityGroupRuleVO vo = dbf.findByUuid(msg.getUuid(), SecurityGroupRuleVO.class);
                        vo.setDescription(msg.getDescription());
                        vo.setState(SecurityGroupRuleState.valueOf(msg.getState()));
                        vo.setAction(msg.getAction());
                        vo.setProtocol(SecurityGroupRuleProtocolType.valueOf(msg.getProtocol()));
                        vo.setRemoteSecurityGroupUuid(msg.getRemoteSecurityGroupUuid());
                        vo.setSrcIpRange(msg.getSrcIpRange());
                        vo.setDstIpRange(msg.getDstIpRange());
                        vo.setDstPortRange(msg.getDstPortRange());

                        if (StringUtils.isNotEmpty(msg.getSrcIpRange()) || StringUtils.isNotEmpty(msg.getDstIpRange()) || StringUtils.isNotEmpty(msg.getRemoteSecurityGroupUuid())) {
                            vo.setAllowedCidr(vo.getIpVersion() == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
                        }
                        if (StringUtils.isNotEmpty(msg.getDstPortRange())) {
                            vo.setStartPort(-1);
                            vo.setEndPort(-1);
                        }

                        if (msg.getPriority() != null && msg.getPriority() != vo.getPriority()) {
                            List<SecurityGroupRuleVO> others = Q.New(SecurityGroupRuleVO.class)
                                .eq(SecurityGroupRuleVO_.securityGroupUuid, vo.getSecurityGroupUuid())
                                .eq(SecurityGroupRuleVO_.type, vo.getType())
                                .notEq(SecurityGroupRuleVO_.uuid, vo.getUuid())
                                .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                                .list();
                            final int finalPriority = msg.getPriority() == -1 ? others.size() + 1 : msg.getPriority();

                            if (vo.getPriority() > finalPriority) {
                                List<SecurityGroupRuleVO> toUpdate = others.stream().filter(r -> r.getPriority() >= finalPriority && r.getPriority() < vo.getPriority()).collect(Collectors.toList());

                                toUpdate.stream().forEach(r -> r.setPriority(r.getPriority() + 1));
                                dbf.updateCollection(toUpdate);
                            } else {
                                List<SecurityGroupRuleVO> toUpdate = others.stream().filter(r -> r.getPriority() <= finalPriority && r.getPriority() > vo.getPriority()).collect(Collectors.toList());

                                toUpdate.stream().forEach(r -> r.setPriority(r.getPriority() - 1));
                                dbf.updateCollection(toUpdate);
                            }

                            vo.setPriority(finalPriority);
                        }

                        dbf.update(vo);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.securityGroupUuids = asList(sgUuid);
                        cal.vmStates = asList(VmInstanceState.Running);
                        List<HostRuleTO> htos = cal.calculate();
                        applyRules(htos);
                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-sdn-controller";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnBackend == null) {
                            trigger.next();
                            return;
                        }

                        sdnRefreshSecurityGroup(sdnBackend, sgUuid, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });

            }
        }).start();
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
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(sgId);
        if (sdnBackend != null) {
            return sdnBackend.getCandidateVmNic(sgId, accountUuid);
        }

        List<String> nicUuidsToInclude = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VmNicVO.class);
        if (nicUuidsToInclude != null && nicUuidsToInclude.isEmpty()) {
            return new ArrayList<VmNicVO>();
        }

        List<String> nicUuidsToExclued = Q.New(VmNicSecurityGroupRefVO.class)
                .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                .eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgId)
                .listValues();

        List<VmNicVO> candidateNics = new ArrayList<>();
        List<VmNicVO> allNics = SQL.New("select nic from VmNicVO nic, VmInstanceVO vm" +
                        " where nic.vmInstanceUuid = vm.uuid" +
                        " and nic.type = :nicType " +
                        " and vm.type = :vmType" +
                        " and vm.state in (:vmStates)", VmNicVO.class)
                .param("nicType", VmInstanceConstant.VIRTUAL_NIC_TYPE)
                .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                .param("vmStates", list(VmInstanceState.Running, VmInstanceState.Stopped))
                .list();
        if (allNics.isEmpty()) {
            return allNics;
        }

        if (!nicUuidsToExclued.isEmpty()) {
            if (nicUuidsToInclude != null && !nicUuidsToInclude.isEmpty()) {
                // accessed by a normal account
                allNics.stream().forEach(nic -> {
                    if (!nicUuidsToExclued.contains(nic.getUuid()) && nicUuidsToInclude.contains(nic.getUuid())) {
                        candidateNics.add(nic);
                    }
                });
            } else {
                // accessed by an admin
                allNics.stream().forEach(nic -> {
                    if (!nicUuidsToExclued.contains(nic.getUuid())) {
                        candidateNics.add(nic);
                    }
                });
            }
        } else {
            if (nicUuidsToInclude != null && !nicUuidsToInclude.isEmpty()) {
                // accessed by a normal account
                allNics.stream().forEach(nic -> {
                    if (nicUuidsToInclude.contains(nic.getUuid())) {
                        candidateNics.add(nic);
                    }
                });
            } else {
                // accessed by an admin
                return allNics;
            }
        }

        return candidateNics;
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
        APIDetachSecurityGroupFromL3NetworkEvent evt = new APIDetachSecurityGroupFromL3NetworkEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                List<String> vmNicUuids = getVmNicUuidsToRemoveForDetachSecurityGroup(msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());
                if (!vmNicUuids.isEmpty()) {
                    RemoveVmNicFromSecurityGroupMsg rmsg = new RemoveVmNicFromSecurityGroupMsg();
                    rmsg.setSecurityGroupUuid(msg.getSecurityGroupUuid());
                    rmsg.setVmNicUuids(vmNicUuids);
                    bus.makeTargetServiceIdByResourceUuid(rmsg, SecurityGroupConstant.SERVICE_ID, msg.getSecurityGroupUuid());

                    bus.send(rmsg, new CloudBusCallBack(msg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                evt.setError(reply.getError());
                            }

                            detachSecurityGroupFromL3Network(msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());
                            SecurityGroupVO vo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
                            evt.setInventory(SecurityGroupInventory.valueOf(vo));
                            bus.publish(evt);
                            chain.next();
                        }
                    });
                } else {
                    detachSecurityGroupFromL3Network(msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());
                    SecurityGroupVO vo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
                    evt.setInventory(SecurityGroupInventory.valueOf(vo));
                    bus.publish(evt);
                    chain.next();
                }
            }

            @Override
            public String getName() {
                return String.format("detach-security-group-%s-from-l3Network-%s", msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());
            }
        });
    }

    private void handle(APIChangeSecurityGroupStateMsg msg) {
        APIChangeSecurityGroupStateEvent evt = new APIChangeSecurityGroupStateEvent(msg.getId());
        SecurityGroupStateEvent sevt = SecurityGroupStateEvent.valueOf(msg.getStateEvent());
        SecurityGroupVO vo = dbf.findByUuid(msg.getUuid(), SecurityGroupVO.class);
        SecurityGroupState oldState = vo.getState();
        SecurityGroupState sgState = SecurityGroupStateEvent.enable.equals(sevt) ? SecurityGroupState.Enabled : SecurityGroupState.Disabled;

        if (oldState == sgState) {
            evt.setInventory(SecurityGroupInventory.valueOf(vo));
            bus.publish(evt);
            return;
        }

        vo.setState(sgState);
        vo = dbf.updateAndRefresh(vo);

        final SecurityGroupVO finalVO = vo;
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(msg.getUuid());
        if (sdnBackend != null) {
            List<String> vmNicUuids = Q.New(VmNicSecurityGroupRefVO.class)
                    .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                    .eq(VmNicSecurityGroupRefVO_.securityGroupUuid, msg.getUuid())
                    .listValues();
            if (vmNicUuids.isEmpty()) {
                evt.setInventory(SecurityGroupInventory.valueOf(finalVO));
                bus.publish(evt);
                return;
            }

            List<SecurityGroupState> sgStates = new ArrayList<>();
            sgStates.add(SecurityGroupState.Enabled);
            if (sgState == SecurityGroupState.Disabled) {
                sgStates.add(SecurityGroupState.Disabled);
            }
            sdnRefreshVmNics(sdnBackend, vmNicUuids, new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(SecurityGroupInventory.valueOf(finalVO));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    finalVO.setState(oldState);
                    dbf.persist(finalVO);
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });
            return;
        }

        List<String> sgUuids = Q.New(SecurityGroupRuleVO.class).select(SecurityGroupRuleVO_.securityGroupUuid)
                .eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, msg.getUuid()).listValues();
        sgUuids.add(msg.getUuid());
        sgUuids = sgUuids.stream().distinct().collect(Collectors.toList());
        RuleCalculator cal = new RuleCalculator();
        cal.securityGroupUuids = sgUuids;
        cal.vmStates = asList(VmInstanceState.Running);
        List<HostRuleTO> htos = cal.calculate();
        applyRules(htos);

        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(msg.getUuid());
        if (!groupMemberTO.getHostUuids().isEmpty()){
            if (!SecurityGroupStateEvent.enable.equals(sevt)) {
                groupMemberTO.getGroupMembersTO().setActionCode(ACTION_CODE_DELETE_GROUP);
            }
            updateGroupMembers(groupMemberTO);
        }

        evt.setInventory(SecurityGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIAttachSecurityGroupToL3NetworkMsg msg) {
        APIAttachSecurityGroupToL3NetworkEvent evt = new APIAttachSecurityGroupToL3NetworkEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
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
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("attach-security-group-%s-to-l3Network-%s", msg.getSecurityGroupUuid(), msg.getL3NetworkUuid());
            }
        });
    }

    private void removeNicFromSecurityGroup(String sgUuid, List<String> vmNicUuids, Completion completion) {
        SecurityGroupSdnBackend backend = getSdnBackend(sgUuid);
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName( String.format("remove-vm-nic-from-security-group-%s", sgUuid));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in removeNicFromSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "remove-nic-from-security-group-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (vmNicUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids).list();
                        List<VmNicSecurityGroupRefVO> toRemove = refs.stream().filter(ref -> ref.getSecurityGroupUuid().equals(sgUuid)).collect(Collectors.toList());
                        dbf.removeCollection(toRemove, VmNicSecurityGroupRefVO.class);
                        refs.removeAll(toRemove);

                        for (String nicUuid : vmNicUuids) {
                            List<VmNicSecurityGroupRefVO> toUpdate = refs.stream().filter(ref -> ref.getVmNicUuid().equals(nicUuid)).sorted(Comparator.comparingInt(VmNicSecurityGroupRefVO::getPriority)).collect(Collectors.toList());
                            if (!toUpdate.isEmpty()) {
                                toUpdate.stream().forEach(ref ->{
                                    ref.setPriority(toUpdate.indexOf(ref) + 1);
                                });
                                dbf.updateCollection(toUpdate);  
                            }
                        }

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in removeNicFromSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return backend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.vmNicUuids = vmNicUuids;
                        List<HostRuleTO> htos = cal.calculate();
                        applyRules(htos);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "update-group-numbers";

                    @Override
                    public boolean skip(Map data) {
                        return backend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(sgUuid);
                        if(!groupMemberTO.getHostUuids().isEmpty()){
                            if (Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).eq(SecurityGroupVO_.state, SecurityGroupState.Disabled).isExists()) {
                                groupMemberTO.getGroupMembersTO().setActionCode(ACTION_CODE_DELETE_GROUP);
                            }
                            updateGroupMembers(groupMemberTO);
                        }

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "update-sdn-controller";

                    @Override
                    public boolean skip(Map data) {
                        return backend == null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (backend == null || vmNicUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        sdnRemoveSecurityGroupFromVmNic(backend, Collections.singletonList(sgUuid), vmNicUuids, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
                
            }
        }).start();
    }

    private void handle(APIDeleteVmNicFromSecurityGroupMsg msg) {
        APIDeleteVmNicFromSecurityGroupEvent evt = new APIDeleteVmNicFromSecurityGroupEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                RemoveVmNicFromSecurityGroupMsg rmsg = new RemoveVmNicFromSecurityGroupMsg();
                rmsg.setSecurityGroupUuid(msg.getSecurityGroupUuid());
                rmsg.setVmNicUuids(msg.getVmNicUuids());
                bus.makeTargetServiceIdByResourceUuid(rmsg, SecurityGroupConstant.SERVICE_ID, msg.getSecurityGroupUuid());

                bus.send(rmsg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            evt.setError(reply.getError());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("remove-vm-nic-from-security-group-%s", msg.getSecurityGroupUuid());
            }
        });
    }

    private List<String> updateRelatedSecurityGroupRules(String sgUuid) {
        List<SecurityGroupRuleVO> rules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, sgUuid)
                .notEq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid).list();
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<String>> toDelete = new HashMap<>();
        rules.forEach(r -> {
            toDelete.computeIfAbsent(r.getSecurityGroupUuid(), k -> new ArrayList<>());
            toDelete.get(r.getSecurityGroupUuid()).add(r.getUuid());
        });

        for (Map.Entry<String, List<String>> entry : toDelete.entrySet()) {
            doDeleteSecurityGroupRule(entry.getKey(), entry.getValue());
        }

        return new ArrayList<>(toDelete.keySet());
    }

    private void deleteSecurityGroupFromSdn(SecurityGroupSdnBackend backend, String sgUuid, Completion completion) {
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("sdn-backend-delete-security-group-%s", sgUuid));
        chain.setData(data);
        // DEBT: NoRollbackFlow — in deleteSecurityGroupFromSdn
        chain.then(new NoRollbackFlow() {
            String __name__ = "update-security-group-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // step 1, update vmnic
                VmNicSecurityGroupTo delNicTo = new VmNicSecurityGroupTo();
                List<String> vmNicUuids = Q.New(VmNicSecurityGroupRefVO.class)
                        .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                        .eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid).listValues();
                if (!vmNicUuids.isEmpty()) {
                    //step 1.1, delete vmnic <--> security group ref
                    SQL.New(VmNicSecurityGroupRefVO.class)
                            .eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid)
                            .in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids).delete();

                    //step 1.2, update remain security group priority
                    for (String uuid : vmNicUuids) {
                        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class)
                                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, uuid)
                                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list();
                        if (!refs.isEmpty()) {
                            refs.forEach(ref ->{
                                ref.setPriority(refs.indexOf(ref) + 1);
                            });
                            dbf.updateCollection(refs);
                        }
                    }
                }

                // step 2, update related security group
                List<String> relatedSgUuids = updateRelatedSecurityGroupRules(sgUuid);
                if (!relatedSgUuids.isEmpty()) {
                    vmNicUuids.addAll(Q.New(VmNicSecurityGroupRefVO.class)
                            .select(VmNicSecurityGroupRefVO_.vmNicUuid)
                            .in(VmNicSecurityGroupRefVO_.securityGroupUuid, relatedSgUuids)
                            .listValues());
                }
                vmNicUuids = vmNicUuids.stream().distinct().collect(Collectors.toList());

                data.put(VM_NIC_UUIDS, vmNicUuids);

                trigger.next();
            }
        // DEBT: NoRollbackFlow — in deleteSecurityGroupFromSdn
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-from-sdn";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> vmNicUuids = (List<String>) data.get(VM_NIC_UUIDS);
                sdnDeleteSecurityGroup(backend, vmNicUuids, sgUuid, new Completion(trigger) {
                    @Override
                    public void success() {
                        // step 3, remove security group
                        SQL.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).delete();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        // step 3, remove security group
                        SQL.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).delete();
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void deleteSecurityGroup(String sgUuid, Completion completion) {
        SecurityGroupSdnBackend backend = getSdnBackend(sgUuid);
        if (backend != null) {
            deleteSecurityGroupFromSdn(backend, sgUuid, completion);
            return;
        }

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in deleteSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "dettach-nic-from-security-group-in-db";

                    // this flow will refresh vmnic attached by current sg: sgUuid
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> attachedNicUuids = Q.New(VmNicSecurityGroupRefVO.class).select(VmNicSecurityGroupRefVO_.vmNicUuid).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, sgUuid).listValues();

                        RemoveVmNicFromSecurityGroupMsg rmsg = new RemoveVmNicFromSecurityGroupMsg();
                        rmsg.setSecurityGroupUuid(sgUuid);
                        rmsg.setVmNicUuids(attachedNicUuids);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, SecurityGroupConstant.SERVICE_ID, sgUuid);

                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });
                // DEBT: NoRollbackFlow — in deleteSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-all-associated-security-group-rules-in-db";

                    // this flow will find other sgs related to current sg by SecurityGroupRuleVO_.remoteSecurityGroupUuid
                    // action: 1. delete related rules
                    //         2. re-calculate the remain rules priority
                    //         3. find the vmnics related to the other sgs

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<SecurityGroupRuleVO> rules = Q.New(SecurityGroupRuleVO.class)
                                .eq(SecurityGroupRuleVO_.remoteSecurityGroupUuid, sgUuid)
                                .notEq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid).list();
                        if (rules.isEmpty()) {
                            dbf.removeByPrimaryKey(sgUuid, SecurityGroupVO.class);
                            trigger.next();
                            return;
                        }

                        Map<String, List<String>> toDelete = new HashMap<>();
                        rules.forEach(r -> {
                            if (!toDelete.containsKey(r.getSecurityGroupUuid())) {
                                toDelete.put(r.getSecurityGroupUuid(), new ArrayList<String>());
                            }

                            toDelete.get(r.getSecurityGroupUuid()).add(r.getUuid());
                        });

                        List<String> otherNicUuids = new ArrayList<>();
                        for (Map.Entry<String, List<String>> entry : toDelete.entrySet()) {
                            List<String> nicUuids = Q.New(VmNicSecurityGroupRefVO.class).select(VmNicSecurityGroupRefVO_.vmNicUuid).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, entry.getKey()).listValues();
                            otherNicUuids.addAll(nicUuids);
                            doDeleteSecurityGroupRule(entry.getKey(), entry.getValue());
                        }
                        dbf.removeByPrimaryKey(sgUuid, SecurityGroupVO.class);

                        data.put(VM_NIC_UUIDS, otherNicUuids);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    // this flow will refresh the other nic found by previous flow

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> vmNicUuids = (List<String>)data.getOrDefault(VM_NIC_UUIDS, new ArrayList<>());
                        if (!vmNicUuids.isEmpty()) {
                            RuleCalculator cal = new RuleCalculator();
                            cal.vmNicUuids = vmNicUuids;
                            cal.vmStates = asList(VmInstanceState.Running);
                            List<HostRuleTO> htos = cal.calculate();
                            applyRules(htos);
                        }

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
                
            }
        }).start();
    }

    private void handle(APIDeleteSecurityGroupMsg msg) {
        APIDeleteSecurityGroupEvent evt = new APIDeleteSecurityGroupEvent(msg.getId());
        final String issuer = SecurityGroupVO.class.getSimpleName();
        SecurityGroupInventory inv = SecurityGroupInventory.valueOf(dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class));
        final List<SecurityGroupInventory> ctx = Collections.singletonList(inv);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-security-group-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            // DEBT: NoRollbackFlow — reason TBD
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            // DEBT: NoRollbackFlow — reason TBD
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            // DEBT: NoRollbackFlow — reason TBD
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIDeleteSecurityGroupRuleMsg msg) {
        String sgUuid = Q.New(SecurityGroupRuleVO.class).select(SecurityGroupRuleVO_.securityGroupUuid).eq(SecurityGroupRuleVO_.uuid, msg.getRuleUuids().get(0)).findValue();
        APIDeleteSecurityGroupRuleEvent evt = new APIDeleteSecurityGroupRuleEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(sgUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteSecurityGroupRule(msg, sgUuid, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SecurityGroupVO sgvo = dbf.findByUuid(sgUuid, SecurityGroupVO.class);
                        evt.setInventory(SecurityGroupInventory.valueOf(sgvo));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-security-group-%s-rules-%s", sgUuid, Arrays.toString(msg.getRuleUuids().toArray()));
            }
        });
    }

    private void doDeleteSecurityGroupRule(String sgUuid, List<String> ruleUuids) {
        if (ruleUuids.isEmpty()) {
            return;
        }

        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid)
                .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                .list();

        boolean isUpdateIngress = false, isUpdateEgress = false;
        List<SecurityGroupRuleVO> toUpdate = new ArrayList<>();
        for (SecurityGroupRuleVO rvo : rvos) {
            if (ruleUuids.contains(rvo.getUuid())) {
                if (SecurityGroupRuleType.Ingress.equals(rvo.getType())) {
                    isUpdateIngress = true;
                } else {
                    isUpdateEgress = true;
                }
            } else {
                toUpdate.add(rvo);
            }
        }

        dbf.removeByPrimaryKeys(ruleUuids, SecurityGroupRuleVO.class);

        if (isUpdateIngress) {
            List<SecurityGroupRuleVO> ingressToUpdate = toUpdate.stream()
                .filter(rvo -> SecurityGroupRuleType.Ingress.equals(rvo.getType()))
                .sorted(Comparator.comparingInt(SecurityGroupRuleVO::getPriority)).collect(Collectors.toList());
            ingressToUpdate.stream().forEach(r -> {
                r.setPriority(ingressToUpdate.indexOf(r) + 1);
            });
            dbf.updateCollection(ingressToUpdate);
        }

        if (isUpdateEgress) {
            List<SecurityGroupRuleVO> egressToUpdate = toUpdate.stream()
                .filter(rvo -> SecurityGroupRuleType.Egress.equals(rvo.getType()))
                .sorted(Comparator.comparingInt(SecurityGroupRuleVO::getPriority)).collect(Collectors.toList());
            egressToUpdate.stream().forEach(r -> {
                r.setPriority(egressToUpdate.indexOf(r) + 1);
            });
            dbf.updateCollection(egressToUpdate);
        }

        return;
    }

    private void deleteSecurityGroupRule(APIDeleteSecurityGroupRuleMsg msg, String sgUuid, Completion completion) {
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(sgUuid);
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in deleteSecurityGroupRule
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-security-group-rules-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        doDeleteSecurityGroupRule(sgUuid, msg.getRuleUuids());
                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in deleteSecurityGroupRule
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SecurityGroupVO sgvo = dbf.findByUuid(sgUuid, SecurityGroupVO.class);
                        if (SecurityGroupState.Enabled.equals(sgvo.getState())) {
                            RuleCalculator cal = new RuleCalculator();
                            cal.securityGroupUuids = asList(sgUuid);
                            cal.vmStates = asList(VmInstanceState.Running);

                            List<HostRuleTO> htos = cal.calculate();
                            applyRules(htos);
                        }
                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in deleteSecurityGroupRule
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-sdn-controller";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnBackend == null) {
                            trigger.next();
                            return;
                        }

                        sdnRefreshSecurityGroup(sdnBackend, sgUuid, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
                
            }
        }).start();
    }

    private void validate(AddVmNicToSecurityGroupMsg msg) {
        List<String> uuids = Q.New(VmNicVO.class)
                .select(VmNicVO_.uuid)
                .in(VmNicVO_.uuid, msg.getVmNicUuids())
                .listValues();
        if (!new HashSet<>(uuids).containsAll(msg.getVmNicUuids())) {
            msg.getVmNicUuids().removeAll(uuids);
            throw new OperationFailureException(err(ORG_ZSTACK_NETWORK_SECURITYGROUP_10126, SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find vm nics[uuids:%s]", msg.getVmNicUuids()
            ));
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, msg.getSecurityGroupUuid()).list();
        if (!refs.isEmpty()) {
            refs.stream().forEach(ref -> {
                if (uuids.contains(ref.getVmNicUuid())) {
                    throw new OperationFailureException(argerr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10127, "vm nic[uuid:%s] has been attach to security group[uuid:%s]", ref.getVmNicUuid(), msg.getSecurityGroupUuid()));
                }
            });
        }

        String sgOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getSecurityGroupUuid());
        List<VmNicVO> nics = Q.New(VmNicVO.class).in(VmNicVO_.uuid, uuids).list();
        for(VmNicVO nic : nics) {
            if (!Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                    .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE).isExists()) {
                throw new OperationFailureException(argerr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10128, "the netwotk service[type:%s] not enabled on the l3Network[uuid:%s] of nic[uuid:%s]", SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE, nic.getL3NetworkUuid(), nic.getUuid()));
            }

            String vmAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(nic.getVmInstanceUuid());
            if (!AccountConstant.isAdminPermission(sgOwnerAccountUuid) && !AccountConstant.isAdminPermission(vmAccountUuid) && !sgOwnerAccountUuid.equals(vmAccountUuid)) {
                throw new OperationFailureException(argerr(ORG_ZSTACK_NETWORK_SECURITYGROUP_10129, "security group[uuid:%s] is not owned by account[uuid:%s] or admin", msg.getSecurityGroupUuid(), vmAccountUuid));
            }
        }

        msg.setVmNicUuids(uuids);
    }

    private void doAddVmNicToSecurityGroup(String sgUuid, List<String> vmNicUuids) {
        if (vmNicUuids.isEmpty()) {
            return;
        }
        List<VmNicVO> nicvos = Q.New(VmNicVO.class).in(VmNicVO_.uuid, vmNicUuids).list();
        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuids).list();

        List<VmNicSecurityGroupRefVO> toCreateRefs = new ArrayList<VmNicSecurityGroupRefVO>();
        List<VmNicSecurityPolicyVO> toCreatePolicies = new ArrayList<VmNicSecurityPolicyVO>();

        String sgOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(sgUuid);

        for (VmNicVO nic : nicvos) {
            VmNicSecurityGroupRefVO vo = new VmNicSecurityGroupRefVO();

            String vmAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(nic.getVmInstanceUuid());
            if (AccountConstant.isAdminPermission(sgOwnerAccountUuid) && !vmAccountUuid.equals(sgOwnerAccountUuid)) {
                List<VmNicSecurityGroupRefVO> toUpdate = refs.stream().filter(ref -> ref.getVmNicUuid().equals(nic.getUuid())).collect(Collectors.toList());
                if (!toUpdate.isEmpty()) {
                    toUpdate.stream().forEach(r ->{
                        r.setPriority(r.getPriority() + 1);
                    });
                    dbf.updateCollection(toUpdate);
                }
                vo.setPriority(1);
            } else {
                Long count = refs.stream().filter(r -> r.getVmNicUuid().equals(nic.getUuid())).count();
                if (count > 0) {
                    vo.setPriority(count.intValue() + 1);
                } else {
                    vo.setPriority(1);
                }
            }

            vo.setSecurityGroupUuid(sgUuid);
            vo.setVmInstanceUuid(nic.getVmInstanceUuid());
            vo.setVmNicUuid(nic.getUuid());
            vo.setUuid(Platform.getUuid());
            toCreateRefs.add(vo);

            if (!Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, nic.getUuid()).isExists()) {
                VmNicSecurityPolicyVO pvo = new VmNicSecurityPolicyVO();
                pvo.setUuid(Platform.getUuid());
                pvo.setVmNicUuid(nic.getUuid());
                pvo.setIngressPolicy(VmNicSecurityPolicy.DENY.toString());
                pvo.setEgressPolicy(VmNicSecurityPolicy.ALLOW.toString());
                toCreatePolicies.add(pvo);
            }
        }

        dbf.persistCollection(toCreateRefs);
        dbf.persistCollection(toCreatePolicies);
    }

    private void addVmNicToSecurityGroup(String sgUuid, List<String> vmNicUuids, Completion completion) {
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(sgUuid);
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in addVmNicToSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "add-vm-nic-to-security-group-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        doAddVmNicToSecurityGroup(sgUuid, vmNicUuids);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in addVmNicToSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.vmNicUuids = vmNicUuids;
                        List<HostRuleTO> htos = cal.calculate();
                        applyRules(htos);

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — in addVmNicToSecurityGroup
                flow(new NoRollbackFlow() {
                    String __name__ = "update-group-numbers";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        HostSecurityGroupMembersTO groupMemberTO = cal.returnHostSecurityGroupMember(sgUuid);
                        if(!groupMemberTO.getHostUuids().isEmpty()){
                            if (Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).eq(SecurityGroupVO_.state, SecurityGroupState.Disabled).isExists()) {
                                groupMemberTO.getGroupMembersTO().setActionCode(ACTION_CODE_DELETE_GROUP);
                            }
                            updateGroupMembers(groupMemberTO);
                        }

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-sdn-controller";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnBackend == null) {
                            trigger.next();
                            return;
                        }

                        sdnRefreshVmNics(sdnBackend, vmNicUuids, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(AddVmNicToSecurityGroupMsg msg) {
        AddVmNicToSecurityGroupReply reply = new AddVmNicToSecurityGroupReply();

        validate(msg);
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getVmNicSecurityGroupRefSyncThreadName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                addVmNicToSecurityGroup(msg.getSecurityGroupUuid(), msg.getVmNicUuids(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        logger.debug(String.format("successfully added vm nics%s to security group[uuid:%s]", msg.getVmNicUuids(), msg.getSecurityGroupUuid()));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("add-vm-nic-to-security-group-%s", msg.getSecurityGroupUuid());
            }
        });
    }

    private void handle(final APIAddVmNicToSecurityGroupMsg msg) {
        APIAddVmNicToSecurityGroupEvent evt = new APIAddVmNicToSecurityGroupEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                AddVmNicToSecurityGroupMsg amsg = new AddVmNicToSecurityGroupMsg();
                amsg.setSecurityGroupUuid(msg.getSecurityGroupUuid());
                amsg.setVmNicUuids(msg.getVmNicUuids());
                bus.makeTargetServiceIdByResourceUuid(amsg, SecurityGroupConstant.SERVICE_ID, msg.getSecurityGroupUuid());
                bus.send(amsg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            evt.setError(reply.getError());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("add-vm-nic-to-security-group-%s", msg.getSecurityGroupUuid());
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

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSecurityGroupSyncThreadName(msg.getSecurityGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doAddSecurityGroupRule(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        SecurityGroupVO sgvo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
                        evt.setInventory(SecurityGroupInventory.valueOf(sgvo));
                        logger.debug(String.format("successfully add rules to security group[uuid:%s, name:%s]:\n%s", sgvo.getUuid(), sgvo.getName(), JSONObjectUtil.toJsonString(msg.getRules())));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("add-security-group-%s-rule", msg.getSecurityGroupUuid());
            }
        });
    }

    private void doAddSecurityGroupRule(AddSecurityGroupRuleMessage msg,  Completion completion) {
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(msg.getSecurityGroupUuid());
        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("add-security-group[%s]-rules", msg.getSecurityGroupUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // DEBT: NoRollbackFlow — in doAddSecurityGroupRule
                flow(new NoRollbackFlow() {
                    String __name__ = "add-security-group-rule-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Integer priority = msg.getPriority();
                        List<SecurityGroupRuleVO> ruleVOs = Q.New(SecurityGroupRuleVO.class)
                            .eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid())
                            .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY).list();
                        List<SecurityGroupRuleVO> ingressRuleVOs = ruleVOs.stream().filter(r -> SecurityGroupRuleType.Ingress.equals(r.getType())).collect(Collectors.toList());
                        List<SecurityGroupRuleVO> egressRuleVOs = ruleVOs.stream().filter(r -> SecurityGroupRuleType.Egress.equals(r.getType())).collect(Collectors.toList());
                        List<SecurityGroupRuleVO> ingressToCreate = new ArrayList<SecurityGroupRuleVO>();
                        List<SecurityGroupRuleVO> egressToCreate = new ArrayList<SecurityGroupRuleVO>();
                        for (SecurityGroupRuleAO ao : msg.getRules()) {
                            SecurityGroupRuleVO vo = new SecurityGroupRuleVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setSecurityGroupUuid(msg.getSecurityGroupUuid());
                            vo.setDescription(ao.getDescription());
                            vo.setType(SecurityGroupRuleType.valueOf(ao.getType()));
                            vo.setState(SecurityGroupRuleState.valueOf(ao.getState()));
                            vo.setIpVersion(ao.getIpVersion());
                            vo.setPriority(-1);
                            vo.setSrcIpRange(ao.getSrcIpRange());
                            vo.setDstIpRange(ao.getDstIpRange());
                            vo.setDstPortRange(ao.getDstPortRange());
                            vo.setProtocol(SecurityGroupRuleProtocolType.valueOf(ao.getProtocol()));
                            vo.setRemoteSecurityGroupUuid(ao.getRemoteSecurityGroupUuid());
                            vo.setAllowedCidr(ao.getAllowedCidr());
                            vo.setStartPort(ao.getStartPort());
                            vo.setEndPort(ao.getEndPort());
                            vo.setAction(ao.getAction());
                            if (ao.getType().equals(SecurityGroupRuleType.Egress.toString())) {
                                egressToCreate.add(vo);
                            } else {
                                ingressToCreate.add(vo);
                            }
                        }

                        if (!ingressToCreate.isEmpty()) {
                            if (priority == -1) {
                                ingressToCreate.stream().forEach(r -> r.setPriority(ingressRuleVOs.size() + ingressToCreate.indexOf(r) + 1));
                                dbf.persistCollection(ingressToCreate);
                            } else {
                                ingressToCreate.stream().forEach(r -> r.setPriority(priority + ingressToCreate.indexOf(r)));
                                dbf.persistCollection(ingressToCreate);
                                List<SecurityGroupRuleVO> toUpdate = ingressRuleVOs.stream().filter(r -> r.getPriority() >= priority).collect(Collectors.toList());
                                toUpdate.stream().forEach(r -> r.setPriority(r.getPriority() + ingressToCreate.size()));
                                dbf.updateCollection(toUpdate);
                            }
                        }
                        if (!egressToCreate.isEmpty()) {
                            if (priority == -1) {
                                egressToCreate.stream().forEach(r -> r.setPriority(egressRuleVOs.size() + egressToCreate.indexOf(r) + 1));
                                dbf.persistCollection(egressToCreate);
                            } else {
                                egressToCreate.stream().forEach(r -> r.setPriority(priority + egressToCreate.indexOf(r)));
                                dbf.persistCollection(egressToCreate);
                                List<SecurityGroupRuleVO> toUpdate = egressRuleVOs.stream().filter(r -> r.getPriority() >= priority).collect(Collectors.toList());
                                toUpdate.stream().forEach(r -> r.setPriority(r.getPriority() + egressToCreate.size()));
                                dbf.updateCollection(toUpdate);
                            }
                        }

                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-hosts";

                    @Override
                    public boolean skip(Map data) {
                        return sdnBackend != null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RuleCalculator cal = new RuleCalculator();
                        cal.securityGroupUuids = asList(msg.getSecurityGroupUuid());
                        cal.vmStates = asList(VmInstanceState.Running);
                        List<HostRuleTO> htos = cal.calculate();
                        applyRules(htos);
                        trigger.next();
                    }
                });

                // DEBT: NoRollbackFlow — reason TBD
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-rules-on-sdn-controller";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sdnBackend == null) {
                            trigger.next();
                            return;
                        }

                        sdnRefreshSecurityGroup(sdnBackend, msg.getSecurityGroupUuid(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
                
            }
        }).start();
    }

    private void handle(APICreateSecurityGroupMsg msg) {
        APICreateSecurityGroupEvent evt = new APICreateSecurityGroupEvent(msg.getId());

        SecurityGroupVO vo = new SecurityGroupVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(SecurityGroupState.Enabled);
        vo.setvSwitchType(msg.getvSwitchType());
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
        SecurityGroupSdnBackend sdnBackend = getSdnBackend(vo.getUuid());
        if (sdnBackend == null) {
            evt.setInventory(inv);
            logger.debug(String.format("successfully created security group[uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
            bus.publish(evt);
            return;
        }

        sdnBackend.createSecurityGroup(inv, new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(inv);
                logger.debug(String.format("successfully created security group[uuid:%s, name:%s]", inv.getUuid(), inv.getName()));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    public String getId() {
        return bus.makeLocalServiceId(SecurityGroupConstant.SERVICE_ID);
    }

    private void createDefaultRule(String sgUuid, int ipVersion){
        List<SecurityGroupRuleVO> vos = new ArrayList<>();

        SecurityGroupRuleVO ingressRuleVo = new SecurityGroupRuleVO();
        ingressRuleVo.setUuid(Platform.getUuid());
        ingressRuleVo.setSecurityGroupUuid(sgUuid);
        ingressRuleVo.setDescription(SecurityGroupConstant.DEFAULT_RULE_DESCRIPTION);
        ingressRuleVo.setState(SecurityGroupRuleState.Enabled);
        ingressRuleVo.setIpVersion(ipVersion);
        ingressRuleVo.setType(SecurityGroupRuleType.Ingress);
        ingressRuleVo.setPriority(SecurityGroupConstant.DEFAULT_RULE_PRIORITY);
        ingressRuleVo.setAction(SecurityGroupRuleAction.ACCEPT.toString());
        ingressRuleVo.setProtocol(SecurityGroupRuleProtocolType.ALL);
        ingressRuleVo.setRemoteSecurityGroupUuid(sgUuid);
        ingressRuleVo.setAllowedCidr(ipVersion == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
        ingressRuleVo.setStartPort(-1);
        ingressRuleVo.setEndPort(-1);
        vos.add(ingressRuleVo);

        SecurityGroupRuleVO egressRuleVo = new SecurityGroupRuleVO();
        egressRuleVo.setUuid(Platform.getUuid());
        egressRuleVo.setRemoteSecurityGroupUuid(sgUuid);
        egressRuleVo.setDescription(SecurityGroupConstant.DEFAULT_RULE_DESCRIPTION);
        egressRuleVo.setState(SecurityGroupRuleState.Enabled);
        egressRuleVo.setIpVersion(ipVersion);
        egressRuleVo.setType(SecurityGroupRuleType.Egress);
        egressRuleVo.setPriority(SecurityGroupConstant.DEFAULT_RULE_PRIORITY);
        egressRuleVo.setAction(SecurityGroupRuleAction.ACCEPT.toString());
        egressRuleVo.setProtocol(SecurityGroupRuleProtocolType.ALL);
        egressRuleVo.setSecurityGroupUuid(sgUuid);
        egressRuleVo.setAllowedCidr(ipVersion == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
        egressRuleVo.setStartPort(-1);
        egressRuleVo.setEndPort(-1);
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

    private synchronized void restartFailureHostCopingThread() {
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
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void afterMigrateVm(final VmInstanceInventory inv, final String srcHostUuid) {
        RuleCalculator cal = new RuleCalculator();
        cal.vmNicUuids = CollectionUtils.transformToList(inv.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                // security group for ovn network does not care vm migration
                String vswitchType = L3NetworkHelper.getL3networkVSwitchType(arg.getL3NetworkUuid());
                if (VSwitchType.valueOf(vswitchType).isAttachToCluster()) {
                    return arg.getUuid();
                } else {
                    return null;
                }
            }
        });

        if (cal.vmNicUuids.isEmpty()) {
            return;
        }

        // if migrate vm with Storage,the vm stat is migrating
        cal.vmStates = asList(VmInstanceState.Running, VmInstanceState.Migrating);
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
                // security group for ovn network does not care vm migration
                String vswitchType = L3NetworkHelper.getL3networkVSwitchType(arg.getL3NetworkUuid());
                if (VSwitchType.valueOf(vswitchType).isAttachToCluster()) {
                    return arg.getUuid();
                } else {
                    return null;
                }
            }
        });
        if (cal.vmNicUuids.isEmpty()) {
            return;
        }

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

    private SecurityGroupSdnBackend getSdnBackend(String sgUuid) {
        String sdnControllerUuid = SecurityGroupHelper.getSdnControllerUuid(sgUuid);
        if (sdnControllerUuid == null) {
            return null;
        }

        for (SecurityGroupGetSdnBackendExtensionPoint exp : pluginRgty.getExtensionList(SecurityGroupGetSdnBackendExtensionPoint.class)) {
            SecurityGroupSdnBackend backend = exp.getSecurityGroupSdnBackend(sdnControllerUuid);
            if (backend != null) {
                return backend;
            }
        }

        throw new CloudRuntimeException(String.format("can not find security backend for sdn controller[uuid:%s]", sdnControllerUuid));
    }

    private SecurityGroupSdnBackend getSdnBackendFroL3Uuid(String l3Uuid) {
        String sdnControllerUuid = L3NetworkHelper.getSdnControllerUuidFromL3Uuid(l3Uuid);
        if (sdnControllerUuid == null) {
            return null;
        }

        for (SecurityGroupGetSdnBackendExtensionPoint exp : pluginRgty.getExtensionList(SecurityGroupGetSdnBackendExtensionPoint.class)) {
            SecurityGroupSdnBackend backend = exp.getSecurityGroupSdnBackend(sdnControllerUuid);
            if (backend != null) {
                return backend;
            }
        }

        return null;
    }

    @Override
    public void deleteNetworkServiceOfSdnController(String sdnControllerUuid, Completion completion) {
        List<String> sgUuids = Q.New(SecurityGroupVO.class)
                .select(SecurityGroupVO_.uuid).listValues();
        if (sgUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<String> sdnSgUuids = new ArrayList<>();
        for (String uuid : sgUuids) {
            String controllerUuid = SecurityGroupHelper.getSdnControllerUuid(uuid);
            if (sdnControllerUuid.equals(controllerUuid)) {
                sdnSgUuids.add(uuid);
            }
        }
        if (sdnControllerUuid.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(sdnSgUuids).step((uuid, wcomp) -> {
            SecurityGroupDeletionMsg msg = new SecurityGroupDeletionMsg();
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to security group [uuid:%s], %s", msg.getUuid(), reply.getError()));
                    }

                    wcomp.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }
}
