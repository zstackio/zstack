package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkDetachStruct;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageDetachStruct;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

/**
 */
public class ApplianceVmCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ApplianceVmCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static String NAME = ApplianceVmVO.class.getSimpleName();


    private static final int OP_NOPE = 0;
    private static final int OP_MIGRATE = 1;
    private static final int OP_DELETION = 2;

    private int toDeleteOpCode(CascadeAction action) {
        if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                return OP_DELETION;
            } else {
                return OP_MIGRATE;
            }
        }

        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer()) && IpRangeVO.class.getSimpleName().equals(action.getRootIssuer())) {
            return OP_DELETION;
        }

        if (ApplianceVmVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        return OP_NOPE;
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE)) {
            handlePrimaryStorageDetach(action, completion);
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            handleL2NetworkDetach(action, completion);
        } else {
            completion.success();
        }
    }

    @Transactional(readOnly = true)
    private List<VmInstanceVO> getVmFromL2NetworkDetached(List<L2NetworkDetachStruct> structs) {
        Set<VmInstanceVO> apvms = new HashSet<>();
        for (L2NetworkDetachStruct s : structs) {
            String sql = "select vm" +
                    " from VmInstanceVO vm, L2NetworkVO l2, L3NetworkVO l3, VmNicVO nic" +
                    " where vm.type = :vmType" +
                    " and vm.clusterUuid = :clusterUuid" +
                    " and vm.state in (:vmStates)" +
                    " and vm.uuid = nic.vmInstanceUuid" +
                    " and nic.l3NetworkUuid = l3.uuid" +
                    " and l3.l2NetworkUuid = l2.uuid" +
                    " and l2.uuid = :l2Uuid";
            TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            q.setParameter("vmType", ApplianceVmConstant.APPLIANCE_VM_TYPE);
            q.setParameter("vmStates", Arrays.asList(
                    VmInstanceState.Running,
                    VmInstanceState.Migrating,
                    VmInstanceState.Starting,
                    VmInstanceState.Rebooting));
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("l2Uuid", s.getL2NetworkUuid());
            apvms.addAll(q.getResultList());
        }

        List<VmInstanceVO> ret = new ArrayList<>(apvms.size());
        ret.addAll(apvms);
        return ret;
    }

    private void migrateOrStopVmOnClusterDetach(final List<VmInstanceVO> toMigrate,
                                                List<String> clusterUuids,
                                                final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        final List<String> avoidHostUuids = q.listValue();
        final List<VmInstanceVO> toDelete = new ArrayList<>();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("handle-appliance-vm-for-cluster-detach"));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                final List<MigrateVmMsg> migrateVmMsgs = CollectionUtils.transformToList(toMigrate,
                        new Function<MigrateVmMsg, VmInstanceVO>() {
                            @Override
                            public MigrateVmMsg call(VmInstanceVO arg) {
                                MigrateVmMsg msg = new MigrateVmMsg();
                                msg.setVmInstanceUuid(arg.getUuid());
                                msg.setAvoidHostUuids(avoidHostUuids);
                                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                return msg;
                            }
                        });

                flow(new NoRollbackFlow() {
                    String __name__ = "migrate-appliance-vm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        bus.send(migrateVmMsgs, 2, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        VmInstanceVO apvm = toMigrate.get(replies.indexOf(r));
                                        toDelete.add(apvm);
                                        logger.warn(String.format("failed to migrate appliance vm[uuid:%s, name:%s], %s. will try to delete it", apvm.getUuid(), r.getError(), apvm.getName()));
                                    }
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-appliance-vm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (toDelete.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<VmInstanceDeletionMsg> msgs = CollectionUtils.transformToList(toDelete, new Function<VmInstanceDeletionMsg, VmInstanceVO>() {
                            @Override
                            public VmInstanceDeletionMsg call(VmInstanceVO arg) {
                                VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
                                msg.setVmInstanceUuid(arg.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                return msg;
                            }
                        });

                        bus.send(msgs, 20, new CloudBusListCallBack(completion) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                for (MessageReply r : replies) {
                                    if (!r.isSuccess()) {
                                        VmInstanceVO apvm = toDelete.get(replies.indexOf(r));
                                        logger.warn(String.format("failed to delete vm[uuid:%s] for cluster detached, %s. However, detaching will go on", apvm.getUuid(), r.getError()));
                                    }
                                }

                                trigger.next();
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

    private void handleL2NetworkDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        final List<VmInstanceVO> apvms = getVmFromL2NetworkDetached(structs);
        if (apvms.isEmpty()) {
            completion.success();
            return;
        }

        List<String> clusterUuids = CollectionUtils.transformToList(structs, new Function<String, L2NetworkDetachStruct>() {
            @Override
            public String call(L2NetworkDetachStruct arg) {
                return arg.getClusterUuid();
            }
        });

        migrateOrStopVmOnClusterDetach(apvms, clusterUuids, completion);
    }

    @Transactional(readOnly = true)
    private List<VmInstanceVO> getVmForPrimaryStorageDetached(List<PrimaryStorageDetachStruct> structs) {
        Set<VmInstanceVO> vms = new HashSet<>();
        for (PrimaryStorageDetachStruct s : structs) {
            String sql = "select vm" +
                    " from VmInstanceVO vm, PrimaryStorageVO ps, VolumeVO vol" +
                    " where vm.type = :vmType" +
                    " and vm.state in (:vmStates)" +
                    " and vm.clusterUuid = :clusterUuid" +
                    " and vm.uuid = vol.vmInstanceUuid" +
                    " and vol.primaryStorageUuid = :psUuid";
            TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            q.setParameter("vmType", ApplianceVmConstant.APPLIANCE_VM_TYPE);
            q.setParameter("vmStates", Arrays.asList(
                    VmInstanceState.Running,
                    VmInstanceState.Starting,
                    VmInstanceState.Migrating,
                    VmInstanceState.Rebooting));
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("psUuid", s.getPrimaryStorageUuid());
            vms.addAll(q.getResultList());
        }

        List<VmInstanceVO> ret = new ArrayList<>(vms.size());
        ret.addAll(vms);
        return ret;
    }

    private void handlePrimaryStorageDetach(CascadeAction action, final Completion completion) {
        List<PrimaryStorageDetachStruct> structs = action.getParentIssuerContext();
        final List<VmInstanceVO> vmInstanceVOs = getVmForPrimaryStorageDetached(structs);
        if (vmInstanceVOs.isEmpty()) {
            completion.success();
            return;
        }

        List<String> clusterUuids = CollectionUtils.transformToList(structs, new Function<String, PrimaryStorageDetachStruct>() {
            @Override
            public String call(PrimaryStorageDetachStruct arg) {
                return arg.getClusterUuid();
            }
        });

        migrateOrStopVmOnClusterDetach(vmInstanceVOs, clusterUuids, completion);
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(ApplianceVmVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        int op = toDeleteOpCode(action);

        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<ApplianceVmInventory> apvms = apvmFromDeleteAction(action);
        if (apvms == null) {
            completion.success();
            return;
        }

        final List<ApplianceVmInventory> apvmToMigrate = new ArrayList<ApplianceVmInventory>();
        final List<ApplianceVmInventory> apvmToDelete = new ArrayList<ApplianceVmInventory>();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-cascade-for-appliance-vm"));

        if (op == OP_MIGRATE) {
            chain.then(new ShareFlow() {
                @Override
                public void setup() {
                    for (ApplianceVmInventory apvm : apvms) {
                        if (VmInstanceState.Running.toString().equals(apvm.getState())) {
                            apvmToMigrate.add(apvm);
                        } else {
                            apvmToDelete.add(apvm);
                        }
                    }

                    List<String> avoidHostUuids = null;
                    if (action.getRootIssuer().equals(ClusterVO.class.getSimpleName())) {
                        List<ClusterInventory> clusters = action.getRootIssuerContext();
                        List<String> clusterUuids = CollectionUtils.transformToList(clusters, new Function<String, ClusterInventory>() {
                            @Override
                            public String call(ClusterInventory arg) {
                                return arg.getUuid();
                            }
                        });
                        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
                        q.select(HostVO_.uuid);
                        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
                        avoidHostUuids = q.listValue();
                    }
                    final List<String> finalAvoidHostUuids = avoidHostUuids;

                    if (!apvmToMigrate.isEmpty()) {
                        flow(new NoRollbackFlow() {
                            String __name__ = "try-migrate-appliancevm";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                final List<GetVmMigrationTargetHostMsg> gmsgs = CollectionUtils.transformToList(apvmToMigrate, new Function<GetVmMigrationTargetHostMsg, ApplianceVmInventory>() {
                                    @Override
                                    public GetVmMigrationTargetHostMsg call(ApplianceVmInventory arg) {
                                        GetVmMigrationTargetHostMsg gmsg = new GetVmMigrationTargetHostMsg();
                                        gmsg.setVmInstanceUuid(arg.getUuid());
                                        if (finalAvoidHostUuids != null) {
                                            gmsg.setAvoidHostUuids(finalAvoidHostUuids);
                                        }
                                        bus.makeTargetServiceIdByResourceUuid(gmsg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                        return gmsg;
                                    }
                                });

                                bus.send(gmsgs, 1, new CloudBusListCallBack(trigger) {
                                    @Override
                                    public void run(List<MessageReply> replies) {
                                        List<ApplianceVmInventory> apvmCannotMigrate = new ArrayList<ApplianceVmInventory>();
                                        for (MessageReply reply : replies) {
                                            if (!reply.isSuccess() || ((GetVmMigrationTargetHostReply) reply).getHosts().isEmpty()) {
                                                ApplianceVmInventory apvm = apvmToMigrate.get(replies.indexOf(reply));
                                                apvmCannotMigrate.add(apvm);
                                            }
                                        }

                                        apvmToMigrate.removeAll(apvmCannotMigrate);
                                        apvmToDelete.addAll(apvmCannotMigrate);
                                        trigger.next();
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "migrate-appliancevm";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                if (apvmToMigrate.isEmpty()) {
                                    trigger.next();
                                    return;
                                }

                                List<MigrateVmMsg> mmsgs = CollectionUtils.transformToList(apvmToMigrate, new Function<MigrateVmMsg, ApplianceVmInventory>() {
                                    @Override
                                    public MigrateVmMsg call(ApplianceVmInventory arg) {
                                        MigrateVmMsg mmsg = new MigrateVmMsg();
                                        mmsg.setVmInstanceUuid(arg.getUuid());
                                        mmsg.setAvoidHostUuids(finalAvoidHostUuids);
                                        bus.makeTargetServiceIdByResourceUuid(mmsg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                        return mmsg;
                                    }
                                });

                                bus.send(mmsgs, 2, new CloudBusListCallBack(trigger) {
                                    @Override
                                    public void run(List<MessageReply> replies) {
                                        for (MessageReply r : replies) {
                                            if (!r.isSuccess()) {
                                                ApplianceVmInventory apvm = apvmToMigrate.get(replies.indexOf(r));
                                                apvmToDelete.add(apvm);
                                            }
                                        }

                                        trigger.next();
                                    }
                                });
                            }
                        });
                    }
                }
            });

        } else if (op == OP_DELETION) {
            apvmToDelete.addAll(apvms);
        }

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-appliancevm";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (apvmToDelete.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<VmInstanceDeletionMsg> msgs = CollectionUtils.transformToList(apvmToDelete, new Function<VmInstanceDeletionMsg, ApplianceVmInventory>() {
                            @Override
                            public VmInstanceDeletionMsg call(ApplianceVmInventory arg) {
                                VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
                                msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
                                msg.setVmInstanceUuid(arg.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg.getUuid());
                                return msg;
                            }
                        });

                        bus.send(msgs, 20, new CloudBusListCallBack(completion) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                                    for (MessageReply r : replies) {
                                        if (!r.isSuccess()) {
                                            trigger.fail(r.getError());
                                            return;
                                        }
                                    }
                                }

                                trigger.next();
                            }
                        });
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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(HostVO.class.getSimpleName(), L3NetworkVO.class.getSimpleName(),
                IpRangeVO.class.getSimpleName(), PrimaryStorageVO.class.getSimpleName(), L2NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Transactional
    private List<ApplianceVmInventory> apvmFromDeleteAction(CascadeAction action) {
        List<ApplianceVmInventory> ret = null;

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> huuids = CollectionUtils.transformToList(hosts, new Function<String, HostInventory>() {
                @Override
                public String call(HostInventory arg) {
                    return arg.getUuid();
                }
            });

            Map<String, ApplianceVmVO> vmvos = new HashMap<String, ApplianceVmVO>();
            SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
            q.add(ApplianceVmVO_.hostUuid, Op.IN, huuids);
            List<ApplianceVmVO> lst = q.list();
            for (ApplianceVmVO vo : lst) {
                vmvos.put(vo.getUuid(), vo);
            }

            if (ClusterVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ClusterInventory> clusters = action.getRootIssuerContext();
                List<String> clusterUuids = CollectionUtils.transformToList(clusters, new Function<String, ClusterInventory>() {
                    @Override
                    public String call(ClusterInventory arg) {
                        return arg.getUuid();
                    }
                });
                q = dbf.createQuery(ApplianceVmVO.class);
                q.add(ApplianceVmVO_.clusterUuid, Op.IN, clusterUuids);
                lst = q.list();
                for (ApplianceVmVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            } else if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ZoneInventory> zones = action.getRootIssuerContext();
                List<String> zoneUuids = CollectionUtils.transformToList(zones, new Function<String, ZoneInventory>() {
                    @Override
                    public String call(ZoneInventory arg) {
                        return arg.getUuid();
                    }
                });
                q = dbf.createQuery(ApplianceVmVO.class);
                q.add(ApplianceVmVO_.zoneUuid, Op.IN, zoneUuids);
                lst = q.list();
                for (ApplianceVmVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            }

            if (!vmvos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vmvos.values());
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> pruuids = CollectionUtils.transformToList((List<PrimaryStorageInventory>) action.getParentIssuerContext(), new Function<String, PrimaryStorageInventory>() {
                @Override
                public String call(PrimaryStorageInventory arg) {
                    return arg.getUuid();
                }
            });

            List<ApplianceVmVO> vmvos = new Callable<List<ApplianceVmVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<ApplianceVmVO> call() {
                    String sql = "select vm from ApplianceVmVO vm, VolumeVO vol, PrimaryStorageVO pr where vm.uuid = vol.vmInstanceUuid" +
                            " and vol.primaryStorageUuid = pr.uuid and vol.type = :volType and pr.uuid in (:uuids)";
                    TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
                    q.setParameter("uuids", pruuids);
                    q.setParameter("volType", VolumeType.Root);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vmvos);
            }
        } else if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L3NetworkInventory> l3s = action.getParentIssuerContext();
            List<String> l3uuids = CollectionUtils.transformToList(l3s, new Function<String, L3NetworkInventory>() {
                @Override
                public String call(L3NetworkInventory arg) {
                    return arg.getUuid();
                }
            });

            String sql = "select apvm from ApplianceVmVO apvm where apvm.uuid in (select nic.vmInstanceUuid from VmNicVO nic where nic.l3NetworkUuid in (:l3Uuids))";
            TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
            q.setParameter("l3Uuids", l3uuids);
            List<ApplianceVmVO> apvms = q.getResultList();
            if (!apvms.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(apvms);
            }
        } else if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> ipruuids = CollectionUtils.transformToList((List<IpRangeInventory>) action.getParentIssuerContext(), new Function<String, IpRangeInventory>() {
                @Override
                public String call(IpRangeInventory arg) {
                    return arg.getUuid();
                }
            });

            List<ApplianceVmVO> vmvos = new Callable<List<ApplianceVmVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<ApplianceVmVO> call() {
                    String sql = "select vm from ApplianceVmVO vm, VmNicVO nic, UsedIpVO ip, IpRangeVO ipr where vm.uuid = nic.vmInstanceUuid" +
                            " and nic.usedIpUuid = ip.uuid and ip.ipRangeUuid = ipr.uuid and ipr.uuid in (:uuids)";
                    TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
                    q.setParameter("uuids", ipruuids);
                    return q.getResultList();
                }
            }.call();

            // find out appliance vm whose ip is gateway of ip range

            final List<String> iprL3Uuids = CollectionUtils.transformToList((List<IpRangeInventory>) action.getParentIssuerContext(), new Function<String, IpRangeInventory>() {
                @Override
                public String call(IpRangeInventory arg) {
                    return arg.getL3NetworkUuid();
                }
            });

            List<ApplianceVmVO> vmvos1 = new Callable<List<ApplianceVmVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<ApplianceVmVO> call() {
                    String sql = "select vm from ApplianceVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid in (:l3uuids)";
                    TypedQuery<ApplianceVmVO> q = dbf.getEntityManager().createQuery(sql, ApplianceVmVO.class);
                    q.setParameter("l3uuids", iprL3Uuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos1.isEmpty()) {
                for (final IpRangeInventory ipr : (List<IpRangeInventory>) action.getParentIssuerContext()) {
                    for (ApplianceVmVO vm : vmvos1) {
                        for (VmNicVO nic : vm.getVmNics()) {
                            if (ipr.getGateway().equals(nic.getIp())) {
                                vmvos.add(vm);
                            }
                        }
                    }
                }
            }

            if (!vmvos.isEmpty()) {
                ret = ApplianceVmInventory.valueOf1(vmvos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            int op = toDeleteOpCode(action);
            if (op == OP_NOPE) {
                return null;
            } else {
                List<ApplianceVmInventory> apvms = apvmFromDeleteAction(action);
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(apvms);
            }
        } else {
            return null;
        }
    }
}
