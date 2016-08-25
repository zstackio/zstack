package org.zstack.test.mevoco.imageStore;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.APICloneVmInstanceMsg;
import org.zstack.header.vm.CloneVmInstanceResults;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;

public class TestImageStoreCloneVm {
    private CLogger logger = Utils.getLogger(TestImageStoreCloneVm.class);
    private Deployer deployer;
    private Api api;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestImageStoreCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("ImageStorePrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        ComponentLoader loader = deployer.getComponentLoader();
        loader.getComponent(CloudBus.class);
        loader.getComponent(DatabaseFacade.class);
        loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmCreator creator = new VmCreator(api);

        // clone the created VM instance
        int numOfClonedVm = 0;
        List<String> names = Collections.singletonList("cloned");

        try {
	    MessageCommandRecorder.reset();
	    MessageCommandRecorder.start(APICloneVmInstanceMsg.class);

            CloneVmInstanceResults res = creator.cloneVm(names, vm.getUuid());

	    String callingChain = MessageCommandRecorder.endAndToString();
	    logger.debug(callingChain);

            numOfClonedVm = res.getNumberOfClonedVm();
        } catch (ApiSenderException e) {
            logger.debug("[bug] " + e);
        }

        Assert.assertTrue(numOfClonedVm == names.toArray().length);
    }
}
