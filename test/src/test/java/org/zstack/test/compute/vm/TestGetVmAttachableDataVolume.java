package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by zouye on 2017/2/18.
 */
public class TestGetVmAttachableDataVolume {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestAttachVolumeToVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SimpleQuery<DiskOfferingVO> dq = dbf.createQuery(DiskOfferingVO.class);
        dq.add(DiskOfferingVO_.name, SimpleQuery.Op.EQ, "TestDataDiskOffering");
        DiskOfferingVO dvo = dq.find();
        VolumeInventory vinv = api.createDataVolume("TestData", dvo.getUuid());
        vinv.setShareable(true);

        VmInstanceInventory vminv = api.listVmInstances(null).get(0);
        vinv = api.attachVolumeToVm(vminv.getUuid(), vinv.getUuid());
        Assert.assertEquals(Integer.valueOf(2), vinv.getDeviceId());
        Assert.assertTrue(vinv.isAttached());
        Assert.assertEquals(VolumeStatus.Ready.toString(), vinv.getStatus());
        Assert.assertNotNull(vinv.getPrimaryStorageUuid());
        Assert.assertNotNull(vinv.getVmInstanceUuid());
        VolumeVO vvo = dbf.findByUuid(vinv.getUuid(), VolumeVO.class);
        Assert.assertTrue(vvo.isAttached());
        Assert.assertEquals(VolumeStatus.Ready, vvo.getStatus());
        Assert.assertNotNull(vvo.getPrimaryStorageUuid());
        Assert.assertNotNull(vvo.getVmInstanceUuid());


        String sql;
        List<VolumeVO> vos;
        sql = "select vol" +
                " from VolumeVO vol, VmInstanceVO vm, PrimaryStorageClusterRefVO ref" +
                " where vol.type = :type" +
                " and vol.state = :volState" +
                " and vol.status = :volStatus" +
                " and vol.format in (:formats)" +
                " and vol.vmInstanceUuid is null" +
                " and vm.clusterUuid = ref.clusterUuid" +
                " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                " and vm.uuid = :vmUuid" +
                " group by vol.uuid";
        TypedQuery<VolumeVO> q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
        q.setParameter("volState", VolumeState.Enabled);
        q.setParameter("volStatus", VolumeStatus.Ready);
        q.setParameter("vmUuid", vminv.getUuid());
        q.setParameter("type", VolumeType.Data);
        vos = q.getResultList();

        sql = "select vol" +
                " from VolumeVO vol" +
                " where vol.type = :type" +
                " and vol.status = :volStatus" +
                " and vol.state = :volState" +
                " group by vol.uuid";
        q = dbf.getEntityManager().createQuery(sql, VolumeVO.class);
        q.setParameter("type", VolumeType.Data);
        q.setParameter("volState", VolumeState.Enabled);
        q.setParameter("volStatus", VolumeStatus.NotInstantiated);
        vos.addAll(q.getResultList());

        sql = "select ref.hostUuid" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid = :volUuid" +
                " and ref.resourceType = :rtype";
        TypedQuery<String> eq = dbf.getEntityManager().createQuery(sql, String.class);
        eq.setParameter("volUuid", vminv.getRootVolumeUuid());
        eq.setParameter("rtype", VolumeVO.class.getSimpleName());
        List<String> ret = eq.getResultList();

        Assert.assertFalse(ret.isEmpty());

        List<String> volUuids = CollectionUtils.transformToList(vos, new Function<String, VolumeVO>() {
            @Override
            public String call(VolumeVO arg) {
                return VolumeStatus.Ready == arg.getStatus() ? arg.getUuid() : null;
            }
        });

        String hostUuid = ret.get(0);
        sql = "select ref.resourceUuid" +
                " from LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid in (:uuids)" +
                " and ref.resourceType = :rtype" +
                " and ref.hostUuid != :huuid";
        eq = dbf.getEntityManager().createQuery(sql, String.class);
        eq.setParameter("uuids", volUuids);
        eq.setParameter("huuid", hostUuid);
        eq.setParameter("rtype", VolumeVO.class.getSimpleName());
        final List<String> toExclude = eq.getResultList();

        vos = CollectionUtils.transformToList(vos, new Function<VolumeVO, VolumeVO>() {
            @Override
            public VolumeVO call(VolumeVO arg) {
                return toExclude.contains(arg.getUuid()) ? null : arg;
            }
        });

        sql = "select ref.volumeUuid" +
                " from ShareableVolumeVmInstanceRefVO ref" +
                " where ref.volumeUuid in (:uuids)" +
                " and ref.vmInstanceUuid = :vmuuid";
        eq = dbf.getEntityManager().createQuery(sql, String.class);
        eq.setParameter("uuids", volUuids);
        eq.setParameter("vmuuid", vminv.getUuid());
        final List<String> attachedShareable = eq.getResultList();

        vos = CollectionUtils.transformToList(vos, new Function<VolumeVO, VolumeVO>() {
            @Override
            public VolumeVO call(VolumeVO arg) {
                return attachedShareable.contains(arg.getUuid()) ? null : arg;
            }
        });


        for (VolumeVO vo : vos) {
            Assert.assertTrue(vo.isShareable());
        }
    }
}
