package org.zstack.expon.sdk.volume;

/**
 * @example
 * {
 *     "VolumeNamespace": "ussns",
 *     "blkSize": 512,
 *     "lun_id": "19",
 *     "poolId": 1,
 *     "size": 28991029248,
 *     "target": "iqn.2024-04.com.sds.wds:100bbf2da6e1",
 *     "treeId": 43,
 *     "uuid": "5550f3ec-af9a-4d9e-8638-ba4cf76293e0",
 *     "volId": 51,
 *     "volName": "volume-0462cfeb-98f9-4f70-b5cc-2a42feeec376"
 * }
 */
public class VolumeLunModule {
    private String volumeNamespace;
    private int blockSize;
    private String lunId;
    private int poolId;
    private long size;
    private String target;
    private int treeId;
    private String uuid;
    private int volId;
    private String volName;

    public String getVolumeNamespace() {
        return volumeNamespace;
    }

    public void setVolumeNamespace(String volumeNamespace) {
        this.volumeNamespace = volumeNamespace;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public String getLunId() {
        return lunId;
    }

    public void setLunId(String lunId) {
        this.lunId = lunId;
    }

    public int getPoolId() {
        return poolId;
    }

    public void setPoolId(int poolId) {
        this.poolId = poolId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(int treeId) {
        this.treeId = treeId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getVolId() {
        return volId;
    }

    public void setVolId(int volId) {
        this.volId = volId;
    }

    public String getVolName() {
        return volName;
    }

    public void setVolName(String volName) {
        this.volName = volName;
    }
}
