package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.storage.backup.APIQueryBackupStorageMsg;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

public class TestAddImage4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/image/TestAddImage4.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException,ApiSenderException {
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test =  identityCreator.createAccount("test", "password");

        SessionInventory session = identityCreator.getAccountSession();
        APIQueryBackupStorageMsg qmsg = new APIQueryBackupStorageMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        APIQueryBackupStorageReply r =  api.query(qmsg, APIQueryBackupStorageReply.class, session);
        Assert.assertEquals(1, r.getInventories().size());
        Assert.assertEquals(sftp.getUuid(), r.getInventories().get(0).getUuid());

        ImageInventory iinv = new ImageInventory();
        iinv.setName("Test Image");
        iinv.setDescription("Test Image");
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
        iinv.setUrl("http://zstack.org/download/win7.qcow2");
        iinv = api.addImage(iinv, session, sftp.getUuid());
        Assert.assertEquals(1, iinv.getBackupStorageRefs().size());
    }
}
