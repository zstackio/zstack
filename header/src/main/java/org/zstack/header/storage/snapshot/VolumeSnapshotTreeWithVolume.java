package org.zstack.header.storage.snapshot;

import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VolumeSnapshotTreeWithVolume {
    public static class VolumeSnapshotLeafInventory {
        private VolumeSnapshotInventory inventory;
        private String parentUuid;
        private List<VolumeSnapshotLeafInventory> children = new ArrayList<VolumeSnapshotLeafInventory>();

        public VolumeSnapshotInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeSnapshotInventory inventory) {
            this.inventory = inventory;
        }

        public String getParentUuid() {
            return parentUuid;
        }

        public void setParentUuid(String parentUuid) {
            this.parentUuid = parentUuid;
        }

        public List<VolumeSnapshotLeafInventory> getChildren() {
            return children;
        }

        public void setChildren(List<VolumeSnapshotLeafInventory> children) {
            this.children = children;
        }
    }

    public static class VolumeSnapshotLeaf {
        private VolumeSnapshotInventory inventory;
        private VolumeSnapshotLeaf parent;
        private List<VolumeSnapshotLeaf> children = new ArrayList<>();
        private List<VolumeSnapshotInventory> descendants;
        private List<VolumeSnapshotInventory> ancestors;

        public VolumeSnapshotInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeSnapshotInventory inventory) {
            this.inventory = inventory;
        }

        public VolumeSnapshotLeaf getParent() {
            return parent;
        }

        public void setParent(VolumeSnapshotLeaf parent) {
            this.parent = parent;
        }

        public List<VolumeSnapshotLeaf> getChildren() {
            return children;
        }

        public void setChildren(List<VolumeSnapshotLeaf> children) {
            this.children = children;
        }

        public String getUuid() {
            return inventory.getUuid();
        }

        public void setUuid(String uuid) {
            if (inventory == null) {
                inventory = new VolumeSnapshotInventory();
            }
            inventory.setUuid(uuid);
        }

        private static void walkDownAll(VolumeSnapshotLeaf me, Consumer<VolumeSnapshotLeaf> consumer) {
            consumer.accept(me);
            me.children.forEach(c -> walkDownAll(c, consumer));

        }

        public void walkDownAll(Consumer<VolumeSnapshotLeaf> consumer) {
            walkDownAll(this, consumer);
        }

        public VolumeSnapshotTreeWithVolume toSubTree() {
            VolumeSnapshotTreeWithVolume tree = new VolumeSnapshotTreeWithVolume();
            tree.root = this;
            tree.volumeUuid = inventory.getVolumeUuid();
            return tree;
        }

        private static VolumeSnapshotLeaf walkUp(VolumeSnapshotLeaf leaf, Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(leaf.inventory)) {
                return leaf;
            }

            if (leaf.getParent() == null) {
                return null;
            }

            return walkUp(leaf.getParent(), func);
        }

        public VolumeSnapshotLeaf walkUp(Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(inventory)) {
                return this;
            }

            if (getParent() == null) {
                return null;
            }

            return walkUp(getParent(), func);
        }

        public VolumeSnapshotLeaf walkDown(Function<Boolean, VolumeSnapshotInventory> func) {
            return walkDown(this, func);
        }

        private static VolumeSnapshotLeaf walkDown(VolumeSnapshotLeaf leaf, Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(leaf.inventory)) {
                return leaf;
            }

            if (leaf.getChildren().isEmpty()) {
                return null;
            }

            for (VolumeSnapshotLeaf l : leaf.getChildren()) {
                VolumeSnapshotLeaf ret = walkDown(l, func);
                if (ret != null) {
                    return ret;
                }
            }

            return null;
        }

        public List<VolumeSnapshotInventory> getDescendants() {
            if (descendants == null) {
                descendants = new ArrayList<VolumeSnapshotInventory>();
                walkDown(new Function<Boolean, VolumeSnapshotInventory>() {
                    @Override
                    public Boolean call(VolumeSnapshotInventory arg) {
                        descendants.add(arg);
                        return false;
                    }
                });
            }

            return descendants;
        }

        public List<VolumeSnapshotInventory> getAncestors() {
            if (ancestors == null) {
                ancestors = new ArrayList<VolumeSnapshotInventory>();
                walkUp(new Function<Boolean, VolumeSnapshotInventory>() {
                    @Override
                    public Boolean call(VolumeSnapshotInventory arg) {
                        ancestors.add(arg);
                        return false;
                    }
                });

                Collections.reverse(ancestors);
            }

            return ancestors;
        }

        public VolumeSnapshotLeafInventory toLeafInventory(Set<String> filterUuids) {
            return doToLeafInventory(filterUuids);
        }

        public VolumeSnapshotLeafInventory toLeafInventory() {
            return doToLeafInventory(null);
        }

        private VolumeSnapshotLeafInventory doToLeafInventory(Set<String> filterUuids) {
            VolumeSnapshotLeafInventory leafInventory = new VolumeSnapshotLeafInventory();
            leafInventory.setInventory(getInventory(filterUuids));
            if (parent != null) {
                leafInventory.setParentUuid(parent.getUuid());
            }

            for (VolumeSnapshotLeaf leaf : children) {
                leafInventory.getChildren().add(leaf.doToLeafInventory(filterUuids));
            }

            return leafInventory;
        }

        private VolumeSnapshotInventory getInventory(Set<String> filterUuids) {
            if (filterUuids == null || filterUuids.contains(inventory.getUuid())) {
                return inventory;
            } else {
                VolumeSnapshotInventory inv = new VolumeSnapshotInventory();
                inv.setUuid(inventory.getUuid());
                return inv;
            }
        }

        public List<String> getChildrenVolumeSnapshotInventoryUuid() {
            return children.stream().map(it -> it.getInventory().getUuid()).collect(Collectors.toList());
        }
    }

    private VolumeSnapshotLeaf root;
    private String volumeUuid;
    private List<VolumeSnapshotInventory> aliveChainInDb;
    private boolean isCurrent;

    public static VolumeSnapshotTreeWithVolume fromInventories(List<VolumeSnapshotInventory> invs, boolean treeIsCurrent, VolumeVO volumeVO) {
        if (treeIsCurrent) {
            VolumeSnapshotInventory latestInv = invs.stream().filter(VolumeSnapshotInventory::isLatest).collect(Collectors.toList()).get(0);
            VolumeSnapshotVO snapshotVO = new VolumeSnapshotVO();
            snapshotVO.setLatest(false);
            snapshotVO.setName(String.format("volume-%s-%s", volumeVO.getName(), volumeVO.getUuid()));
            snapshotVO.setUuid(volumeVO.getUuid());
            snapshotVO.setParentUuid(latestInv.getUuid());
            snapshotVO.setTreeUuid(latestInv.getTreeUuid());
            snapshotVO.setState(VolumeSnapshotState.Enabled);
            snapshotVO.setStatus(VolumeSnapshotStatus.Deleting);
            snapshotVO.setPrimaryStorageInstallPath(volumeVO.getInstallPath());
            snapshotVO.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
            invs.add(VolumeSnapshotInventory.valueOf(snapshotVO));
        }

        VolumeSnapshotTreeWithVolume tree = new VolumeSnapshotTreeWithVolume();
        Map<String, VolumeSnapshotLeaf> map = new HashMap<>();
        for (VolumeSnapshotInventory inv : invs) {
            VolumeSnapshotLeaf leaf = map.get(inv.getUuid());
            if (leaf == null) {
                leaf = new VolumeSnapshotLeaf();
                leaf.inventory = inv;
                map.put(inv.getUuid(), leaf);
            } else {
                leaf.inventory = inv;
            }

            if (inv.getParentUuid() != null) {
                VolumeSnapshotLeaf parent = map.get(inv.getParentUuid());
                if (parent == null) {
                    parent = new VolumeSnapshotLeaf();
                    parent.setUuid(inv.getParentUuid());
                    map.put(parent.getUuid(), parent);
                }

                parent.children.add(leaf);
                leaf.parent = parent;
            } else {
                tree.root = leaf;
            }

            tree.volumeUuid = inv.getVolumeUuid();
        }

        tree.aliveChainInDb = tree.getAliveChainInventory();
        Collections.reverse(tree.aliveChainInDb);
        tree.isCurrent = treeIsCurrent;
        DebugUtils.Assert(tree.root != null, "why tree root is null???");
        return tree;
    }

    public static VolumeSnapshotTreeWithVolume fromVOs(List<VolumeSnapshotVO> vos, boolean treeIsCurrent, VolumeVO volumeVO) {
        return fromInventories(VolumeSnapshotInventory.valueOf(vos), treeIsCurrent, volumeVO);
    }

    public VolumeSnapshotLeaf getRoot() {
        return root;
    }

    public void setRoot(VolumeSnapshotLeaf root) {
        this.root = root;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public List<VolumeSnapshotInventory> getAliveChainInDb() {
        return aliveChainInDb;
    }

    public void setAliveChainInDb(List<VolumeSnapshotInventory> aliveChainInDb) {
        this.aliveChainInDb = aliveChainInDb;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    private VolumeSnapshotLeaf findSnapshot(final List<VolumeSnapshotLeaf> leafs, final Function<Boolean, VolumeSnapshotInventory> func) {
        for (VolumeSnapshotLeaf leaf : leafs) {
            VolumeSnapshotLeaf ret = findSnapshot(leaf.children, func);
            if (ret != null) {
                return ret;
            }

            if (func.call(leaf.getInventory())) {
                return leaf;
            }
        }

        return null;
    }

    public VolumeSnapshotLeaf findSnapshot(Function<Boolean, VolumeSnapshotInventory> func) {
        if (func.call(root.getInventory())) {
            return root;
        }
        return findSnapshot(root.children, func);
    }

    public List<VolumeSnapshotLeaf> getAllSnapshotLeafs() {
        List<VolumeSnapshotLeaf> ret = new ArrayList<>();
        root.walkDownAll(new Consumer<VolumeSnapshotLeaf>() {
            @Override
            public void accept(VolumeSnapshotLeaf leaf) {
                ret.add(leaf);
            }
        });

        return ret;
    }

    public boolean inAliveChain(String snapshotUuid) {
        return isCurrent && getAliveChainSnapshotUuids().contains(snapshotUuid);
    }

    private List<VolumeSnapshotInventory> getAliveChainInventory() {
        List<VolumeSnapshotInventory> latestSnapshots = getAllSnapshotLeafs().stream().map(VolumeSnapshotLeaf::getInventory)
                .filter(VolumeSnapshotInventory::isLatest).collect(Collectors.toList());
        if (latestSnapshots.isEmpty()) {
            return new ArrayList<>();
        }

        VolumeSnapshotLeaf latestLeaf = findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getUuid().equals(latestSnapshots.get(0).getUuid());
            }
        });
        return latestLeaf.getAncestors();
    }

    public List<String> getAliveChainSnapshotUuids() {
        if (getAliveChainInventory().isEmpty()) {
            return new ArrayList<>();
        }
        return getAliveChainInventory().stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toList());
    }

    public List<String> getAliveChainSnapshotInstallPath() {
        if (getAliveChainInventory().isEmpty()) {
            return new ArrayList<>();
        }
        return getAliveChainInventory().stream().map(VolumeSnapshotInventory::getPrimaryStorageInstallPath).collect(Collectors.toList());
    }

    public List<VolumeSnapshotLeaf> getSiblingLeaves(VolumeSnapshotLeaf leaf) {
        if (leaf.getParent() == null || leaf.getParent().getChildren().size() <= 1) {
            return new ArrayList<>();
        }

        List<VolumeSnapshotLeaf> siblingLeaves = leaf.getParent().getChildren();
        siblingLeaves.remove(leaf);
        return siblingLeaves;
    }
}
