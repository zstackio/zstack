package org.zstack.test.storage.volume;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by miao on 16-7-22.
 */
public class TestVolumeQuota {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SftpBackupStorageSimulatorConfig sftpConfig;
    GlobalConfigFacade gcf;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        IdentityCreator identityCreator = new IdentityCreator(api);
        api.createAccount("Test", "Test");
        AccountInventory test = identityCreator.useAccount("test");

        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_DATA_VOLUME_NUM, 0);

        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        String itag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY.instantiateTag(map(e("tag", "ps1")));
        api.createSystemTag(dinv.getUuid(), itag, DiskOfferingVO.class);
        api.shareResource(list(dinv.getUuid()), list(test.getUuid()), true, null);

        thrown.expect(ApiSenderException.class);
        thrown.expectMessage(VolumeConstant.QUOTA_DATA_VOLUME_NUM);

        VolumeInventory vol = api.createDataVolume("data", dinv.getUuid(), identityCreator.getAccountSession());
    }
}


