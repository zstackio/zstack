package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

/**
 * 1. create a vm
 * 2. attach an iso to the vm
 *
 * confirm the iso attached successfully
 *
 * 3. detach the iso
 *
 * confirm the iso detached successfully
 *
 * 4. stop the vm
 * 5. attach the iso
 *
 * confirm the iso attached successfully
 *
 * 6. start the vm
 *
 * confirm the iso is attached
 *
 * 7. stop the vm
 * 8. detach the iso
 *
 * confirm the iso detached successfully
 *
 * 9. start the vm
 * 10. attach the iso
 * 11. delete the iso
 * 12. detach the iso
 *
 * confirm the iso detached successfully
 */
public class TestAttachIsoOnKvm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestAttachIsoOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        ImageInventory iso = deployer.images.get("TestIso");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        api.attachIso(vm.getUuid(), iso.getUuid(), null);
        Assert.assertFalse(config.attachIsoCmds.isEmpty());
        AttachIsoCmd cmd = config.attachIsoCmds.get(0);
        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, iso.getUuid());
        ImageCacheVO cvo = q.find();
        Assert.assertEquals(cvo.getInstallUrl(), cmd.iso.getPath());
        Assert.assertEquals(vm.getUuid(), cmd.vmUuid);
        String isoUuid = VmSystemTags.ISO.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.ISO_TOKEN);
        Assert.assertEquals(iso.getUuid(), isoUuid);

        api.detachIso(vm.getUuid(), null);
        Assert.assertFalse(config.detachIsoCmds.isEmpty());
        DetachIsoCmd dcmd = config.detachIsoCmds.get(0);
        Assert.assertEquals(vm.getUuid(), dcmd.vmUuid);
        Assert.assertEquals(isoUuid, dcmd.isoUuid);
        isoUuid = VmSystemTags.ISO.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.ISO_TOKEN);
        Assert.assertNull(isoUuid);

        api.stopVmInstance(vm.getUuid());
        config.attachIsoCmds.clear();
        api.attachIso(vm.getUuid(), iso.getUuid(), null);
        Assert.assertTrue(config.attachIsoCmds.isEmpty());
        isoUuid = VmSystemTags.ISO.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.ISO_TOKEN);
        Assert.assertNotNull(isoUuid);

        api.startVmInstance(vm.getUuid());
        StartVmCmd scmd = config.startVmCmd;
        Assert.assertNotNull(scmd.getBootIso());
        Assert.assertEquals(cvo.getInstallUrl(), scmd.getBootIso().getPath());
        Assert.assertEquals(iso.getUuid(), scmd.getBootIso().getImageUuid());

        api.stopVmInstance(vm.getUuid());
        config.detachIsoCmds.clear();
        api.detachIso(vm.getUuid(), null);
        Assert.assertTrue(config.detachIsoCmds.isEmpty());
        isoUuid = VmSystemTags.ISO.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.ISO_TOKEN);
        Assert.assertNull(isoUuid);

        api.startVmInstance(vm.getUuid());
        api.attachIso(vm.getUuid(), iso.getUuid(), null);
        api.deleteImage(iso.getUuid());
        config.detachIsoCmds.clear();
        api.detachIso(vm.getUuid(), null);
        Assert.assertFalse(config.detachIsoCmds.isEmpty());
        dcmd = config.detachIsoCmds.get(0);
        Assert.assertEquals(vm.getUuid(), dcmd.vmUuid);
        Assert.assertEquals(iso.getUuid(), dcmd.isoUuid);
        isoUuid = VmSystemTags.ISO.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.ISO_TOKEN);
        Assert.assertNull(isoUuid);
    }
}
