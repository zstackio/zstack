package org.zstack.network.service.eip;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.*;

/**
 */
public class EipManagerImpl extends AbstractService implements EipManager, VipReleaseExtensionPoint,
        AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, VmPreAttachL3NetworkExtensionPoint,
        VmIpChangedExtensionPoint, ResourceOwnerAfterChangeExtensionPoint, VipGetServiceReferencePoint,
        ManagementNodeReadyExtensionPoint, FilterAttachableL3NetworkExtensionPoint, VmNicChangeNetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(EipManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private L3NetworkManager l3Mgr;
    @Autowired
    protected ThreadFacade thdf;

    private Map<String, EipBackend> backends = new HashMap<>();

    private static List<String> eipAttachableVmTypes = new ArrayList<>();

    private String getThreadSyncSignature(String eipUuid) {
        return String.format("eip-%s", eipUuid);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof EipDeletionMsg) {
            handle((EipDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(EipDeletionMsg msg) {
        EipDeletionReply reply = new EipDeletionReply();
        doDeleteEip(msg.getEipUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateEipMsg) {
            handle((APICreateEipMsg) msg);
        } else if (msg instanceof APIDeleteEipMsg) {
            handle((APIDeleteEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            handle((APIAttachEipMsg) msg);
        } else if (msg instanceof APIDetachEipMsg) {
            handle((APIDetachEipMsg) msg);
        } else if (msg instanceof APIChangeEipStateMsg) {
            handle((APIChangeEipStateMsg) msg);
        } else if (msg instanceof APIGetEipAttachableVmNicsMsg) {
            handle((APIGetEipAttachableVmNicsMsg) msg);
        } else if (msg instanceof APIUpdateEipMsg) {
            handle((APIUpdateEipMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateEipMsg msg) {
        EipVO vo = dbf.findByUuid(msg.getUuid(), EipVO.class);
        boolean update = false;
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

        APIUpdateEipEvent evt = new APIUpdateEipEvent(msg.getId());
        evt.setInventory(EipInventory.valueOf(vo));
        bus.publish(evt);
    }

    private String sqlStringJoin(List<String> elements) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("(");
        int count = 1;
        for (String e : elements) {
            if (count > 1) {
                sqlBuilder.append(",");
            }
            sqlBuilder.append("'").append(e).append("'");
            count ++;
        }
        sqlBuilder.append(")");
        return sqlBuilder.toString();
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> getAttachableVmNicForEip(VipInventory vip, APIGetEipAttachableVmNicsReply reply, APIGetEipAttachableVmNicsMsg msg) {
        String providerType = vip.getServiceProvider();
        List<String> peerL3NetworkUuids = vip.getPeerL3NetworkUuids();
        String zoneUuid = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.zoneUuid)
                .eq(L3NetworkVO_.uuid, vip.getL3NetworkUuid())
                .findValue();
        L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vip.getL3NetworkUuid()).find();
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l3Vo.getL2NetworkUuid())
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids == null || clusterUuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> l3Uuids;

        int vipVersion = IPv6Constants.IPv6;
        List<Integer> l3Versions = new ArrayList<>();
        if (NetworkUtils.isIpv4Address(vip.getIp())) {
            vipVersion = IPv6Constants.IPv4;
            l3Versions.add(IPv6Constants.IPv4);
            l3Versions.add(IPv6Constants.DUAL_STACK);
        } else {
            vipVersion = IPv6Constants.IPv6;
            l3Versions.add(IPv6Constants.IPv6);
            l3Versions.add(IPv6Constants.DUAL_STACK);
        }
        if (providerType != null) {
            // the eip is created on the backend
            l3Uuids = SQL.New("select l3.uuid" +
                    " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO np, L2NetworkClusterRefVO l2ref" +
                    " where l3.system = :system" +
                    " and l3.uuid != :vipL3NetworkUuid" +
                    " and l3.uuid = ref.l3NetworkUuid" +
                    " and l3.ipVersion in (:l3Versions)" +
                    " and ref.networkServiceType = :nsType" +
                    " and l3.zoneUuid = :zoneUuid" +
                    " and np.uuid = ref.networkServiceProviderUuid" +
                    " and np.type = :npType and l3.l2NetworkUuid = l2ref.l2NetworkUuid and l2ref.clusterUuid in :clusterUuids")
                    .param("system", false)
                    .param("zoneUuid", zoneUuid)
                    .param("nsType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                    .param("npType", providerType)
                    .param("vipL3NetworkUuid", vip.getL3NetworkUuid())
                    .param("clusterUuids", clusterUuids)
                    .param("l3Versions", l3Versions)
                    .list();
        } else {
            // the eip is not created on the backend
            l3Uuids = SQL.New("select l3.uuid" +
                    " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, L2NetworkClusterRefVO l2ref" +
                    " where l3.system = :system" +
                    " and l3.uuid != :vipL3NetworkUuid" +
                    " and l3.uuid = ref.l3NetworkUuid" +
                    " and l3.ipVersion in (:l3Versions)" +
                    " and ref.networkServiceType = :nsType" +
                    " and l3.zoneUuid = :zoneUuid and l3.l2NetworkUuid = l2ref.l2NetworkUuid and l2ref.clusterUuid in :clusterUuids")
                    .param("system", false)
                    .param("zoneUuid", zoneUuid)
                    .param("nsType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                    .param("vipL3NetworkUuid", vip.getL3NetworkUuid())
                    .param("clusterUuids", clusterUuids)
                    .param("l3Versions", l3Versions)
                    .list();
        }

        if (peerL3NetworkUuids != null) {
            VmNicVO rnic = Q.New(VmNicVO.class).in(VmNicVO_.l3NetworkUuid, peerL3NetworkUuids)
                    .notNull(VmNicVO_.metaData).limit(1).find();
            if (rnic != null) {
                List<String> vrAttachedL3Uuids = Q.New(VmNicVO.class)
                        .select(VmNicVO_.l3NetworkUuid)
                        .eq(VmNicVO_.vmInstanceUuid, rnic.getVmInstanceUuid())
                        .listValues();
                Set l3UuidSet = new HashSet<>(vrAttachedL3Uuids);
                l3Uuids = l3Uuids.stream().filter(l3UuidSet::contains).collect(Collectors.toList());
            }
        }

        if (l3Uuids.isEmpty()) {
            return new ArrayList<>();
        }

        /*vm has both private l3 and public l3, can not be attachable */
        List<String> vmInPublicL3s = SQL.New("select distinct nic.vmInstanceUuid from UsedIpVO ip, VmNicVO nic" +
                " where nic.uuid = ip.vmNicUuid and ip.l3NetworkUuid = :pubL3")
                .param("pubL3", vip.getL3NetworkUuid()).list();
        vmInPublicL3s = vmInPublicL3s.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());

        List<String> attachableVmStates = EipConstant.attachableVmStates.stream().map(VmInstanceState::toString).collect(Collectors.toList());

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select distinct nic.uuid from VmNicVO nic, VmInstanceVO vm, UsedIpVO ip where nic.uuid = ip.vmNicUuid")
                .append(" and ip.l3NetworkUuid in ").append(sqlStringJoin(l3Uuids))
                .append(" and nic.vmInstanceUuid = vm.uuid and ip.ipVersion = ").append(vipVersion)
                .append(" and vm.type in ('").append(StringUtils.join(eipAttachableVmTypes, "','")).append("')")
                .append(" and vm.state in ").append(sqlStringJoin(attachableVmStates))
                .append(" and nic.ip is not null and vm.clusterUuid is not null");
        if (!vmInPublicL3s.isEmpty()) {
            sqlBuilder.append(" and vm.uuid not in ").append(sqlStringJoin(vmInPublicL3s));
        }
        if (!StringUtils.isEmpty(msg.getVmName())) {
            sqlBuilder.append(" and vm.name like '%").append(msg.getVmName()).append("%\'");
        }
        if (!StringUtils.isEmpty(msg.getVmUuid())) {
            sqlBuilder.append(" and vm.uuid like '%").append(msg.getVmUuid()).append("%\'");
        }

        sqlBuilder.append(" order by nic.vmInstanceUuid")
                .append(" limit ").append(msg.getLimit()).append(" offset ").append(msg.getStart());

        logger.debug("sql is " + sqlBuilder.toString());

        Query q = dbf.getEntityManager().createNativeQuery(sqlBuilder.toString());
        List<String> nicUuids = q.getResultList();

        List<String> unusedVmNicUuids = Q.New(VmNicVO.class).select(VmNicVO_.uuid).in(VmNicVO_.l3NetworkUuid, l3Uuids).isNull(VmNicVO_.vmInstanceUuid).listValues();
        nicUuids.addAll(unusedVmNicUuids);
        if (nicUuids.isEmpty()) {
            reply.setMore(false);
            return new ArrayList<>();
        }

        if (nicUuids.size() >= msg.getLimit()) {
            reply.setStart(msg.getStart() + msg.getLimit());
            reply.setMore(true);
        } else {
            reply.setStart(msg.getStart() + nicUuids.size());
            reply.setMore(false);
        }

        List<VmNicVO> nics = Q.New(VmNicVO.class).in(VmNicVO_.uuid, nicUuids).list();
        List<VmNicInventory> ret;
        if (IPv6NetworkUtils.isIpv6Address(vip.getIp())) {
            ret = l3Mgr.filterVmNicByIpVersion(VmNicInventory.valueOf(nics), IPv6Constants.IPv6);
        } else {
            ret = l3Mgr.filterVmNicByIpVersion(VmNicInventory.valueOf(nics), IPv6Constants.IPv4);
        }
        return ret;
    }

    private List<VmNicInventory> filterVmNicsForEipInVirtualRouterExtensionPoint(VipInventory vip, List<VmNicInventory> vmNics) {
        if (vmNics.isEmpty()){
            return vmNics;
        }

        List<VmNicInventory> ret = new ArrayList<>(vmNics);
        for (FilterVmNicsForEipInVirtualRouterExtensionPoint extp : pluginRgty.getExtensionList(FilterVmNicsForEipInVirtualRouterExtensionPoint.class)) {
            ret = extp.filterVmNicsForEipInVirtualRouter(vip, ret);
        }
        return ret;
    }

    private List<VmNicInventory> filterVmNicsOnFlatNetworkForEip(final String networkProviderType, VipInventory vip, List<VmNicInventory> vmNics) {
        if (vmNics.isEmpty()){
            return vmNics;
        }
        List<String> vmNicL3Uuids  = new ArrayList<>();
        for (VmNicInventory nic : vmNics) {
            vmNicL3Uuids.addAll(VmNicHelper.getL3Uuids(nic));
        }
        List<String> l3 = new ArrayList<>();
        for (GetL3NetworkForEipInVirtualRouterExtensionPoint extp : pluginRgty.getExtensionList(GetL3NetworkForEipInVirtualRouterExtensionPoint.class)) {
            l3.addAll(extp.getL3NetworkForEipInVirtualRouter(networkProviderType, vip, vmNicL3Uuids));
        }

        if (l3.size() > 0) {
            return vmNics.stream().filter(nic -> l3.contains(nic.getL3NetworkUuid())).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> getEipAttachableVmNics(APIGetEipAttachableVmNicsMsg msg, APIGetEipAttachableVmNicsReply reply){
        VipVO vipvo = msg.getEipUuid() == null ?
                Q.New(VipVO.class).eq(VipVO_.uuid, msg.getVipUuid()).find() :
                SQL.New("select vip" +
                        " from VipVO vip, EipVO eip" +
                        " where eip.uuid = :eipUuid" +
                        " and eip.vipUuid = vip.uuid")
                        .param("eipUuid", msg.getEipUuid())
                        .find();
        VipInventory vipInv = VipInventory.valueOf(vipvo);
        List<VmNicInventory> nics = getAttachableVmNicForEip(vipInv, reply, msg);
        if (nics == null || nics.isEmpty()) {
            return nics;
        }

        logger.debug(String.format("get eip[uuid:%s] attachable vm nics[%s] before filter extension point",
                msg.getEipUuid(), nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList())));

        nics = filterVmNicsForEipInVirtualRouterExtensionPoint(vipInv, nics);
        if (nics != null && !nics.isEmpty()) {
            logger.debug(String.format("get eip[uuid:%s] attachable vm nics[%s] after filter extension point",
                    msg.getEipUuid(), nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList())));
        }

        if (nics != null && !msg.isAttachedToVm()) {
            nics = nics.stream().filter(nic -> nic.getVmInstanceUuid() == null).collect(Collectors.toList());
        }

        if (nics != null && msg.getNetworkServiceProvider() != null) {
            nics = filterVmNicsOnFlatNetworkForEip(msg.getNetworkServiceProvider(), vipInv, nics);
        }

        return nics;
    }

    private void handle(APIGetEipAttachableVmNicsMsg msg) {
        APIGetEipAttachableVmNicsReply reply = new APIGetEipAttachableVmNicsReply();
        boolean isAttached = Q.New(EipVO.class).eq(EipVO_.uuid, msg.getEipUuid()).notNull(EipVO_.vmNicUuid).isExists();
        reply.setInventories(isAttached ? new ArrayList<>() : getEipAttachableVmNics(msg, reply));
        bus.reply(msg, reply);
    }

    private void handle(APIChangeEipStateMsg msg) {
        EipVO eip = dbf.findByUuid(msg.getUuid(), EipVO.class);
        eip.setState(eip.getState().nextState(EipStateEvent.valueOf(msg.getStateEvent())));
        eip = dbf.updateAndRefresh(eip);

        APIChangeEipStateEvent evt = new APIChangeEipStateEvent(msg.getId());
        evt.setInventory(EipInventory.valueOf(eip));
        bus.publish(evt);
    }

    private void handle(APIDetachEipMsg msg) {
        final APIDetachEipEvent evt = new APIDetachEipEvent(msg.getId());
        final EipVO vo = dbf.findByUuid(msg.getUuid(), EipVO.class);

        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);

        EipInventory eip = EipInventory.valueOf(vo);
        String l3NetworkUuid = getEipL3Network(nicInventory, eip);

        NetworkServiceProviderType providerType = nwServiceMgr.
                getTypeOfNetworkServiceProviderForService(l3NetworkUuid, EipConstant.EIP_TYPE);

        UsedIpInventory guestIp = getEipGuestIp(eip.getUuid());
        EipStruct struct = generateEipStruct(nicInventory, vipInventory, eip, guestIp);
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        detachEipAndUpdateDb(struct, providerType.toString(), DetachEipOperation.DB_UPDATE, true, new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(EipInventory.valueOf(dbf.reload(vo)));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private boolean checkVmStateBeforeAttachEipToBackend(String l3Uuid) {
        L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
        return l3Mgr.applyNetworkServiceWhenVmStateChange(l3Vo.getType());
    }

    private void handle(final APIAttachEipMsg msg) {
        final APIAttachEipEvent evt = new APIAttachEipEvent(msg.getId());
        final EipVO vo = dbf.findByUuid(msg.getEipUuid(), EipVO.class);

        VmNicVO nicvo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        final VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);
        UsedIpVO guestIp = dbf.findByUuid(msg.getUsedIpUuid(), UsedIpVO.class);

        boolean applyWithCheck = checkVmStateBeforeAttachEipToBackend(guestIp.getL3NetworkUuid());
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (applyWithCheck && EipConstant.noNeedApplyOnBackendVmStates.contains(state)) {
            vo.setVmNicUuid(nicInventory.getUuid());
            vo.setGuestIp(guestIp.getIp());
            EipVO evo = dbf.updateAndRefresh(vo);
            evt.setInventory(EipInventory.valueOf(evo));
            bus.publish(evt);
            return;
        }

        EipInventory eip = EipInventory.valueOf(vo);
        String l3NetworkUuid = getEipL3Network(nicInventory, eip);
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                l3NetworkUuid, EipConstant.EIP_TYPE);
        EipStruct struct = generateEipStruct(nicInventory, vipInventory, eip, UsedIpInventory.valueOf(guestIp));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        attachEip(struct, providerType.toString(), new Completion(msg) {
            @Override
            public void success() {
                vo.setVmNicUuid(nicInventory.getUuid());
                vo.setGuestIp(guestIp.getIp());
                EipVO evo = dbf.updateAndRefresh(vo);
                evt.setInventory(EipInventory.valueOf(evo));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    public EipStruct generateEipStruct(VmNicInventory nic, VipInventory vip, EipInventory eip, UsedIpInventory guestIp) {
        EipStruct struct = new EipStruct();
        struct.setNic(nic);
        struct.setVip(vip);
        struct.setEip(eip);
        if (guestIp != null) {
            /* when delete l3 network, ip range is deleted, then release network, so can not get iprange */
            NormalIpRangeVO ipr = dbf.findByUuid(guestIp.getIpRangeUuid(), NormalIpRangeVO.class);
            if (ipr != null) {
                struct.setGuestIpRange(IpRangeInventory.valueOf(ipr));
            }
            struct.setGuestIp(guestIp);
        }

        return struct;
    }

    public UsedIpInventory getEipGuestIp(String eipUuid) {
        EipVO eip = dbf.findByUuid(eipUuid, EipVO.class);

        if (eip.getVmNicUuid() == null || eip.getGuestIp() == null) {
            return null;
        }

        VmNicVO nic = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);
        for (UsedIpVO ip : nic.getUsedIps()) {
            if (ip.getIp().equals(eip.getGuestIp())) {
                return UsedIpInventory.valueOf(ip);
            }
        }

        return null;
    }

    private String formatDeduplicateEipFlowName(String vmInstanceUuid, String hostUuid) {
        return String.format("%s-%s", vmInstanceUuid, hostUuid);
    }

    private List<Flow> getAdditionalApplyEipForAttachFlow(EipStruct eipStruct, String providerType) {
        List<Flow> flows = new ArrayList<>();
        List<String> deduplicateFlowsName = new ArrayList<>();
        String flowName = formatDeduplicateEipFlowName(eipStruct.getNic().getVmInstanceUuid(), eipStruct.getHostUuid());
        logger.debug("flow name " + flowName);
        deduplicateFlowsName.add(flowName);
        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
            EipStruct as = ext.getAdditionalEipStruct(eipStruct);

            if (as == null) {
                continue;
            }

            flowName = formatDeduplicateEipFlowName(as.getNic().getVmInstanceUuid(), as.getHostUuid());
            logger.debug("flow name " + flowName);
            if (deduplicateFlowsName.contains(flowName)) {
                continue;
            }

            deduplicateFlowsName.add(flowName);

            String temp = flowName;

            flows.add(new Flow() {
                String __name__ = String.format("additional-create-eip-on-backend-for-attach-eip-%s", temp);

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    EipBackend bkd = getEipBackend(providerType);
                    bkd.applyEip(as, new Completion(trigger) {
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

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    EipBackend bkd = getEipBackend(providerType);
                    bkd.revokeEip(as, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.rollback();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                    as.getEip().getUuid(), as.getVip().getIp(), as.getNic().getUuid(), providerType, errorCode));
                            trigger.rollback();
                        }
                    });
                }
            });
        }

        return flows;
    }

    private List<Flow> getAdditionalApplyEipFlow(EipStruct eipStruct, NetworkServiceProviderType providerType) {
        List<Flow> flows = new ArrayList<>();
        List<String> deduplicateFlowsName = new ArrayList<>();
        String flowName = formatDeduplicateEipFlowName(eipStruct.getNic().getVmInstanceUuid(), eipStruct.getHostUuid());
        logger.debug("flow name " + flowName);
        deduplicateFlowsName.add(flowName);
        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
            EipStruct as = ext.getAdditionalEipStruct(eipStruct);

            if (as == null) {
                continue;
            }

            flowName = formatDeduplicateEipFlowName(as.getNic().getVmInstanceUuid(), as.getHostUuid());
            logger.debug("flow name " + flowName);
            if (deduplicateFlowsName.contains(flowName)) {
                continue;
            }

            deduplicateFlowsName.add(flowName);


            String temp = flowName;
            flows.add(new NoRollbackFlow() {
                String __name__ = String.format("additional-apply-eip-on-backend-%s", temp);

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    EipBackend bkd = getEipBackend(providerType.toString());
                    bkd.applyEip(as, new Completion(trigger) {
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

        return flows;
    }

    private List<Flow> getAdditionalDeleteEipFlow(EipStruct eipStruct, NetworkServiceProviderType providerType) {
        List<Flow> flows = new ArrayList<>();
        List<String> deduplicateFlowsName = new ArrayList<>();
        String flowName = formatDeduplicateEipFlowName(eipStruct.getNic().getVmInstanceUuid(), eipStruct.getHostUuid());
        logger.debug("flow name " + flowName);
        deduplicateFlowsName.add(flowName);
        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
            EipStruct as = ext.getAdditionalEipStruct(eipStruct);

            if (as == null) {
                continue;
            }

            flowName = formatDeduplicateEipFlowName(as.getNic().getVmInstanceUuid(), as.getHostUuid());
            logger.debug("flow name " + flowName);
            if (deduplicateFlowsName.contains(flowName)) {
                continue;
            }

            deduplicateFlowsName.add(flowName);

            String temp = flowName;

            flows.add(new NoRollbackFlow() {
                String __name__ = String.format("additional-delete-eip-from-backend-%s", temp);

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    EipBackend bkd = getEipBackend(providerType.toString());
                    bkd.revokeEip(as, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO: add GC instead of failing the API
                            logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                    as.getEip().getUuid(), as.getVip().getIp(), as.getNic().getUuid(), providerType, errorCode));
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        return flows;
    }

    private void doDeleteEip(String eipUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(eipUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (!dbf.isExist(eipUuid, EipVO.class)) {
                    completion.success();
                    chain.next();
                    return;
                }

                deleteEip(eipUuid, new Completion(chain) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("deleteEip-%s", eipUuid);
            }
        });
    }

    private void deleteEip(String eipUuid, Completion completion) {
        final EipVO vo = dbf.findByUuid(eipUuid, EipVO.class);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);

        if (vo.getVmNicUuid() == null) {
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
            struct.setServiceUuid(eipUuid);
            Vip vip = new Vip(vo.getVipUuid());
            vip.setStruct(struct);
            vip.release(new Completion(completion) {
                @Override
                public void success() {
                    dbf.remove(vo);
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });

            return;
        }

        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);

        EipInventory eip = EipInventory.valueOf(vo);
        String l3NetworkUuid = getEipL3Network(nicInventory, eip);
        EipStruct struct = generateEipStruct(nicInventory, vipInventory, eip, getEipGuestIp(eipUuid));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        NetworkServiceProviderType providerType = nwServiceMgr.
                getTypeOfNetworkServiceProviderForService(l3NetworkUuid, EipConstant.EIP_TYPE);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-eip-vmNic-%s-vip-%s", nicvo.getUuid(), vipvo.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-delete-eip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
                            ext.preAttachEip(struct);
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-eip-from-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType.toString());
                        bkd.revokeEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: add GC instead of failing the API
                                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                for (Flow f : getAdditionalDeleteEipFlow(struct, providerType)) {
                    flow(f);
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "release-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        struct.setServiceUuid(eipUuid);
                        Vip vip = new Vip(vipInventory.getUuid());
                        vip.setStruct(struct);
                        vip.release(new Completion(trigger) {
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
                        dbf.remove(vo);
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

    private void handle(APIDeleteEipMsg msg) {
        final APIDeleteEipEvent evt = new APIDeleteEipEvent(msg.getId());

        doDeleteEip(msg.getEipUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });

    }

    private String getEipL3Network(VmNicInventory nic, EipInventory eip){
        String L3NetworkUuid = nic.getL3NetworkUuid();
        if (eip.getGuestIp() != null) {
            for (UsedIpInventory ip : nic.getUsedIps()) {
                if (ip.getIp().equals(eip.getGuestIp())) {
                    L3NetworkUuid = ip.getL3NetworkUuid();
                    break;
                }
            }
        }

        return L3NetworkUuid;
    }

    private void handle(APICreateEipMsg msg) {
        final APICreateEipEvent evt = new APICreateEipEvent(msg.getId());

        EipVO vo = new EipVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setVipUuid(msg.getVipUuid());

        SimpleQuery<VipVO> vipq = dbf.createQuery(VipVO.class);
        vipq.select(VipVO_.ip);
        vipq.add(VipVO_.uuid, Op.EQ, msg.getVipUuid());
        String vipIp = vipq.findValue();
        vo.setVipIp(vipIp);

        vo.setVmNicUuid(msg.getVmNicUuid());
        vo.setState(EipState.Enabled);
        EipVO finalVo1 = vo;
        vo = new SQLBatchWithReturn<EipVO>() {
            @Override
            protected EipVO scripts() {
                finalVo1.setAccountUuid(msg.getSession().getAccountUuid());
                persist(finalVo1);
                reload(finalVo1);
                tagMgr.createTagsFromAPICreateMessage(msg, finalVo1.getUuid(), EipVO.class.getSimpleName());
                return finalVo1;
            }
        }.execute();

        VipVO vipvo = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        final VipInventory vipInventory = VipInventory.valueOf(vipvo);

        if (vo.getVmNicUuid() == null) {
            EipVO finalVo = vo;
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
            struct.setServiceUuid(finalVo.getUuid());
            Vip vip = new Vip(vipInventory.getUuid());
            vip.setStruct(struct);
            vip.acquire(new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(EipInventory.valueOf(finalVo));
                    logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s, ip:%s]",
                            finalVo.getUuid(), finalVo.getName(), vipInventory.getUuid(), vipInventory.getIp()));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });

            return;
        }

        boolean applyWithCheck;
        VmNicVO nicvo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        if (msg.getUsedIpUuid() != null) {
            UsedIpVO guestIp = dbf.findByUuid(msg.getUsedIpUuid(), UsedIpVO.class);
            vo.setGuestIp(guestIp.getIp());
            applyWithCheck = checkVmStateBeforeAttachEipToBackend(guestIp.getL3NetworkUuid());
        } else {
            vo.setGuestIp(nicvo.getIp());
            applyWithCheck = checkVmStateBeforeAttachEipToBackend(nicvo.getL3NetworkUuid());
        }
        vo = dbf.updateAndRefresh(vo);
        final EipInventory retinv = EipInventory.valueOf(vo);

        final VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();

        if (applyWithCheck && state != VmInstanceState.Running) {
            EipVO finalVo = vo;
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
            struct.setServiceUuid(finalVo.getUuid());
            Vip vip = new Vip(vipInventory.getUuid());
            vip.setStruct(struct);
            vip.acquire(new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(EipInventory.valueOf(finalVo));
                    logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s, ip:%s]",
                            finalVo.getUuid(), finalVo.getName(), vipInventory.getUuid(), vipInventory.getIp()));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });

            return;
        }

        final EipVO fevo = vo;
        EipInventory eip = EipInventory.valueOf(vo);
        UsedIpInventory guestIp = getEipGuestIp(eip.getUuid());
        EipStruct struct = generateEipStruct(nicInventory, vipInventory, eip, guestIp);
        String l3NetworkUuid = getEipL3Network(nicInventory, eip);
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3NetworkUuid, EipConstant.EIP_TYPE);
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-eip-vmNic-%s-vip-%s", msg.getVmNicUuid(), msg.getVipUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-create-eip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
                            ext.preAttachEip(struct);
                        }

                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "prepare-vip";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        struct.setPeerL3NetworkUuid(guestIp.getL3NetworkUuid());
                        struct.setServiceProvider(providerType.toString());
                        struct.setServiceUuid(fevo.getUuid());
                        Vip vip = new Vip(vipInventory.getUuid());
                        vip.setStruct(struct);
                        vip.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!s) {
                            trigger.rollback();
                            return;
                        }

                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        struct.setServiceUuid(fevo.getUuid());
                        Vip vip = new Vip(vipInventory.getUuid());
                        vip.setStruct(struct);
                        vip.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(errorCode.toString());
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-eip-on-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType.toString());
                        bkd.applyEip(struct, new Completion(trigger) {
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

                for (Flow f : getAdditionalApplyEipFlow(struct, providerType)) {
                    flow(f);
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(retinv);
                        logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s] for vm nic[uuid:%s]",
                                retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), nicInventory.getUuid()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        dbf.remove(fevo);
                        logger.debug(String.format("failed to create eip[uuid:%s, name:%s] on vip[uuid:%s] for vm nic[uuid:%s], %s",
                                retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), nicInventory.getUuid(), errCode));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(EipConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (EipBackend ext : pluginRgty.getExtensionList(EipBackend.class)) {
            EipBackend old = backends.get(ext.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate EipBackend[%s,%s] for type[%s]", old.getClass().getName(),
                        ext.getClass().getName(), ext.getNetworkServiceProviderType()));
            }
            backends.put(ext.getNetworkServiceProviderType(), ext);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public EipBackend getEipBackend(String providerType) {

        EipBackend bkd = backends.get(providerType);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find EipBackend for type[%s]", providerType));
        }

        return bkd;
    }

    private void doDetachEip(final EipStruct struct, final String providerType, final DetachEipOperation operation, final boolean fromApiMessage, final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(struct.getEip().getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (!dbf.isExist(struct.getEip().getUuid(), EipVO.class)) {
                    completion.fail(operr("eip [uuid:%s] is deleted", struct.getEip().getUuid()));
                    chain.next();
                    return;
                }

                detachEip(struct, providerType, operation, fromApiMessage, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("detachEip-%s", struct.getEip().getUuid());
            }
        });
    }

    private void detachEip(final EipStruct struct, final String providerType, final DetachEipOperation operation, final boolean fromApiMessage, final Completion completion) {
        VmNicInventory nic = struct.getNic();
        final EipInventory eip = struct.getEip();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("detach-eip-%s-vmNic-%s", eip.getUuid(), nic.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-delete-eip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
                            ext.preAttachEip(struct);
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-eip-from-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType);
                        bkd.revokeEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO add GC instead of failing the API
                                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                if (fromApiMessage) {
                    for (Flow f : getAdditionalDeleteEipFlow(struct, NetworkServiceProviderType.valueOf(providerType))) {
                        flow(f);
                    }
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-l3network-from-vip";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct mstruct = new ModifyVipAttributesStruct();
                        mstruct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        mstruct.setServiceUuid(struct.getEip().getUuid());
                        mstruct.setPeerL3NetworkUuid(nic.getL3NetworkUuid());
                        mstruct.setServiceProvider(providerType);
                        Vip vip = new Vip(struct.getVip().getUuid());
                        vip.setStruct(mstruct);
                        vip.stop(new Completion(trigger) {
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
                        if (operation != DetachEipOperation.NO_DB_UPDATE) {
                            SQL.New(EipVO.class).eq(EipVO_.uuid, eip.getUuid()).set(EipVO_.vmNicUuid, null).set(EipVO_.guestIp, null).update();
                        }

                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (operation == DetachEipOperation.FORCE_DB_UPDATE) {
                            SQL.New(EipVO.class).eq(EipVO_.uuid, eip.getUuid()).set(EipVO_.vmNicUuid, null).set(EipVO_.guestIp, null).update();
                        }
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void detachEip(EipStruct struct, String providerType, final Completion completion) {
        doDetachEip(struct, providerType, DetachEipOperation.NO_DB_UPDATE, false, completion);
    }

    public void detachEipAndUpdateDb(EipStruct struct, String providerType, DetachEipOperation dbOperation, boolean fromApiMessage, Completion completion) {
        doDetachEip(struct, providerType, dbOperation, fromApiMessage, completion);
    }

    @Override
    public void detachEipAndUpdateDb(EipStruct struct, String providerType, DetachEipOperation dbOperation, Completion completion) {
        doDetachEip(struct, providerType, dbOperation, false, completion);
    }

    private void doAttachEip(final EipStruct struct, final String providerType, final Completion completion) {
        final EipInventory eip = struct.getEip();
        final VmNicInventory nic = struct.getNic();
        final UsedIpInventory guestIp = struct.getGuestIp();
        if (guestIp == null) {
            /* fix http://jira.zstack.io/browse/ZSTAC-16343 */
            List<String> nicIps = nic.getUsedIps().stream().map(UsedIpInventory::getIp).collect(Collectors.toList());
            completion.fail(operr("cannot find Eip guest ip: %s in vmNic ips :%s", eip.getGuestIp(), nicIps));
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("attach-eip-%s-vmNic-%s", eip.getUuid(), nic.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-create-eip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (AdditionalEipOperationExtensionPoint ext : pluginRgty.getExtensionList(AdditionalEipOperationExtensionPoint.class)) {
                            ext.preAttachEip(struct);
                        }

                        trigger.next();
                    }
                });

                flow(new Flow() {
                    boolean s = false;

                    String __name__ = "acquire-vip-for-attach-eip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType);
                        vipStruct.setPeerL3NetworkUuid(guestIp.getL3NetworkUuid());
                        vipStruct.setServiceUuid(eip.getUuid());
                        Vip vip = new Vip(struct.getVip().getUuid());
                        vip.setStruct(vipStruct);
                        vip.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!s) {
                            trigger.rollback();
                            return;
                        }

                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceUuid(struct.getEip().getUuid());
                        Vip vip = new Vip(struct.getVip().getUuid());
                        vip.setStruct(vipStruct);
                        vip.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(errorCode.toString());
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-eip-on-backend-for-attach-eip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType);
                        bkd.applyEip(struct, new Completion(trigger) {
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

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType);
                        bkd.revokeEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                                trigger.rollback();
                            }
                        });
                    }
                });

                for (Flow f : getAdditionalApplyEipForAttachFlow(struct, providerType)) {
                    flow(f);
                }

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

    @Override
    public void attachEip(final EipStruct struct, final String providerType, final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(struct.getEip().getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (!dbf.isExist(struct.getEip().getUuid(), EipVO.class)) {
                    completion.fail(operr("eip [uuid:%s] is deleted", struct.getEip().getUuid()));
                    chain.next();
                    return;
                }

                doAttachEip(struct, providerType, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attachEip-%s", struct.getEip().getUuid());
            }
        });
    }

    @Override
    public String getVipUse() {
        return EipConstant.EIP_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void releaseServicesOnVip(VipInventory vip, final Completion completion) {
        SimpleQuery<EipVO> eq = dbf.createQuery(EipVO.class);
        eq.add(EipVO_.vipUuid, SimpleQuery.Op.EQ, vip.getUuid());
        final EipVO vo = eq.find();
        if (vo == null || vo.getVmNicUuid() == null) {
            if (vo != null) {
                dbf.remove(vo);
            }
            completion.success();
            return;
        }

        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (VmInstanceState.Stopped == state) {
            dbf.remove(vo);
            completion.success();
            return;
        }

        UsedIpInventory guestIp = null;
        for (UsedIpInventory ip : nicInventory.getUsedIps()) {
            if (ip.getIp().equals(vo.getGuestIp())) {
                guestIp = ip;
            }
        }
        String peerL3;
        if (guestIp == null) {
            peerL3 = nicInventory.getL3NetworkUuid();
        } else {
            peerL3 = guestIp.getL3NetworkUuid();
        }
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(peerL3, EipConstant.EIP_TYPE);
        EipStruct struct = new EipStruct();
        struct.setVip(vip);
        struct.setNic(nicInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));

        EipBackend bkd = getEipBackend(providerType.toString());
        bkd.revokeEip(struct, new Completion(completion) {
            @Override
            public void success() {
                dbf.remove(vo);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<>();

        ExpandedQueryStruct struct = new ExpandedQueryStruct();
        struct.setInventoryClassToExpand(VmNicInventory.class);
        struct.setExpandedField("eip");
        struct.setInventoryClass(EipInventory.class);
        struct.setForeignKey("uuid");
        struct.setExpandedInventoryKey("vmNicUuid");
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setInventoryClassToExpand(VipInventory.class);
        struct.setExpandedField("eip");
        struct.setInventoryClass(EipInventory.class);
        struct.setForeignKey("uuid");
        struct.setExpandedInventoryKey("vipUuid");
        structs.add(struct);

        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        return null;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateEipMsg) {
                        check((APICreateEipMsg) msg, pairs);
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                } else {
                    if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(EipQuotaConstant.EIP_NUM);
                usage.setUsed(getUsedEipNum(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedEipNum(String accountUuid) {
                String sql = "select count(eip)" +
                        " from EipVO eip, AccountResourceRefVO ref" +
                        " where ref.resourceUuid = eip.uuid" +
                        " and ref.accountUuid = :auuid" +
                        " and ref.resourceType = :rtype";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", EipVO.class.getSimpleName());
                Long usedEipNum = q.getSingleResult();
                usedEipNum = usedEipNum == null ? 0 : usedEipNum;
                return usedEipNum;
            }

            @Transactional(readOnly = true)
            private long getVmEipNum(String vmUuid) {
                String sql = "select count(eip)" +
                        " from EipVO eip, VmNicVO vmnic" +
                        " where vmnic.vmInstanceUuid = :vmuuid" +
                        " and vmnic.uuid = eip.vmNicUuid";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("vmuuid", vmUuid);
                Long vmEipNum = q.getSingleResult();
                vmEipNum = vmEipNum == null ? 0 : vmEipNum;
                return vmEipNum;
            }

            @Transactional(readOnly = true)
            private void check(APICreateEipMsg msg, Map<String, QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                long eipNumQuota = pairs.get(EipQuotaConstant.EIP_NUM).getValue();
                long usedEipNum = getUsedEipNum(msg.getSession().getAccountUuid());
                long askedEipNum = 1;

                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                quotaCompareInfo.quotaName = EipQuotaConstant.EIP_NUM;
                quotaCompareInfo.quotaValue = eipNumQuota;
                quotaCompareInfo.currentUsed = usedEipNum;
                quotaCompareInfo.request = askedEipNum;
                new QuotaUtil().CheckQuota(quotaCompareInfo);
            }

            @Transactional(readOnly = true)
            private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
                    return;
                }

                SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
                q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, msg.getResourceUuid());
                AccountResourceRefVO accResRefVO = q.find();


                if (accResRefVO.getResourceType().equals(VmInstanceVO.class.getSimpleName())) {
                    long eipNumQuota = pairs.get(EipQuotaConstant.EIP_NUM).getValue();
                    long usedEipNum = getUsedEipNum(resourceTargetOwnerAccountUuid);
                    long askedEipNum = getVmEipNum(msg.getResourceUuid());

                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = EipQuotaConstant.EIP_NUM;
                    quotaCompareInfo.quotaValue = eipNumQuota;
                    quotaCompareInfo.currentUsed = usedEipNum;
                    quotaCompareInfo.request = askedEipNum;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateEipMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(EipQuotaConstant.EIP_NUM);
        p.setValue(EipQuotaGlobalConfig.EIP_NUM.defaultValue(Long.class));
        quota.addPair(p);

        return list(quota);
    }

    @Override
    public void vmPreAttachL3Network(final VmInstanceInventory vm, final L3NetworkInventory l3) {
        final List<String> nicUuids = CollectionUtils.transformToList(vm.getVmNics(),
                VmNicInventory::getUuid);

        if (nicUuids.isEmpty()) {
            return;
        }

        new Runnable() {
            @Override
            @Transactional(readOnly = true)
            public void run() {
                String sql = "select count(*)" +
                        " from EipVO eip, VipVO vip" +
                        " where eip.vipUuid = vip.uuid" +
                        " and vip.l3NetworkUuid = :l3Uuid" +
                        " and eip.vmNicUuid in (:nicUuids)";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("l3Uuid", l3.getUuid());
                q.setParameter("nicUuids", nicUuids);
                Long count = q.getSingleResult();
                if (count > 0) {
                    throw new OperationFailureException(operr("unable to attach the L3 network[uuid:%s, name:%s] to the vm[uuid:%s, name:%s]," +
                                    " because the L3 network is providing EIP to one of the vm's nic",
                            l3.getUuid(), l3.getName(), vm.getUuid(), vm.getName()));
                }
            }
        }.run();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<L3NetworkInventory> filterAttachableL3Network(VmInstanceInventory vm, List<L3NetworkInventory> l3s) {
        final List<String> nicUuids = CollectionUtils.transformToList(vm.getVmNics(),
            VmNicInventory::getUuid);
    
        List<L3NetworkInventory> rets = new ArrayList<>(l3s);
        if (nicUuids.isEmpty()) {
            return rets;
        }
        
        Set<String> l3Uuids = SQL.New("select vip.l3NetworkUuid from EipVO eip left join VipVO vip " +
                        "on eip.vipUuid = vip.uuid where eip.vmNicUuid in (:nicUuids)")
                .param("nicUuids", nicUuids)
                .list()
                .stream().map(Objects::toString).collect(Collectors.toSet());
        
        rets.removeIf(l3 -> l3Uuids.contains(l3.getUuid()));
        return rets;
    }

    @Override
    public void vmIpChanged(VmInstanceInventory vm, VmNicInventory nic, Map<Integer, UsedIpInventory> oldIpMap, Map<Integer, UsedIpInventory> newIpMap) {
        for (Map.Entry<Integer, UsedIpInventory> oldIp : oldIpMap.entrySet()) {
            SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
            q.add(EipVO_.vmNicUuid, Op.EQ, nic.getUuid());
            q.add(EipVO_.guestIp, Op.EQ, oldIp.getValue().getIp());
            EipVO eip = q.find();

            if (eip == null) {
                return;
            }

            UsedIpInventory newIp = newIpMap.get(oldIp.getKey());
            eip.setGuestIp(newIp.getIp());
            dbf.update(eip);

            logger.debug(String.format("update the EIP[uuid:%s, name:%s]'s guest IP from %s to %s for the nic[uuid:%s]",
                    eip.getUuid(), eip.getName(), oldIp.getValue().getIp(), newIp.getIp(), nic.getUuid()));
        }
    }


    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        changeEipOwner(ref, newOwnerUuid);
    }

    @Transactional
    private void changeEipOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        String sql = "select eip.uuid" +
                " from VmInstanceVO vm, VmNicVO nic, EipVO eip" +
                " where vm.uuid = nic.vmInstanceUuid" +
                " and nic.uuid = eip.vmNicUuid" +
                " and vm.uuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", ref.getResourceUuid());
        List<String> eipUuids = q.getResultList();
        if (eipUuids.isEmpty()) {
            logger.debug(String.format("Vm[uuid:%s] doesn't have any eip, there is no need to change owner of eip.",
                    ref.getResourceUuid()));
            return;
        }

        for (String uuid : eipUuids) {
            acntMgr.changeResourceOwner(uuid, newOwnerUuid);
        }
    }

    @Override
    public ServiceReference getServiceReference(String vipUuid) {
        List<String> uuids = Q.New(EipVO.class).select(EipVO_.uuid).eq(EipVO_.vipUuid, vipUuid).notNull(EipVO_.vmNicUuid).listValues();
        if (uuids == null || uuids.isEmpty()) {
            return new VipGetServiceReferencePoint.ServiceReference(EipConstant.EIP_NETWORK_SERVICE_TYPE, 0, new ArrayList<>());
        }
        return new VipGetServiceReferencePoint.ServiceReference(EipConstant.EIP_NETWORK_SERVICE_TYPE, uuids.size(), uuids);
    }

    @Override
    public ServiceReference getServicePeerL3Reference(String vipUuid, String peerL3Uuid) {
        List<String> uuids = SQL.New("select eip.uuid from EipVO eip, VmNicVO nic where eip.vipUuid = :vipUuid " +
            " and eip.vmNicUuid = nic.uuid and nic.l3NetworkUuid = :peerL3Uuid")
           .param("vipUuid", vipUuid).param("peerL3Uuid", peerL3Uuid).list();

        if (uuids == null || uuids.isEmpty()) {
            return new VipGetServiceReferencePoint.ServiceReference(EipConstant.EIP_NETWORK_SERVICE_TYPE, 0, new ArrayList<>());
        }
        return new VipGetServiceReferencePoint.ServiceReference(EipConstant.EIP_NETWORK_SERVICE_TYPE, uuids.size(), uuids);
    }

    @Override
    public void managementNodeReady() {
        eipAttachableVmTypes.add(VmInstanceConstant.USER_VM_TYPE);

        for (GetEipAttachableVmNicsExtensionPoint ext : pluginRgty.getExtensionList(GetEipAttachableVmNicsExtensionPoint.class)) {
            eipAttachableVmTypes.add(ext.getAdditionalVmState());
        }
    }

    @Override
    public Map<String, String> getVmNicAttachedNetworkService(VmNicInventory nic) {
        List<String> eipUuids = Q.New(EipVO.class).select(EipVO_.uuid).eq(EipVO_.vmNicUuid, nic.getUuid()).listValues();
        if (eipUuids.isEmpty()) {
            return null;
        }
        HashMap<String, String> ret = new HashMap<>();
        for (String eipUuid : eipUuids) {
            ret.put(EipConstant.EIP_NETWORK_SERVICE_TYPE, eipUuid);
        }
        return ret;
    }
}
