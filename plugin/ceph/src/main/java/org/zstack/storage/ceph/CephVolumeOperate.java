package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.volume.DistributedVolumeOperateInterface;

import java.util.Random;
import java.util.Set;

/**
 * Created by mingjian.deng on 16/11/9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephVolumeOperate implements DistributedVolumeOperateInterface {
    Set<CephPrimaryStorageMonVO> monsSet;

    public CephPrimaryStorageMonBase chooseTargetVmUuid() {
        CephPrimaryStorageMonBase mon = getRandomMon(monsSet);
        return mon == null? null : mon;
    }

    private CephPrimaryStorageMonBase getRandomMon(final Set<CephPrimaryStorageMonVO> monsSet) {
        int i = 0;
        int random = new Random().nextInt(monsSet.size());
        for(CephPrimaryStorageMonVO vo: monsSet){
            if(i ++ == random){
                return new CephPrimaryStorageMonBase(vo);
            }
        }
        return null;
    }

    @Override
    public String exportFile(String srcPath, String desPath) {
        return null;
    }

    @Override
    public void importFile(String srcPath, String desPath) {

    }

    public Set<CephPrimaryStorageMonVO> getMonsSet() {
        return monsSet;
    }

    public void setMonsSet(Set<CephPrimaryStorageMonVO> monsSet) {
        this.monsSet = monsSet;
    }
}
