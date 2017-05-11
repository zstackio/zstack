package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.notification.N;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkDetachStruct;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 */
public class VmCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VmCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VmInstanceDeletionPolicyManager deletionPolicyManager;

    private static final String NAME = VmInstanceVO.class.getSimpleName();

    private static final int OP_NOPE = 0;
    private static final int OP_STOP = 1;
    private static final int OP_DELETION = 2;
    private static final int OP_REMOVE_INSTANCE_OFFERING = 3;
    private static final int OP_DETACH_NIC = 4;

    private int toDeletionOpCode(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return OP_NOPE;
        }

        if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                return OP_DELETION;
            } else {
                return OP_STOP;
            }
        }

        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH_NIC;
        }

        if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer()) &&
                IpRangeVO.class.getSimpleName().equals(action.getRootIssuer())) {
            return OP_STOP;
        }

        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (InstanceOfferingVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_REMOVE_INSTANCE_OFFERING;
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
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
    private List<DetachNicFromVmMsg> getVmNicDetachMsgs(List<L2NetworkDetachStruct> structs) {
        List<DetachNicFromVmMsg> dmsgs  = new ArrayList<>();

        for (L2NetworkDetachStruct s : structs) {
            String sql = "select vm.uuid, nic.uuid from VmInstanceVO vm, VmNicVO nic, L3NetworkVO l3"
                    + " where vm.clusterUuid = :clusterUuid"
                    + " and l3.l2NetworkUuid = :l2NetworkUuid"
                    + " and nic.l3NetworkUuid = l3.uuid "
                    + " and nic.vmInstanceUuid = vm.uuid";
            TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("l2NetworkUuid", s.getL2NetworkUuid());
            dmsgs.addAll(q.getResultList().stream().map((t) -> {
                DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                msg.setVmInstanceUuid(t.get(0, String.class));
                msg.setVmNicUuid(t.get(1, String.class));
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                return msg;
            }).collect(Collectors.toList()));
        }

        return dmsgs;
    }

    private void handleL2NetworkDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        final List<DetachNicFromVmMsg> dmsgs = getVmNicDetachMsgs(structs);
        if (dmsgs.isEmpty()) {
            completion.success();
            return;
        }

        bus.send(dmsgs, 20, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        DetachNicFromVmMsg msg = dmsgs.get(replies.indexOf(r));
                        logger.warn(String.format("failed to stop vm[uuid:%s] for l2Network detached, %s." +
                                " However, detaching will go on", msg.getVmInstanceUuid(), r.getError()));
                    }
                }

                completion.success();
            }
        });
    }

    @Transactional(readOnly = true)
    private List<String> getVmUuidForPrimaryStorageDetached(List<PrimaryStorageDetachStruct> structs) {
        List<String> vmUuids = new ArrayList<>();
        for (PrimaryStorageDetachStruct s : structs) {
            String sql = "select vm.uuid" +
                    " from VmInstanceVO vm, PrimaryStorageVO ps, VolumeVO vol" +
                    " where vm.type = :vmType" +
                    " and vm.state in (:vmStates)" +
                    " and vm.clusterUuid = :clusterUuid" +
                    " and vm.uuid = vol.vmInstanceUuid" +
                    " and vol.primaryStorageUuid = :psUuid";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
            q.setParameter("vmStates", Arrays.asList(
                    VmInstanceState.Stopped,
                    VmInstanceState.Unknown,
                    VmInstanceState.Running));
            q.setParameter("clusterUuid", s.getClusterUuid());
            q.setParameter("psUuid", s.getPrimaryStorageUuid());
            vmUuids.addAll(q.getResultList());
        }

        return vmUuids;
    }

    private void handlePrimaryStorageDetach(CascadeAction action, final Completion completion) {
        List<PrimaryStorageDetachStruct> structs = action.getParentIssuerContext();
        final List<String> vmUuids = getVmUuidForPrimaryStorageDetached(structs);
        if (vmUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<StopVmInstanceMsg> msgs = CollectionUtils.transformToList(vmUuids, new Function<StopVmInstanceMsg, String>() {
            @Override
            public StopVmInstanceMsg call(String arg) {
                StopVmInstanceMsg msg = new StopVmInstanceMsg();
                msg.setVmInstanceUuid(arg);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, 20, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        String vmUuid = vmUuids.get(replies.indexOf(r));
                        logger.warn(String.format("failed to stop vm[uuid:%s] for primary storage detached, %s." +
                                " However, detaching will go on", vmUuid, r.getError()));
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(VmInstanceVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        if (op == OP_REMOVE_INSTANCE_OFFERING) {
            if (VmGlobalConfig.UPDATE_INSTANCE_OFFERING_TO_NULL_WHEN_DELETING.value(Boolean.class)) {
                new Runnable() {
                    @Override
                    @Transactional
                    public void run() {
                        List<InstanceOfferingInventory> offerings = action.getParentIssuerContext();
                        List<String> offeringUuids = CollectionUtils.transformToList(offerings,
                                new Function<String, InstanceOfferingInventory>() {
                                    @Override
                                    public String call(InstanceOfferingInventory arg) {
                                        return arg.getUuid();
                                    }
                                });

                        String sql = "update VmInstanceVO vm" +
                                " set vm.instanceOfferingUuid = null" +
                                " where vm.instanceOfferingUuid in (:offeringUuids)";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("offeringUuids", offeringUuids);
                        q.executeUpdate();
                    }
                }.run();
            }

            completion.success();
            return;
        }

        final List<VmDeletionStruct> vminvs = vmFromDeleteAction(action);
        if (vminvs == null || vminvs.isEmpty()) {
            completion.success();
            return;
        }

        if (op == OP_STOP) {
            List<StopVmInstanceMsg> msgs = new ArrayList<>();
            List<String> vmStateCanStop = Arrays.asList(
                    VmInstanceState.Unknown.toString(),
                    VmInstanceState.Stopped.toString(),
                    VmInstanceState.Running.toString());
            for (VmDeletionStruct inv : vminvs) {
                if (!vmStateCanStop.stream().anyMatch(
                        str -> str.trim().equals(inv.getInventory().getState()))) {
                    continue;
                }
                StopVmInstanceMsg msg = new StopVmInstanceMsg();
                msg.setVmInstanceUuid(inv.getInventory().getUuid());
                msg.setGcOnFailure(true);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                msgs.add(msg);
            }

            if (msgs.isEmpty()) {
                completion.success();
                return;
            }

            bus.send(msgs, 20, new CloudBusListCallBack(completion) {
                @Override
                public void run(List<MessageReply> replies) {
                    if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                completion.fail(r.getError());
                                return;
                            }
                        }
                    }

                    completion.success();
                }
            });
        } else if (op == OP_DELETION) {
            new While<>(vminvs).all((inv, noErrorCompletion) -> {
                VmInstanceDeletionMsg msg = new VmInstanceDeletionMsg();
                // Upon primary storage deletion, the VM instance records will be deleted
                // accordingly.  However, the VMs are still kept in their hosts.
                if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
                    msg.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.KeepVolume.toString());
                }
                msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
                msg.setVmInstanceUuid(inv.getInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                bus.send(msg, new CloudBusCallBack(noErrorCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                            if (!reply.isSuccess()) {
                                // TODO
                                logger.warn(reply.getError().toString());
                            }
                        }

                        noErrorCompletion.done();
                    }
                });
            }).run(new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                        dbf.removeByPrimaryKeys(vminvs
                                        .stream()
                                        .map(p -> p.getInventory().getUuid())
                                        .collect(Collectors.toList()),
                                VmInstanceVO.class);
                    }

                    completion.success();
                }
            });
        } else if (op == OP_DETACH_NIC) {
            final List<DetachNicFromVmMsg> msgs = new ArrayList<>();
            List<L3NetworkInventory> l3s = action.getParentIssuerContext();
            for (VmDeletionStruct vm : vminvs) {
                for (L3NetworkInventory l3 : l3s) {
                    VmNicInventory nic = vm.getInventory().findNic(l3.getUuid());
                    if (nic == null) {
                        continue;
                    }

                    DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                    msg.setVmInstanceUuid(vm.getInventory().getUuid());
                    msg.setVmNicUuid(nic.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.getInventory().getUuid());
                    msgs.add(msg);
                }
            }

            bus.send(msgs, new CloudBusListCallBack(completion) {
                @Override
                public void run(List<MessageReply> replies) {
                    List<String> vmNicUuids = new ArrayList<String>();

                    for (MessageReply r : replies) {
                        DetachNicFromVmMsg msg = msgs.get(replies.indexOf(r));
                        if (!r.isSuccess()) {
                            N.New(VmInstanceVO.class, msg.getVmInstanceUuid()).warn_("unable to detach a nic[uuid:%s] from the vm[uuid:%s], %s", msg.getVmNicUuid(), msg.getVmInstanceUuid(), r.getError());
                            logger.warn(String.format("failed to detach nic[uuid:%s] from the vm[uuid:%s], %s",
                                    msg.getVmNicUuid(), msg.getVmInstanceUuid(), r.getError()));
                        } else {
                            vmNicUuids.add(msg.getVmNicUuid());
                        }
                    }

                    if (action.getParentIssuer().equals(L3NetworkVO.class.getSimpleName())) {
                        if (!vmNicUuids.isEmpty()) {
                            UpdateQuery q = UpdateQuery.New(AccountResourceRefVO.class)
                                    .condAnd(AccountResourceRefVO_.resourceUuid, Op.IN, vmNicUuids)
                                    .condAnd(AccountResourceRefVO_.resourceType, Op.EQ, VmNicVO.class.getSimpleName());
                            q.delete();
                        }
                    }

                    completion.success();
                }
            });
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE || op == OP_STOP) {
            completion.success();
            return;
        }

        List<VmDeletionStruct> vminvs = vmFromDeleteAction(action);
        if (vminvs == null) {
            completion.success();
            return;
        }

        for (VmDeletionStruct inv : vminvs) {
            ErrorCode err = extEmitter.preDestroyVm(inv.getInventory());
            if (err != null) {
                completion.fail(err);
                return;
            }
        }

        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                HostVO.class.getSimpleName(),
                L3NetworkVO.class.getSimpleName(),
                IpRangeVO.class.getSimpleName(),
                PrimaryStorageVO.class.getSimpleName(),
                L2NetworkVO.class.getSimpleName(),
                InstanceOfferingVO.class.getSimpleName(),
                AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<VmDeletionStruct> toVmDeletionStruct(Collection<VmInstanceVO> vos) {
        List<VmDeletionStruct> structs = new ArrayList<>();
        for (VmInstanceVO vo : vos) {
            VmDeletionStruct s = new VmDeletionStruct();
            s.setInventory(VmInstanceInventory.valueOf(vo));
            s.setDeletionPolicy(deletionPolicyManager.getDeletionPolicy(vo.getUuid()));
            structs.add(s);
        }
        return structs;
    }

    private List<VmDeletionStruct> vmFromDeleteAction(CascadeAction action) {
        List<VmDeletionStruct> ret = null;
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> huuids = CollectionUtils.transformToList(hosts, new Function<String, HostInventory>() {
                @Override
                public String call(HostInventory arg) {
                    return arg.getUuid();
                }
            });

            Map<String, VmInstanceVO> vmvos = new HashMap<>();
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.add(VmInstanceVO_.hostUuid, SimpleQuery.Op.IN, huuids);
            q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
            List<VmInstanceVO> lst = q.list();
            for (VmInstanceVO vo : lst) {
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
                q = dbf.createQuery(VmInstanceVO.class);
                q.add(VmInstanceVO_.clusterUuid, Op.IN, clusterUuids);
                q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
                lst = q.list();
                for (VmInstanceVO vo : lst) {
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
                q = dbf.createQuery(VmInstanceVO.class);
                q.add(VmInstanceVO_.zoneUuid, Op.IN, zoneUuids);
                q.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
                lst = q.list();
                for (VmInstanceVO vo : lst) {
                    vmvos.put(vo.getUuid(), vo);
                }
            }

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos.values());
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> pruuids = CollectionUtils.transformToList(
                    (List<PrimaryStorageInventory>) action.getParentIssuerContext(),
                    new Function<String, PrimaryStorageInventory>() {
                        @Override
                        public String call(PrimaryStorageInventory arg) {
                            return arg.getUuid();
                        }
                    });


            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select vm from VmInstanceVO vm, VolumeVO vol, PrimaryStorageVO pr" +
                            " where vm.type = :vmType" +
                            " and vm.uuid = vol.vmInstanceUuid" +
                            " and vol.primaryStorageUuid = pr.uuid" +
                            " and vol.type = :volType" +
                            " and pr.uuid in (:uuids)" +
                            " group by vm.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
                    q.setParameter("uuids", pruuids);
                    q.setParameter("volType", VolumeType.Root);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        } else if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> l3uuids = CollectionUtils.transformToList(
                    (List<L3NetworkInventory>) action.getParentIssuerContext(),
                    new Function<String, L3NetworkInventory>() {
                        @Override
                        public String call(L3NetworkInventory arg) {
                            return arg.getUuid();
                        }
                    });

            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select vm from VmInstanceVO vm, L3NetworkVO l3, VmNicVO nic" +
                            " where vm.type = :vmType" +
                            " and vm.uuid = nic.vmInstanceUuid" +
                            " and vm.state in (:vmStates)" +
                            " and nic.l3NetworkUuid = l3.uuid" +
                            " and l3.uuid in (:uuids)" +
                            " group by vm.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
                    q.setParameter("vmStates", Arrays.asList(
                            VmInstanceState.Stopped,
                            VmInstanceState.Running,
                            VmInstanceState.Destroyed));
                    q.setParameter("uuids", l3uuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        } else if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> ipruuids = CollectionUtils.transformToList(
                    (List<IpRangeInventory>) action.getParentIssuerContext(),
                    new Function<String, IpRangeInventory>() {
                        @Override
                        public String call(IpRangeInventory arg) {
                            return arg.getUuid();
                        }
                    });

            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select vm from VmInstanceVO vm, VmNicVO nic, UsedIpVO ip, IpRangeVO ipr" +
                            " where vm.type = :vmType" +
                            " and vm.uuid = nic.vmInstanceUuid" +
                            " and vm.state not in (:vmStates)" +
                            " and nic.usedIpUuid = ip.uuid" +
                            " and ip.ipRangeUuid = ipr.uuid" +
                            " and ipr.uuid in (:uuids)" +
                            " group by vm.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
                    q.setParameter("vmStates", Arrays.asList(VmInstanceState.Stopped, VmInstanceState.Stopping));
                    q.setParameter("uuids", ipruuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transformToList(
                    (List<AccountInventory>) action.getParentIssuerContext(),
                    new Function<String, AccountInventory>() {
                        @Override
                        public String call(AccountInventory arg) {
                            return arg.getUuid();
                        }
                    });

            List<VmInstanceVO> vmvos = new Callable<List<VmInstanceVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<VmInstanceVO> call() {
                    String sql = "select d from VmInstanceVO d, AccountResourceRefVO r" +
                            " where d.uuid = r.resourceUuid" +
                            " and r.resourceType = :rtype" +
                            " and r.accountUuid in (:auuids)" +
                            " group by d.uuid";
                    TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
                    q.setParameter("rtype", VmInstanceVO.class.getSimpleName());
                    q.setParameter("auuids", auuids);
                    return q.getResultList();
                }
            }.call();

            if (!vmvos.isEmpty()) {
                ret = toVmDeletionStruct(vmvos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            int op = toDeletionOpCode(action);
            if (op == OP_NOPE || op == OP_STOP || op == OP_REMOVE_INSTANCE_OFFERING || op == OP_DETACH_NIC) {
                return null;
            }

            List<VmDeletionStruct> vms = vmFromDeleteAction(action);
            if (vms == null) {
                return null;
            }

            return action.copy().setParentIssuer(NAME).setParentIssuerContext(vms);
        }

        return null;
    }
}
