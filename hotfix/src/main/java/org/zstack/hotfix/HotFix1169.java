package org.zstack.hotfix;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CacheInstallPath;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/10/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HotFix1169 implements HotFix {
    private static CLogger logger = Utils.getLogger(HotFix1169.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private APIHotFix1169KvmSnapshotChainMsg msg;
    private APIHotFix1169KvmSnapshotChainEvent evt;

    private PrimaryStorageVO primaryStorageVO;
    private List<String> attachedKvmClusterUuids;
    private List<VolumeVO> volumesReady;

    class NodeException extends Exception {
        ErrorCode error;

        public NodeException(ErrorCode err) {
            error = err;
        }
    }

    /*
#!/bin/bash

all=`find $1 -type f -exec file {} \; | awk -F: '$2~/QEMU QCOW/{print $1}'`
for f in $all
do
    bk=`qemu-img info $f | grep -w 'backing file:' | awk '{print $3}'`
    if [ x"$bk" == "x" ]; then
        bk="NONE"
    fi

    size=`ls -l $f | awk '{print $5}'`
    if [ x"$size" == "x" ]; then
        size="NONE"
    fi

    date=`stat -c %Y $f`
    if [ x"$date" == "x" ]; then
        date="NONE"
    fi

    echo $f $bk $size $date
done
     */
    private String READ_QCOW2_SCRIPT = "#!/bin/bash\n" +
            "\n" +
            "all=`find %s -type f -exec file {} \\; | awk -F: '$2~/QEMU QCOW/{print $1}'`\n" +
            "for f in $all\n" +
            "do\n" +
            "    bk=`qemu-img info $f | grep -w 'backing file:' | awk '{print $3}'`\n" +
            "    if [ x\"$bk\" == \"x\" ]; then\n" +
            "        bk=\"NONE\"\n" +
            "    fi\n" +
            "\n" +
            "    size=`ls -l $f | awk '{print $5}'`\n" +
            "    if [ x\"$size\" == \"x\" ]; then\n" +
            "        size=\"NONE\"\n" +
            "    fi\n" +
            "\n" +
            "    date=`stat -c %%Y $f`\n" +
            "    if [ x\"$date\" == \"x\" ]; then\n" +
            "        date=\"NONE\"\n" +
            "    fi\n" +
            "\n" +
            "    echo $f $bk $size $date\n" +
            "done";

    class Node {
        Node parent;
        Map<String, Node> children = new HashMap<String, Node>();
        String path;
        Long size;
        long lastModificationTime;

        Node findAncient() {
            if (parent == null) {
                return this;
            }

            Node p = parent;
            while (p.parent != null) {
                p = p.parent;
            }

            return p;
        }

        boolean isLeaf() {
            return children.isEmpty();
        }
    }

    class Fixer {
        Map<String, Node> nodesOnStorage;
        Map<String, Node> nodesInDb;
        // start point.
        // for root volume, it's image cache path
        // for data volume, it's the first snapshot
        String start;
        // end point
        // it's current volume path
        String end;
        VolumeVO volume;
        String treeUuid;
        HotFix1169Result result = new HotFix1169Result();
        ImageCachePath imageCachePath;

        boolean hasMissingSnapshots;

        @Transactional
        HotFix1169Result fix() throws NodeException {
            result.volumeName = volume.getName();
            result.volumeUuid = volume.getUuid();

            boolean startMustInDb = new Callable<Boolean>() {
                public Boolean call() {
                    if (volume.getType() == VolumeType.Root) {
                        return true;
                    } else {
                        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
                        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, volume.getUuid());
                        return q.count() > 0;
                    }
                }
            }.call();

            if (startMustInDb) {
                // the volume is a root volume or
                // it's a data volume with snapshots
                Node startNodeInDb = nodesInDb.get(start);
                DebugUtils.Assert(startNodeInDb != null, String.format("startNodeInDb is null for %s", start));
                Node startNodeOnStorage = nodesOnStorage.get(start);
                DebugUtils.Assert(startNodeOnStorage != null, String.format("startNodeOnStorage is null for %s", start));

                Stack<String> path = new Stack<String>();
                compareNodes(startNodeInDb, startNodeOnStorage, path);
                logger.debug(String.format("[HOTFIX 1169] Snapshot tree check passed for the volume[name:%s, uuid:%s]",
                        volume.getName(), volume.getUuid()));
                walkAndFix(startNodeOnStorage, startNodeInDb);
            } else {
                // data volume with no snapshots
                Node startNodeInDb = nodesInDb.get(start);
                if (startNodeInDb != null) {
                    if (!startNodeInDb.path.equals(volume.getInstallPath())) {
                        result.setError(String.format("The data volume[uuid:%s, name:%s] has a unknown parent[%s] in" +
                                " the database", volume.getUuid(), volume.getName(), startNodeInDb.path));
                    }
                    // else the node is ok, no need to fix
                } else {
                    DebugUtils.Assert(treeUuid == null, "why treeUuid has value");
                    treeUuid = createNewTree();

                    Node startNodeOnStorage = nodesOnStorage.get(start);
                    DebugUtils.Assert(startNodeOnStorage != null, "startNodeOnStorage is null");

                    insertMissingSnapshotInDatabase(startNodeOnStorage);
                }
            }

            if (hasMissingSnapshots) {
                recalculateVolumePathAndSize();
                recalculateSnapshotDistance();
            }

            verify();

            return result;
        }

        @Transactional
        private void verify() throws NodeException {
            Map<String, Node> newNodesInDb = new HashMap<>();
            buildNodesInDb(volume, newNodesInDb, imageCachePath);

            class FullTreeVerify {
                void verify(Node inDb, Node onStorage, Stack<String> path) throws NodeException {
                    if (!inDb.path.equals(onStorage.path)) {
                        List<String> pathIndb = new ArrayList<>();
                        pathIndb.addAll(path);
                        pathIndb.add(inDb.path);

                        List<String> pathOnStorage = new ArrayList<>();
                        pathOnStorage.addAll(path);
                        pathOnStorage.add(onStorage.path);

                        String err = String.format(
                                "diverged snapshot tree(AFTER FIXING!!!):\n" +
                                        "volume[name:%s, uuid:%s]'s snapshot tree is diverged between database and storage\n" +
                                        "path in database:\n%s\n" +
                                        "path on storage:\n%s\n", volume.getName(), volume.getUuid(), StringUtils.join(pathIndb, " --> "),
                                StringUtils.join(pathOnStorage, " --> "));
                        logger.warn(err);

                        throw new NodeException(errf.stringToOperationError(err));
                    }

                    if (inDb.children.size() != onStorage.children.size()) {
                        List<String> pathIndb = new ArrayList<>();
                        pathIndb.addAll(path);
                        pathIndb.add(inDb.path);

                        String err = String.format(
                                "diverged snapshot tree(AFTER FIXING!!!), this may not be an error:\n" +
                                        "volume[name:%s, uuid:%s]'s snapshot tree is diverged between database and storage\n" +
                                        "path: %s\n" +
                                        "in database: has %s children\n" +
                                        "on storage: has %s children\n", volume.getName(), volume.getUuid(), StringUtils.join(pathIndb, " --> "),
                                        inDb.children.size(), onStorage.children.size());
                        logger.warn(err);
                        //throw new NodeException(errf.stringToOperationError(err));
                    }

                    path.push(inDb.path);
                    for (Node db : inDb.children.values()) {
                        Node stor = onStorage.children.get(db.path);
                        if (stor == null) {
                            String err = String.format("diverged snapshot tree(AFTER FIXING!!!):\n" +
                                            "volume[name:%s, uuid:%s]'s snapshot tree has a path not found on the storage:\n" +
                                            "path: %s\n" +
                                            "the missing on storage: %s\n", volume.getName(), volume.getUuid(), StringUtils.join(path, " --> "), db.path);
                            logger.warn(err);
                            throw new NodeException(errf.stringToOperationError(err));
                        }

                        verify(db, stor, path);
                    }
                    path.pop();
                }
            }

            Node startNodeInDb = newNodesInDb.get(volume.getInstallPath()).findAncient();
            Node startNodeOnStorage = nodesOnStorage.get(volume.getInstallPath()).findAncient();
            new FullTreeVerify().verify(startNodeInDb, startNodeOnStorage, new Stack<>());
        }

        @Transactional
        private void recalculateSnapshotDistance() {
            String sql = "select sp from VolumeSnapshotVO sp where sp.treeUuid = :treeUuid";
            TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
            q.setParameter("treeUuid", treeUuid);
            final List<VolumeSnapshotVO> vos = q.getResultList();

            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);

            class D {
                int distance;
            }

            D d = new D();
            tree.getRoot().walk(new Function<Void, SnapshotLeaf>() {
                public Void call(SnapshotLeaf arg) {
                    VolumeSnapshotVO vo = vos.stream().filter(v -> v.getUuid().equals(arg.getUuid())).findAny().get();
                    vo.setDistance(d.distance);
                    dbf.getEntityManager().merge(vo);
                    dbf.getEntityManager().flush();
                    d.distance ++;
                    return null;
                }
            });
        }

        @Transactional
        private void recalculateVolumePathAndSize() {
            Node nodeOnStorage = nodesOnStorage.get(volume.getInstallPath());

            final List<Node> leaves = new ArrayList<Node>();
            class FindLeaf {
                void find(Node n) {
                    if (n.isLeaf()) {
                        leaves.add(n);
                    } else {
                        for (Node nn : n.children.values()) {
                            find(nn);
                        }
                    }
                }
            }
            new FindLeaf().find(nodeOnStorage);

            // find the last modified qcow2 if there are more than one
            Node l = leaves.get(0);
            for (Node ll : leaves) {
                if (ll.lastModificationTime > l.lastModificationTime) {
                    l = ll;
                }
            }

            result.addDetail(String.format("fixed installPath from %s to %s, size from %s to %s for the volume" +
                    "[uuid:%s, name:%s]", volume.getInstallPath(), l.path, volume.getSize(), l.size, volume.getUuid(),
                    volume.getName()));

            VolumeSnapshotVO latestSnapshot = findSnapshotByPath(l.parent.path);
            DebugUtils.Assert(latestSnapshot != null, String.format("cannot find the latest snapshot[%s]", l.parent.path));

            // reset the latest flag
            // DON'T USE query to update, it won't work with JPA transaction
            // so the later query won't see the changes
            String sql = "select sp from VolumeSnapshotVO sp where sp.treeUuid = :uuid";
            TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
            q.setParameter("uuid", latestSnapshot.getTreeUuid());
            List<VolumeSnapshotVO> sps = q.getResultList();
            for (VolumeSnapshotVO sp : sps) {
                if (sp.getUuid().equals(latestSnapshot.getUuid())) {
                    sp.setLatest(true);
                } else {
                    sp.setLatest(false);
                }
                dbf.getEntityManager().merge(latestSnapshot);
                dbf.getEntityManager().flush();
            }

            logger.debug(String.format("[HOTFIX 1169] reset the latest flag to the snapshot[uuid:%s, path:%s] on the tree[uuid:%s]",
                    latestSnapshot.getUuid(), latestSnapshot.getPrimaryStorageInstallPath(), latestSnapshot.getTreeUuid()));

            volume.setActualSize(l.size);
            volume.setInstallPath(l.path);
            dbf.getEntityManager().merge(volume);
            dbf.getEntityManager().flush();

            // delete the one selected as the new volume
            VolumeSnapshotVO vs = findSnapshotByPath(l.path);
            dbf.getEntityManager().remove(vs);
            dbf.getEntityManager().flush();
        }

        @Transactional
        private void walkAndFix(Node snode, Node dbnode) throws NodeException {
            walkAndFixMissingSnapshot(snode, dbnode);
        }


        private String createNewTree() {
            // a missing new tree
            VolumeSnapshotTreeVO t = new VolumeSnapshotTreeVO();
            t.setCurrent(true);
            t.setVolumeUuid(volume.getUuid());
            t.setUuid(Platform.getUuid());
            dbf.getEntityManager().persist(t);
            dbf.getEntityManager().flush();

            logger.debug(String.format("[HOTFIX 1169] created a new snapshot tree[uuid:%s] for the volume[uuid:%s, name:%s]",
                    t.getUuid(), volume.getUuid(), volume.getName()));
            return t.getUuid();

        }

        private void walkAndFixMissingSnapshot(Node snode, Node dbnode) throws NodeException {
            if (!snode.path.equals(dbnode.path) && snode.children.size() == dbnode.children.size()) {
                throw new NodeException(errf.stringToOperationError(
                        String.format("DB node[%s] and storage node[%s] has the same parent[%s] and the" +
                                " same number of children", dbnode.path, snode.path, snode.parent == null ?
                        null : snode.parent.path)
                ));
            }

            for (Node db : dbnode.children.values()) {
                Node stor = snode.children.get(db.path);
                if (!db.isLeaf()) {
                    walkAndFixMissingSnapshot(stor, db);
                } else if (db.isLeaf() && !stor.isLeaf()) {
                    // two cases to be here
                    // 1. the snapshot doesn't exist in the database
                    // 2. the snapshot exists in the database, but it has children not recorded by the database
                    logger.debug(String.format("[HOTFIX 1169] found %s missing in the DB snapshot tree of the volume[uuid:%s, name:%s]",
                            stor.path, volume.getUuid(), volume.getName()));

                    if (treeUuid == null) {
                        treeUuid = createNewTree();
                    }

                    insertMissingSnapshotInDatabase(stor);
                }
            }

            /*
            for (Node scnode : snode.children.values()) {
                Node dbcnode = dbnode.children.get(scnode.path);
                if (dbcnode != null && scnode.children.size() == dbcnode.children.size()) {
                    walkAndFixMissingSnapshot(scnode, dbcnode);
                } else {
                    // two cases to be here
                    // 1. the snapshot doesn't exist in the database
                    // 2. the snapshot exists in the database, but it has children not recorded by the database
                    logger.debug(String.format("[HOTFIX 1169] found %s missing in the DB snapshot tree of the volume[uuid:%s, name:%s]",
                            scnode.path, volume.getUuid(), volume.getName()));

                    if (treeUuid == null) {
                        treeUuid = createNewTree();
                    }

                    insertMissingSnapshotInDatabase(scnode);
                }
            }
            */
        }

        @Transactional
        private VolumeSnapshotVO findSnapshotByPath(String path) {
            String sql = "select sp from VolumeSnapshotVO sp where sp.primaryStorageInstallPath = :path";
            TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
            q.setParameter("path", path);
            List<VolumeSnapshotVO> vos = q.getResultList();
            return vos.isEmpty() ? null : vos.get(0);
        }

        @Transactional
        private void insertMissingSnapshotInDatabase(final Node scnode) {
            VolumeSnapshotVO spvo = findSnapshotByPath(scnode.path);

            if (spvo == null) {
                // the case 1: the snapshot doesn't exist in the database
                spvo = new VolumeSnapshotVO();
                spvo.setUuid(Platform.getUuid());
                spvo.setName(String.format("sp-for-volume-%s", volume.getName()));
                spvo.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                spvo.setFormat(volume.getFormat());
                spvo.setDistance(new Callable<Integer>() {
                    public Integer call() {
                        int d = 0;
                        Node p = scnode;
                        while (p.parent != null) {
                            p = p.parent;
                            d++;
                        }
                        return d;
                    }
                }.call());
                spvo.setFullSnapshot(false);
                spvo.setLatest(scnode.isLeaf());
                spvo.setSize(scnode.size);
                spvo.setStatus(VolumeSnapshotStatus.Ready);
                spvo.setState(VolumeSnapshotState.Enabled);
                spvo.setPrimaryStorageInstallPath(scnode.path);
                spvo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                spvo.setVolumeUuid(volume.getUuid());
                spvo.setVolumeType(volume.getType().toString());
                spvo.setTreeUuid(treeUuid);

                if (scnode.parent != null) {
                    VolumeSnapshotVO parent = findSnapshotByPath(scnode.parent.path);
                    if (parent == null) {
                        logger.debug(String.format("[HOTFIX 1169]the orphan snapshot[%s]'s parent[%s] has no record in our database, treat it as the" +
                                " first  snapshot", scnode.path, scnode.parent.path));
                    } else {
                        spvo.setParentUuid(parent.getUuid());
                    }
                }

                dbf.getEntityManager().persist(spvo);
                dbf.getEntityManager().flush();

                hasMissingSnapshots = true;
                String info = String.format("fixed a missing snapshot[uuid:%s, path:%s]", spvo.getUuid(), spvo.getPrimaryStorageInstallPath());
                logger.debug(String.format("[HOTFIX 1169 %s", info));
                result.addDetail(info);
            }

            for (Node c : scnode.children.values()) {
                insertMissingSnapshotInDatabase(c);
            }
        }

        private void compareNodes(Node left, Node right, Stack<String> path) throws NodeException {
            if (!left.path.equals(right.path)) {
                List<String> pathIndb = new ArrayList<String>();
                pathIndb.addAll(path);
                pathIndb.add(left.path);

                List<String> pathOnStorage = new ArrayList<String>();
                pathOnStorage.addAll(path);
                pathOnStorage.add(right.path);

                String err = String.format(
                        "diverged snapshot tree:\n" +
                                "volume[name:%s, uuid:%s]'s snapshot tree is diverged between database and storage\n" +
                                "path in database:\n%s\n" +
                                "path on storage:\n%s\n", volume.getName(), volume.getUuid(), StringUtils.join(pathIndb, " --> "),
                        StringUtils.join(pathOnStorage, " --> "));
                logger.warn(err);
                throw new NodeException(errf.stringToOperationError(err));
            }

            if (left.isLeaf()) {
                return;
            }

            path.push(left.path);

            for (Node child : left.children.values()) {
                Node rightChild = right.children.get(child.path);
                if (rightChild == null) {
                    List<String> pathIndb = new ArrayList<String>();
                    pathIndb.addAll(path);
                    pathIndb.add(child.path);

                    List<String> pathOnStorage = new ArrayList<String>();
                    pathOnStorage.addAll(path);
                    String err =  String.format(
                            "diverged snapshot tree:\n" +
                                    "volume[name:%s, uuid:%s]'s snapshot tree on storage is shorter than the tree in the database\n" +
                                    "path in database:\n%s\n" +
                                    "path on storage:\n%s\n", volume.getName(), volume.getUuid(), StringUtils.join(pathIndb, " --> "),
                            StringUtils.join(pathOnStorage, " --> "));
                    logger.warn(err);
                    throw new NodeException(errf.stringToOperationError(err));
                }

                compareNodes(child, rightChild, path);
            }

            path.pop();
        }
    }

    class LocalStorageHotFix {
        void fix() {
            List<HotFix1169Result> results = new ArrayList<>();

            List<String> volumeUuids = volumesReady.stream().map(VolumeVO::getUuid).collect(Collectors.toList());
            SimpleQuery<LocalStorageResourceRefVO>  lq = dbf.createQuery(LocalStorageResourceRefVO.class);
            lq.add(LocalStorageResourceRefVO_.resourceUuid, Op.IN, volumeUuids);
            lq.add(LocalStorageResourceRefVO_.resourceType, Op.IN, VolumeVO.class.getSimpleName());
            List<LocalStorageResourceRefVO> refs = lq.list();

            Map<String, Object> nodesOnStorage = new HashMap<>();
            for (LocalStorageResourceRefVO ref : refs) {
                if (nodesOnStorage.containsKey(ref.getHostUuid())) {
                    continue;
                }

                KvmRunShellMsg msg = new KvmRunShellMsg();
                msg.setHostUuid(ref.getHostUuid());
                msg.setScript(String.format(READ_QCOW2_SCRIPT, primaryStorageVO.getUrl()));
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, ref.getHostUuid());
                MessageReply reply = bus.call(msg);
                if (!reply.isSuccess()) {
                    nodesOnStorage.put(ref.getHostUuid(), errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                            String.format("unable to get qcow2 file information on the host[uuid:%s]", ref.getHostUuid()),
                            reply.getError()));
                } else {
                    KvmRunShellReply r = reply.castReply();
                    if (r.getReturnCode() != 0) {
                        nodesOnStorage.put(ref.getHostUuid(), errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                                String.format("unable to get qcow2 file information on the host[uuid:%s]. %s %s",
                                        ref.getHostUuid(), r.getStderr(), r.getStdout())));
                    } else {
                        nodesOnStorage.put(ref.getHostUuid(), buildNodesOnStorage(r.getStdout()));
                    }
                }
            }

            for (VolumeVO vol : volumesReady) {
                LocalStorageResourceRefVO ref = refs.stream().filter(r->r.getResourceUuid().equals(vol.getUuid())).findAny().get();

                Object o = nodesOnStorage.get(ref.getHostUuid());
                if (o instanceof ErrorCode) {
                    HotFix1169Result res = new HotFix1169Result();
                    res.volumeName = vol.getName();
                    res.volumeUuid = vol.getUuid();
                    res.setError(((ErrorCode)o).getDetails());
                    results.add(res);
                    continue;
                }

                ImageCachePath imageCachePath = vol1 -> {
                    SimpleQuery<ImageCacheVO> iq = dbf.createQuery(ImageCacheVO.class);
                    iq.add(ImageCacheVO_.imageUuid, Op.EQ, vol1.getRootImageUuid());
                    iq.add(ImageCacheVO_.installUrl, Op.LIKE, String.format("%%hostUuid://%s%%", ref.getHostUuid()));
                    ImageCacheVO cache = iq.find();
                    DebugUtils.Assert(cache != null, String.format("cannot find image cache for the volume[uuid:%s, name:%s]",
                            vol1.getUuid(), vol1.getName()));
                    CacheInstallPath path = new CacheInstallPath();
                    path.fullPath = cache.getInstallUrl();
                    path.disassemble();
                    return path.installPath;
                };

                Fixer fixer = new Fixer();
                fixer.nodesOnStorage = (Map<String, Node>) o;
                fixer.nodesInDb = new HashMap<>();
                fixer.treeUuid = buildNodesInDb(vol, fixer.nodesInDb, imageCachePath);
                fixer.volume = vol;
                fixer.imageCachePath = imageCachePath;

                if (vol.getType() == VolumeType.Root) {
                    SimpleQuery<VolumeSnapshotTreeVO> tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                    tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                    List<VolumeSnapshotTreeVO> trees = tq.list();

                    if (trees.isEmpty() || trees.size() == 1) {
                        // the volume has no snapshot or only one snapshot tree
                        // then the start is the image cache
                        fixer.start = imageCachePath.getImageCachePath(vol);
                        fixer.end = vol.getInstallPath();
                    } else {
                        // the volume has more than one snapshot tree, then the start should be
                        // the first snapshot of the current tree
                        VolumeSnapshotTreeVO current = trees.stream().filter(VolumeSnapshotTreeAO::isCurrent).findAny().get();
                        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
                        q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, current.getUuid());
                        q.add(VolumeSnapshotVO_.parentUuid, Op.NULL);
                        VolumeSnapshotVO sp = q.find();

                        fixer.start = sp.getPrimaryStorageInstallPath();
                        fixer.end = vol.getInstallPath();
                    }
                } else {
                    Node node = fixer.nodesOnStorage.get(vol.getInstallPath());
                    Node ancient = node.findAncient();
                    fixer.start = ancient.path;
                    fixer.end = vol.getInstallPath();
                }

                try {
                    HotFix1169Result res = fixer.fix();
                    if (res.error != null || res.details != null) {
                        // a hotfix applied
                        results.add(res);
                    }
                } catch (NodeException e) {
                    HotFix1169Result res = new HotFix1169Result();
                    res.volumeName = vol.getName();
                    res.volumeUuid = vol.getUuid();
                    res.setError(e.error.getDetails());
                    results.add(res);
                }
            }

            evt.setResults(results);
        }
    }

    class NfsHostFix {
        void fix() {
            List<HotFix1169Result> results = new ArrayList<HotFix1169Result>();

            List<String> connectedKvmHosts = findConnectedKvmHosts();
            KvmRunShellReply r = null;
            List<ErrorCode> errors = new ArrayList<ErrorCode>();
            for (String huuid : connectedKvmHosts) {
                KvmRunShellMsg msg = new KvmRunShellMsg();
                msg.setHostUuid(huuid);
                msg.setScript(String.format(READ_QCOW2_SCRIPT, primaryStorageVO.getMountPath()));
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
                MessageReply reply = bus.call(msg);
                if (reply.isSuccess()) {
                    r = reply.castReply();
                    break;
                } else {
                    errors.add(reply.getError());
                }
            }

            if (r == null) {
                throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                        "no kvm host succeed to get QCOW2 file information", errors));
            }

            if (r.getReturnCode() != 0) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("failed to collected qcow2 info on the primary storage. ret code:%s," +
                                "stdout: %s, stderr: %s", r.getReturnCode(), r.getStdout(), r.getStderr())
                ));
            }

            Map<String, Node> nodes = buildNodesOnStorage(r.getStdout());
            ImageCachePath imageCachePath = vol1 -> {
                SimpleQuery<ImageCacheVO> iq = dbf.createQuery(ImageCacheVO.class);
                iq.add(ImageCacheVO_.imageUuid, Op.EQ, vol1.getRootImageUuid());
                ImageCacheVO cache = iq.find();
                DebugUtils.Assert(cache != null, String.format("cannot find image cache for the volume[uuid:%s, name:%s]",
                        vol1.getUuid(), vol1.getName()));
                return cache.getInstallUrl();
            };

            for (VolumeVO vol : volumesReady) {
                Fixer fixer = new Fixer();
                fixer.nodesOnStorage = nodes;
                fixer.nodesInDb = new HashMap<>();
                fixer.treeUuid = buildNodesInDb(vol, fixer.nodesInDb, imageCachePath);
                fixer.volume = vol;
                fixer.imageCachePath = imageCachePath;

                if (vol.getType() == VolumeType.Root) {
                    SimpleQuery<VolumeSnapshotTreeVO> tq = dbf.createQuery(VolumeSnapshotTreeVO.class);
                    tq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, vol.getUuid());
                    List<VolumeSnapshotTreeVO> trees = tq.list();

                    if (trees.isEmpty() || trees.size() == 1) {
                        // the volume has no snapshot or only one snapshot tree
                        // then the start is the image cache
                        fixer.start = imageCachePath.getImageCachePath(vol);
                        fixer.end = vol.getInstallPath();
                    } else {
                        // the volume has more than one snapshot tree, then the start should be
                        // the first snapshot of the current tree
                        VolumeSnapshotTreeVO current = trees.stream().filter(VolumeSnapshotTreeAO::isCurrent).findAny().get();
                        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
                        q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, current.getUuid());
                        q.add(VolumeSnapshotVO_.parentUuid, Op.NULL);
                        VolumeSnapshotVO sp = q.find();

                        fixer.start = sp.getPrimaryStorageInstallPath();
                        fixer.end = vol.getInstallPath();
                    }
                } else {
                    Node node = nodes.get(vol.getInstallPath());
                    Node ancient = node.findAncient();
                    fixer.start = ancient.path;
                    fixer.end = vol.getInstallPath();
                }

                try {
                    HotFix1169Result res = fixer.fix();
                    if (res.error != null || !res.details.isEmpty()) {
                        // a hotfix applied
                        results.add(res);
                    }
                } catch (NodeException e) {
                    HotFix1169Result res = new HotFix1169Result();
                    res.volumeName = vol.getName();
                    res.volumeUuid = vol.getUuid();
                    res.setError(e.error.getDetails());
                    results.add(res);
                }
            }

            evt.setResults(results);
        }

        private List<String> findConnectedKvmHosts() {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.select(HostVO_.uuid);
            q.add(HostVO_.clusterUuid, Op.IN, attachedKvmClusterUuids);
            q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
            return q.listValue();
        }
    }

    @Transactional
    private Map<String, Node> buildNodesFromTreeUuid(final String startPath, String endPath, String treeUuid) {
        final Map<String, Node> nodes = new HashMap<String, Node>();

        String sql = "select sp from VolumeSnapshotVO sp where sp.treeUuid = :uuid";
        TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
        q.setParameter("uuid", treeUuid);
        List<VolumeSnapshotVO> sps = q.getResultList();
        final VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(sps);

        if (startPath != null) {
            // the tree is linked to a image cache
            // image cache(parent) --> root snapshot(child)
            Node root = new Node();
            root.path = tree.getRoot().getInventory().getPrimaryStorageInstallPath();

            Node p = new Node();
            p.path = startPath;
            p.children.put(root.path, root);
            root.parent = p;

            nodes.put(root.path, root);
            nodes.put(p.path, p);
        }

        tree.getRoot().walk(new Function<Void, SnapshotLeaf>() {
            public Void call(SnapshotLeaf arg) {
                if (startPath != null && arg.getUuid().equals(tree.getRoot().getUuid())) {
                    // the tree is linked to a image cache, skip the root as
                    // we have manually add the relationship
                    // image cache(parent) --> root snapshot(child)
                    return null;
                }

                Node n = nodes.get(arg.getInventory().getPrimaryStorageInstallPath());
                if (n == null) {
                    n = new Node();
                    n.path = arg.getInventory().getPrimaryStorageInstallPath();
                    nodes.put(n.path, n);
                }

                if (arg.getParent() != null) {
                    Node p = nodes.get(arg.getParent().getInventory().getPrimaryStorageInstallPath());
                    if (p == null) {
                        p = new Node();
                        p.path = arg.getParent().getInventory().getPrimaryStorageInstallPath();
                        nodes.put(p.path, p);
                    }

                    p.children.put(n.path, n);
                    n.parent = p;
                }

                return null;
            }
        });

        VolumeSnapshotInventory latest = tree.getRoot().getDescendants().stream()
                .filter(VolumeSnapshotInventory::isLatest).findAny().get();

        Node lnode = nodes.get(latest.getPrimaryStorageInstallPath());
        DebugUtils.Assert(lnode.isLeaf(), String.format("node[%s] is not leaf node", lnode.path));

        Node volNode = new Node();
        volNode.path = endPath;
        volNode.parent = lnode;
        nodes.put(endPath, volNode);

        lnode.children.put(endPath, volNode);

        return nodes;
    }

    interface ImageCachePath {
        String getImageCachePath(VolumeVO vol);
    }

    @Transactional
    private String buildNodesInDb(VolumeVO vol, Map<String, Node> nodes, ImageCachePath cachePath) {
        String treeUuid = null;

        String sql = "select count(*) from VolumeSnapshotTreeVO t where t.volumeUuid = :uuid";
        TypedQuery<Long> tq = dbf.getEntityManager().createQuery(sql, Long.class);
        tq.setParameter("uuid", vol.getUuid());
        long count = tq.getSingleResult();

        if (vol.getType() == VolumeType.Root) {
            String imageCachePath = cachePath.getImageCachePath(vol);

            if (count == 0) {
                // no snapshot
                Node n = new Node();
                n.path = vol.getInstallPath();

                Node p = new Node();
                p.path = imageCachePath;
                p.children.put(n.path, n);
                n.parent = p;

                nodes.put(n.path, n);
                nodes.put(p.path, p);
            } else if (count == 1) {
                // one snapshot tree
                sql = "select t.uuid from VolumeSnapshotTreeVO t where t.volumeUuid = :uuid";
                TypedQuery<String> tsq = dbf.getEntityManager().createQuery(sql, String.class);
                tsq.setParameter("uuid", vol.getUuid());
                treeUuid = tsq.getSingleResult();

                nodes.putAll(buildNodesFromTreeUuid(imageCachePath, vol.getInstallPath(), treeUuid));
            } else {
                // multiple snapshot trees
                sql = "select t.uuid from VolumeSnapshotTreeVO t where t.volumeUuid = :uuid and t.current = :current";
                TypedQuery<String> tsq = dbf.getEntityManager().createQuery(sql, String.class);
                tsq.setParameter("uuid", vol.getUuid());
                tsq.setParameter("current", true);
                treeUuid = tsq.getSingleResult();

                nodes.putAll(buildNodesFromTreeUuid(null, vol.getInstallPath(), treeUuid));
            }
        } else {
            if (count == 0) {
                nodes = new HashMap<>();
                // no snapshot
                Node n = new Node();
                n.path = vol.getInstallPath();
                nodes.put(n.path, n);
            } else {
                // has snapshot trees
                sql = "select t.uuid from VolumeSnapshotTreeVO t where t.volumeUuid = :uuid and t.current = :current";
                TypedQuery<String> tsq = dbf.getEntityManager().createQuery(sql, String.class);
                tsq.setParameter("uuid", vol.getUuid());
                tsq.setParameter("current", true);
                treeUuid = tsq.getSingleResult();
                nodes.putAll(buildNodesFromTreeUuid(null, vol.getInstallPath(), treeUuid));
            }
        }

        return treeUuid;
    }

    private Map<String, Node> buildNodesOnStorage(String qcow2InfoRawOutput) {
        Map<String, Node> nodes = new HashMap<String, Node>();

        for (String s : qcow2InfoRawOutput.split("\n")) {
            s = s.trim().replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "");
            if (s.isEmpty()) {
                continue;
            }

            String[] parts = s.split(" ");
            if (parts.length != 4) {
                throw new CloudRuntimeException(String.format("invalid qcow2 raw info: %s", s));
            }

            String path = parts[0];
            String backingFile = parts[1];

            long size = Long.valueOf(parts[2]);
            long lastModified = Long.valueOf(parts[3]);

            Node node = nodes.get(path);
            if (node == null) {
                node = new Node();
                node.path = path;
                nodes.put(node.path, node);
            }

            node.size = size;
            node.lastModificationTime = lastModified;

            if (!"NONE".equals(backingFile)) {
                Node parent = nodes.get(backingFile);
                if (parent == null) {
                    parent = new Node();
                    parent.path = backingFile;
                    nodes.put(backingFile, parent);
                }

                parent.children.put(node.path, node);
                node.parent = parent;
            }

        }

        return nodes;
    }

    @Transactional(readOnly = true)
    private void findAttachedKvmClusters() {
        String sql = "select ref.clusterUuid from PrimaryStorageClusterRefVO ref, ClusterVO c where" +
                " c.uuid = ref.clusterUuid and ref.primaryStorageUuid = :psUuid and c.hypervisorType = :hvType";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", primaryStorageVO.getUuid());
        q.setParameter("hvType", KVMConstant.KVM_HYPERVISOR_TYPE);
        attachedKvmClusterUuids = q.getResultList();
        if (attachedKvmClusterUuids.isEmpty()) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the primary storage[uuid:%s, name:%s] is not attached to any KVM clusters",
                            primaryStorageVO.getUuid(), primaryStorageVO.getName())
            ));
        }
    }

    public HotFix1169(APIHotFix1169KvmSnapshotChainMsg msg) {
        this.msg = msg;
        evt = new APIHotFix1169KvmSnapshotChainEvent(msg.getId());
    }

    public void fix() {
        primaryStorageVO = dbf.findByUuid(msg.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (!primaryStorageVO.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)
                && !primaryStorageVO.getType().equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the hotfix1169 is only for primary storage with type[%s, %s], but the" +
                                    " primary storage[uuid:%s] is of type[%s]", LocalStorageConstants.LOCAL_STORAGE_TYPE,
                            NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE, primaryStorageVO.getUuid(),
                            primaryStorageVO.getType())
            ));
        }

        findAttachedKvmClusters();

        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Ready);
        q.add(VolumeVO_.primaryStorageUuid, Op.EQ, primaryStorageVO.getUuid());
        volumesReady = q.list();
        if (volumesReady.isEmpty()) {
            // no volumes
            bus.publish(evt);
            return;
        }

        if (primaryStorageVO.getType().equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            new NfsHostFix().fix();
        } else {
            new LocalStorageHotFix().fix();
        }

        bus.publish(evt);
    }
}
