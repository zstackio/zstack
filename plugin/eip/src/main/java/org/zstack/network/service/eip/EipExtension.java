package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
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
public class EipExtension extends AbstractNetworkServiceExtension implements Component {
    private static final CLogger logger = Utils.getLogger(EipExtension.class);

    @Autowired
    private EipManager eipMgr;

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

        EipStruct struct = new EipStruct();
        struct.setNic(VmNicInventory.valueOf(nicvo));
        struct.setVip(VipInventory.valueOf(vipvo));
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        return struct;
    }

    private Map<String, List<EipStruct>> workOutEipStruct(VmInstanceSpec spec) {
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> map = getNetworkServiceProviderMap(EipConstant.EIP_TYPE, spec.getL3Networks());
        Map<String, List<EipStruct>> ret = new HashMap<String, List<EipStruct>>();
        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : map.entrySet()) {
            List<EipStruct> structs = new ArrayList<EipStruct>();
            for (final L3NetworkInventory l3 : e.getValue()) {
                final VmNicInventory nic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        if (arg.getL3NetworkUuid().equals(l3.getUuid())) {
                            return arg;
                        }
                        return null;
                    }
                });

                List<EipVO> evos = new FunctionNoArg<List<EipVO>>() {
                    @Override
                    @Transactional
                    public List<EipVO> call() {
                        String sql = "select eip from EipVO eip, VmNicVO nic where nic.l3NetworkUuid = :l3uuid and nic.uuid = eip.vmNicUuid and nic.uuid = :nicUuid";
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
                List<EipStruct> s = applieds.get(providerType);
                if (s == null) {
                    s = new ArrayList<EipStruct>();
                    applieds.put(providerType, s);
                }
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
            eipMgr.detachEipAndUpdateDb(struct, providerType, cop);
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

        Map<String, List<EipStruct>> map = null;
        if (data.containsKey(SUCCESS)) {
            map = (Map<String, List<EipStruct>>) data.get(SUCCESS);
        } else {
            map = workOutEipStruct(spec);
        }

        if (map.isEmpty()) {
            completion.done();
            return;
        }

        boolean updateDb = spec.getCurrentVmOperation() == VmOperation.Destroy || spec.getCurrentVmOperation() == VmOperation.DetachNic;
        releaseNetworkService(map.entrySet().iterator(), updateDb, completion);
    }
}
