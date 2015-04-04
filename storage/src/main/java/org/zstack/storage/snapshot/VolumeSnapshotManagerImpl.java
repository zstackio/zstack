package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ReplyMessagePreSendingExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.TakeSnapshotMsg;
import org.zstack.header.storage.primary.TakeSnapshotReply;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class VolumeSnapshotManagerImpl extends AbstractService implements VolumeSnapshotManager, ReplyMessagePreSendingExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;

    private void passThrough(VolumeSnapshotMessage msg) {
        VolumeSnapshotVO vo = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (vo == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find volume snapshot[uuid:%s]", msg.getSnapshotUuid())
            ));
        }

        if (msg.getVolumeUuid() != null) {
            VolumeSnapshotTreeBase tree = new VolumeSnapshotTreeBase(vo, true);
            tree.handleMessage((Message) msg);
        } else if (msg.getTreeUuid() != null) {
            VolumeSnapshotTreeBase tree = new VolumeSnapshotTreeBase(vo, false);
            tree.handleMessage((Message) msg);
        } else {
            VolumeSnapshot snapshot = new VolumeSnapshotBase(vo);
            snapshot.handleMessage((Message) msg);
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeSnapshotMessage) {
            passThrough((VolumeSnapshotMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVolumeSnapshotMsg) {
            handle((CreateVolumeSnapshotMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    private void handle(APIGetVolumeSnapshotTreeMsg msg) {
        APIGetVolumeSnapshotTreeReply reply = new APIGetVolumeSnapshotTreeReply();
        if (msg.getTreeUuid() != null) {
            VolumeSnapshotTreeVO treeVO = dbf.findByUuid(msg.getTreeUuid(), VolumeSnapshotTreeVO.class);
            if (treeVO == null) {
                reply.setInventories(new ArrayList<VolumeSnapshotTreeInventory>());
                bus.reply(msg, reply);
                return;
            }

            VolumeSnapshotTreeInventory inv = VolumeSnapshotTreeInventory.valueOf(treeVO);
            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, msg.getTreeUuid());
            List<VolumeSnapshotVO> vos = q.list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory());
            reply.setInventories(Arrays.asList(inv));
        } else if (msg.getVolumeUuid() != null) {
            SimpleQuery<VolumeSnapshotTreeVO> q = dbf.createQuery(VolumeSnapshotTreeVO.class);
            q.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, msg.getVolumeUuid());
            List<VolumeSnapshotTreeVO> trees = q.list();
            if (trees.isEmpty()) {
                reply.setInventories(new ArrayList<VolumeSnapshotTreeInventory>());
                bus.reply(msg, reply);
                return;
            }

            List<VolumeSnapshotTreeInventory> treeInventories = new ArrayList<VolumeSnapshotTreeInventory>();
            for (VolumeSnapshotTreeVO vo : trees) {
                VolumeSnapshotTreeInventory inv = VolumeSnapshotTreeInventory.valueOf(vo);
                SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                sq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, vo.getUuid());
                List<VolumeSnapshotVO> vos = sq.list();
                VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
                inv.setTree(tree.getRoot().toLeafInventory());
                treeInventories.add(inv);
            }

            reply.setInventories(treeInventories);
        }

        bus.reply(msg, reply);
    }


    @Transactional
    private VolumeSnapshotStruct newChain(VolumeSnapshotVO vo) {
        VolumeSnapshotTreeVO chain = new VolumeSnapshotTreeVO();
        chain.setCurrent(true);
        chain.setVolumeUuid(vo.getVolumeUuid());
        chain.setUuid(Platform.getUuid());
        chain = dbf.getEntityManager().merge(chain);

        logger.debug(String.format("created new volume snapshot tree[tree uuid:%s, volume uuid:%s, full snapshot uuid:%s]",
                chain.getUuid(), vo.getVolumeUuid(), vo.getUuid()));

        vo.setTreeUuid(chain.getUuid());
        vo.setDistance(0);
        vo.setParentUuid(null);
        vo.setLatest(true);
        vo.setFullSnapshot(true);
        vo = dbf.getEntityManager().merge(vo);

        VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
        struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
        struct.setFullSnapshot(true);
        return struct;
    }

    @Transactional
    private VolumeSnapshotStruct saveSnapshot(VolumeSnapshotVO vo) {
        String sql = "select c from VolumeSnapshotTreeVO c where c.volumeUuid = :volUuid and c.current = true";
        TypedQuery<VolumeSnapshotTreeVO> cq = dbf.getEntityManager().createQuery(sql, VolumeSnapshotTreeVO.class);
        cq.setParameter("volUuid", vo.getVolumeUuid());
        List<VolumeSnapshotTreeVO> rets = cq.getResultList();
        DebugUtils.Assert(rets.size() < 2, "can not have more than one VolumeSnapshotTreeVO with current=1");
        VolumeSnapshotTreeVO chain = rets.isEmpty() ? null : rets.get(0);
        if (chain == null) {
            return newChain(vo);
        } else {
            sql = "select s from VolumeSnapshotVO s where s.latest = true and s.volumeUuid = :volUuid and s.treeUuid = :chainUuid";
            TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
            q.setParameter("volUuid", vo.getVolumeUuid());
            q.setParameter("chainUuid", chain.getUuid());
            VolumeSnapshotVO latest = q.getSingleResult();

            if (VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.value(Integer.class) == latest.getDistance()) {
                chain.setCurrent(false);
                dbf.getEntityManager().merge(chain);
                return newChain(vo);
            }

            latest.setLatest(false);
            latest = dbf.getEntityManager().merge(latest);

            vo.setTreeUuid(latest.getTreeUuid());
            vo.setLatest(true);
            vo.setParentUuid(latest.getUuid());
            vo.setDistance(latest.getDistance() + 1);
            vo = dbf.getEntityManager().merge(vo);

            VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
            struct.setParent(VolumeSnapshotInventory.valueOf(latest));
            struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
            return struct;
        }
    }

    @Transactional
    private void rollbackSnapshot(String uuid) {
        VolumeSnapshotVO vo = dbf.getEntityManager().find(VolumeSnapshotVO.class, uuid);
        dbf.getEntityManager().remove(vo);
        if (vo.getParentUuid() != null) {
            VolumeSnapshotVO parent = dbf.getEntityManager().find(VolumeSnapshotVO.class, vo.getParentUuid());
            parent.setLatest(true);
            dbf.getEntityManager().merge(parent);
        } else {
            VolumeSnapshotTreeVO chain = dbf.getEntityManager().find(VolumeSnapshotTreeVO.class, vo.getTreeUuid());
            dbf.getEntityManager().remove(chain);
        }

    }


    private void handle(final CreateVolumeSnapshotMsg msg) {
        final CreateVolumeSnapshotReply ret = new CreateVolumeSnapshotReply();

        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        String primaryStorageUuid = vol.getPrimaryStorageUuid();

        final VolumeSnapshotVO vo = new VolumeSnapshotVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setVolumeUuid(msg.getVolumeUuid());
        vo.setFormat(vol.getFormat());
        vo.setState(VolumeSnapshotState.Enabled);
        vo.setStatus(VolumeSnapshotStatus.Creating);
        vo.setVolumeType(vol.getType().toString());

        acntMgr.createAccountResourceRef(msg.getAccountUuid(), vo.getUuid(), VolumeSnapshotVO.class);

        final VolumeSnapshotStruct struct = saveSnapshot(vo);
        TakeSnapshotMsg tmsg = new TakeSnapshotMsg();
        tmsg.setPrimaryStorageUuid(primaryStorageUuid);
        tmsg.setStruct(struct);
        bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(tmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    TakeSnapshotReply treply = (TakeSnapshotReply)reply;
                    if (treply.getNewVolumeInstallPath() != null) {
                        vol.setInstallPath(treply.getNewVolumeInstallPath());
                        dbf.update(vol);
                    }
                    VolumeSnapshotInventory sinv = treply.getInventory();
                    VolumeSnapshotVO svo = dbf.findByUuid(sinv.getUuid(), VolumeSnapshotVO.class);
                    svo.setType(sinv.getType());
                    svo.setPrimaryStorageUuid(sinv.getPrimaryStorageUuid());
                    svo.setPrimaryStorageInstallPath(sinv.getPrimaryStorageInstallPath());
                    svo.setStatus(VolumeSnapshotStatus.Ready);
                    svo.setSize(sinv.getSize());
                    svo = dbf.updateAndRefresh(svo);
                    ret.setInventory(VolumeSnapshotInventory.valueOf(svo));
                    bus.reply(msg, ret);
                } else {
                    rollbackSnapshot(struct.getCurrent().getUuid());
                    ret.setError(reply.getError());
                    bus.reply(msg, ret);
                }
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetVolumeSnapshotTreeMsg) {
            handle((APIGetVolumeSnapshotTreeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VolumeSnapshotConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getReplyMessageClassForPreSendingExtensionPoint() {
        List<Class> ret = new ArrayList<Class>();
        ret.add(APIQueryVolumeSnapshotTreeReply.class);
        return ret;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message msg) {
        if (msg instanceof APIQueryVolumeSnapshotTreeReply) {
            marshal((APIQueryVolumeSnapshotTreeReply) msg);
        }
    }

    private void marshal(APIQueryVolumeSnapshotTreeReply reply) {
        if (reply.getInventories() == null) {
            // this is for count
            return;
        }

        for (VolumeSnapshotTreeInventory inv : reply.getInventories()) {
            SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
            sq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, inv.getUuid());
            List<VolumeSnapshotVO> vos = sq.list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory());
        }
    }
}
