package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeStatus;

/**
 * Created by frank on 6/9/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class IscsiBtrfsSnapshotValidator {
    @Autowired
    private DatabaseFacade dbf;

    void validate(VolumeSnapshotInventory sp) {
        Assert.assertEquals(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString(), sp.getType());
        Assert.assertEquals(VolumeStatus.Ready.toString(), sp.getStatus());
        Assert.assertNull(sp.getParentUuid());
        Assert.assertNotNull(sp.getPrimaryStorageUuid());
        Assert.assertNotNull(sp.getPrimaryStorageInstallPath());
        Assert.assertTrue(sp.isLatest());

        VolumeSnapshotTreeVO treevo = dbf.findByUuid(sp.getTreeUuid(), VolumeSnapshotTreeVO.class);
        Assert.assertNotNull(treevo);
        Assert.assertTrue(treevo.isCurrent());

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, sp.getTreeUuid());
        long count = q.count();
        Assert.assertEquals(1, count);
    }
}
