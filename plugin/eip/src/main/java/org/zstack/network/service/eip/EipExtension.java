package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.function.FunctionNoArg;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 */
public class EipExtension extends AbstractNetworkServiceExtension implements Component, ReleaseNetworkServiceOnDeletingNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(EipExtension.class);

    @Autowired
    private EipManager eipMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    L3NetworkManager l3Mgr;

    private static final String SUCCESS = EipExtension.class.getName();

    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return EipConstant.EIP_TYPE;
    }

    @Transactional
    private EipStruct eipVOtoEipStruct(EipVO vo) {
        String sql = "select nic from VmNicVO nic where nic.uuid = :uuid";
        TypedQuery<VmNicVO> q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        q.setParameter("uuid", vo.getVmNicUuid());
        VmNicVO nicvo = q.getSingleResult();

        sql = "select vip from VipVO vip where vip.uuid = :uuid";
        TypedQuery<VipVO> vq = dbf.getEntityManager().createQuery(sql, VipVO.class);
        vq.setParameter("uuid", vo.getVipUuid());
        VipVO vipvo = vq.getSingleResult();

        UsedIpInventory guestIp = eipMgr.getEipGuestIp(vo.getUuid());
        EipStruct struct = eipMgr.generateEipStruct(VmNicInventory.valueOf(nicvo), VipInventory.valueOf(vipvo), EipInventory.valueOf(vo), guestIp);

        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        return struct;
    }

    private EipStruct workOutEipStruct(VmNicInventory vmNic) {
        EipVO eipVO = SQL.New("select eip from EipVO eip, VmNicVO nic, UsedIpVO ip" +
                " where nic.uuid = ip.vmNicUuid " +
                " and ip.l3NetworkUuid = :l3uuid " +
                " and nic.uuid = eip.vmNicUuid and nic.uuid = :nicUuid")
                .param("l3uuid", vmNic.getL3NetworkUuid())
                .param("nicUuid", vmNic.getUuid())
                .find();
        if (eipVO == null) {
            return null;
        }
        return eipVOtoEipStruct(eipVO);
    }

    private boolean isEipShouldBeAttachedToBackend(String vmUuid, String l3Uuid, VmOperation operation) {
        boolean ipChanged = new StaticIpOperator().isIpChange(vmUuid, l3Uuid);

        L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
        boolean l3Need = l3Mgr.applyNetworkServiceWhenVmStateChange(l3Vo.getType());

        /* when vm is destroyed, eip configure will be deleted */
        boolean stateNeed = EipConstant.vmOperationForDetachEip.contains(operation);

        return ipChanged || l3Need || stateNeed;
    }

    private Map<String, List<EipStruct>> workOutEipStruct(VmInstanceSpec spec) {
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> map = getNetworkServiceProviderMap(EipConstant.EIP_TYPE,
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()));
        Map<String, List<EipStruct>> ret = new HashMap<String, List<EipStruct>>();
        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : map.entrySet()) {
            List<EipStruct> structs = new ArrayList<EipStruct>();
            for (final L3NetworkInventory l3 : e.getValue()) {
                if (!isEipShouldBeAttachedToBackend(spec.getVmInventory().getUuid(), l3.getUuid(), spec.getCurrentVmOperation())) {
                    continue;
                }
                final VmNicInventory nic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        if (arg.getUsedIps() != null && !arg.getUsedIps().isEmpty()) {
                            for (UsedIpInventory ip : arg.getUsedIps()) {
                                if (ip.getL3NetworkUuid().equals(l3.getUuid())) {
                                    return arg;
                                }
                            }
                        } else {
                            VmNicVO nic = dbf.findByUuid(arg.getUuid(), VmNicVO.class);
                            for (UsedIpVO ip : nic.getUsedIps()) {
                                if (ip.getL3NetworkUuid().equals(l3.getUuid())) {
                                    return arg;
                                }
                            }
                        }
                        return null;
                    }
                });

                if (nic == null) {
                    continue;
                }

                List<EipVO> evos = new FunctionNoArg<List<EipVO>>() {
                    @Override
                    @Transactional
                    public List<EipVO> call() {
                        String sql = "select eip from EipVO eip, VmNicVO nic, UsedIpVO ip where nic.uuid = ip.vmNicUuid and ip.l3NetworkUuid = :l3uuid and nic.uuid = eip.vmNicUuid and nic.uuid = :nicUuid";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("l3uuid", l3.getUuid());
                        q.setParameter("nicUuid", nic.getUuid());
                        return q.getResultList();
                    }
                }.call();

                if (evos.isEmpty()) {
                    continue;
                }

                for (EipVO evo : evos) {
                    structs.add(eipVOtoEipStruct(evo));
                }
            }
            ret.put(e.getKey().toString(), structs);
        }

        for (WorkOutEipStructureExtensionPoint ext : pluginRgty.getExtensionList(WorkOutEipStructureExtensionPoint.class)) {
            ext.afterWorkOutEipStruct(spec, ret);
        }

        return ret;
    }

    private void applyNetworkService(final String providerType, final Iterator<EipStruct> it, final Map<String, List<EipStruct>> applieds, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final EipStruct struct = it.next();
        eipMgr.attachEip(struct, providerType, new Completion(completion) {
            private void addAppliedStruct() {
                List<EipStruct> s = applieds.computeIfAbsent(providerType, k -> new ArrayList<EipStruct>());
                s.add(struct);
                logger.debug(String.format("successfully applied eip[uuid:%s, ip:%s] for vm nic[uuid:%s]", struct.getEip().getUuid(),
                        struct.getVip().getIp(), struct.getNic().getUuid()));
            }

            @Override
            public void success() {
                addAppliedStruct();
                applyNetworkService(providerType, it, applieds, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyNetworkService(final Iterator<Map.Entry<String, List<EipStruct>>> it, final Map<String, List<EipStruct>> applieds, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        Map.Entry<String, List<EipStruct>> e = it.next();
        String providerType = e.getKey();
        applyNetworkService(providerType, e.getValue().iterator(), applieds, new Completion(completion) {
            @Override
            public void success() {
                applyNetworkService(it, applieds, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion completion) {
        // For new created vm, there is no eip
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            completion.success();
            return;
        }

        Map<String, List<EipStruct>> map = workOutEipStruct(spec);
        Map<String, List<EipStruct>> applieds = new HashMap<String, List<EipStruct>>();
        data.put(SUCCESS, applieds);
        applyNetworkService(map.entrySet().iterator(), applieds, completion);
    }


    private void releaseNetworkService(final Iterator<Map.Entry<String, List<EipStruct>>> it, final boolean detachInDb, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        Map.Entry<String, List<EipStruct>> e = it.next();
        releaseNetworkService(e.getValue().iterator(), e.getKey(), detachInDb, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseNetworkService(it, detachInDb, completion);
            }
        });
    }

    private void releaseNetworkService(final Iterator<EipStruct> it, final String providerType, final boolean detachInDb, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final EipStruct struct = it.next();
        Completion cop = new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released eip[uuid:%s, ip:%s] for vm[uuid:%s, nic uuid:%s]",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getVmInstanceUuid(), struct.getNic().getUuid()));
                releaseNetworkService(it, providerType, detachInDb, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                String err = String.format("failed to release eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider should handle the failure, %s",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode);
                logger.warn(err);
                releaseNetworkService(it, providerType, detachInDb, completion);
            }
        };

        if (detachInDb) {
            eipMgr.detachEipAndUpdateDb(struct, providerType, DetachEipOperation.FORCE_DB_UPDATE, cop);
        } else {
            eipMgr.detachEip(struct, providerType, cop);
        }
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        // For new created vm, there is no eip
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            completion.done();
            return;
        }

        Map<String, List<EipStruct>> map;
        if (data.containsKey(SUCCESS)) {
            map = (Map<String, List<EipStruct>>) data.get(SUCCESS);
        } else {
            map = workOutEipStruct(spec);
            for (Map.Entry<String, List<EipStruct>> e : map.entrySet()) {
                for (EipStruct struct : e.getValue()) {
                    struct.setHostUuid(spec.getDestHost().getUuid());
                }
            }
        }

        if (map.isEmpty()) {
            completion.done();
            return;
        }

        boolean updateDb = spec.getCurrentVmOperation() == VmOperation.Destroy || spec.getCurrentVmOperation() == VmOperation.DetachNic;
        releaseNetworkService(map.entrySet().iterator(), updateDb, completion);
    }

    @Override
    public void releaseNetworkServiceOnDeletingNic(VmNicInventory nic, Completion completion) {
        String networkServiceProviderType = SQL.New("select provider.type from NetworkServiceProviderVO provider, NetworkServiceL3NetworkRefVO ref" +
                " where ref.l3NetworkUuid = :l3Uuid" +
                " and ref.networkServiceProviderUuid = provider.uuid" +
                " and ref.networkServiceType = :networkServiceType")
                .param("l3Uuid", nic.getL3NetworkUuid())
                .param("networkServiceType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                .find();
        EipStruct struct = workOutEipStruct(nic);
        if (struct == null) {
            logger.debug(String.format("vmNic[%s] dont need release eip",nic.getUuid()));
            completion.success();
            return;
        }
        eipMgr.detachEipAndUpdateDb(struct, networkServiceProviderType, DetachEipOperation.FORCE_DB_UPDATE, completion);
    }
}
