package org.zstack.hybrid;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.aliyun.ecs.APICreateEcsInstanceFromLocalImageEvent;
import org.zstack.header.aliyun.ecs.APICreateEcsInstanceFromLocalImageMsg;
import org.zstack.header.aliyun.ecs.EcsInstanceConstant;
import org.zstack.header.aliyun.network.group.EcsSecurityGroupVO;
import org.zstack.header.aliyun.network.group.EcsSecurityGroupVO_;
import org.zstack.header.aliyun.network.vpc.EcsVpcVO;
import org.zstack.header.aliyun.network.vpc.EcsVpcVO_;
import org.zstack.header.datacenter.APIAddDataCenterFromRemoteMsg;
import org.zstack.header.datacenter.APIAddDataCenterFromRemoteEvent;
import org.zstack.header.datacenter.DataCenterVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identityzone.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.hybrid.core.HybridType;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by mingjian.deng on 17/2/8.
 */
public class TestCreateEcsInstance {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    private SessionInventory adminSession;

    protected static final CLogger logger = Utils.getLogger(TestCreateEcsInstance.class);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/aliyun/TestImageStoreCreateVmOnKvm.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("ImageStorePrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        adminSession = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        APIAddDataCenterFromRemoteMsg msg = new APIAddDataCenterFromRemoteMsg();
        msg.setRegionId("cn-hangzhou");
        msg.setType(HybridType.aliyun.toString());
        APIAddDataCenterFromRemoteEvent event = createDataCenter(msg);
        Assert.assertTrue(event.isSuccess());
        Assert.assertNotNull(dbf.findByUuid(event.getInventory().getUuid(), DataCenterVO.class));


        APIAddIdentityZoneFromRemoteMsg msg1 = new APIAddIdentityZoneFromRemoteMsg();
        msg1.setType(HybridType.aliyun.toString());
        msg1.setDataCenterUuid(event.getInventory().getUuid());
        APIAddIdentityZoneFromRemoteEvent event1 = createIdentityZone(msg1);
        Assert.assertTrue(event1.isSuccess());
        Assert.assertNotNull(dbf.findByUuid(event1.getInventory().getUuid(), IdentityZoneVO.class));

        List<EcsVpcVO> vpcs = dbf.createQuery(EcsVpcVO.class).add(EcsVpcVO_.dataCenterUuid, SimpleQuery.Op.EQ, event.getInventory().getUuid()).list();

        APICreateEcsInstanceFromLocalImageMsg msg2 = new APICreateEcsInstanceFromLocalImageMsg();
        msg2.setDescription("zstack-test");
        msg2.setImageUuid(deployer.images.get("TestImage").getUuid());
        msg2.setBackupStorageUuid(deployer.backupStorages.get("imagestore").getUuid());
        msg2.setEcsBandWidth(10l);
        msg2.setEcsRootPassword("Password123");
        msg2.setIdentityZoneUuid(event1.getInventory().getUuid());
        msg2.setInstanceOfferingUuid(deployer.instanceOfferings.get("TestInstanceOffering").getUuid());
        msg2.setEcsRootVolumeType(EcsInstanceConstant.EcsVolumeCategory.Cloud_Efficiency);
        msg2.setEcsSecurityGroupUuid(dbf.createQuery(EcsSecurityGroupVO.class)
                .add(EcsSecurityGroupVO_.ecsVpcUuid, SimpleQuery.Op.EQ, vpcs.get(0).getUuid())
                .find().getUuid());
        APICreateEcsInstanceFromLocalImageEvent event2 = createEcs(msg2);
        Assert.assertTrue(event2.isSuccess());

    }

    private APICreateEcsInstanceFromLocalImageEvent createEcs(APICreateEcsInstanceFromLocalImageMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(15);
        APICreateEcsInstanceFromLocalImageEvent event = sender.send(msg, APICreateEcsInstanceFromLocalImageEvent.class);
        return event;
    }

    private APIAddDataCenterFromRemoteEvent createDataCenter(APIAddDataCenterFromRemoteMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(10);
        APIAddDataCenterFromRemoteEvent event = sender.send(msg, APIAddDataCenterFromRemoteEvent.class);
        return event;
    }

    private APIAddIdentityZoneFromRemoteEvent createIdentityZone(APIAddIdentityZoneFromRemoteMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(10);
        APIAddIdentityZoneFromRemoteEvent event = sender.send(msg, APIAddIdentityZoneFromRemoteEvent.class);
        return event;
    }

    private APIDeleteIdentityZoneInLocalEvent deleteIdentityZone(APIDeleteIdentityZoneInLocalMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(10);
        APIDeleteIdentityZoneInLocalEvent event = sender.send(msg, APIDeleteIdentityZoneInLocalEvent.class);
        return event;
    }
}
