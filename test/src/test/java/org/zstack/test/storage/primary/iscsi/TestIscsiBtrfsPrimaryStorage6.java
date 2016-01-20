package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.BootDev;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.kvm.KVMConstant;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageVO;
import org.zstack.storage.primary.iscsi.IscsiVolumePath;
import org.zstack.storage.primary.iscsi.KVMIscsiIsoTO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * 1. create VM using ISO with IscsiBtrfsPrimaryStorage
 *
 * confirm the ISO related parameters are passed down to KVM host
 *
 */
public class TestIscsiBtrfsPrimaryStorage6 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;
    IscsiBtrfsPrimaryStorageSimulatorConfig iconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/iscsiBtrfsPrimaryStorage/TestIscsiBtrfsPrimaryStorage1.xml", con);
        deployer.addSpringConfig("iscsiBtrfsPrimaryStorage.xml");
        deployer.addSpringConfig("iscsiFileSystemPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        iconfig = loader.getComponent(IscsiBtrfsPrimaryStorageSimulatorConfig.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory iso = deployer.images.get("TestImage");
        SimpleQuery query = dbf.createQuery(ImageCacheVO.class);
        query.add(ImageCacheVO_.imageUuid, Op.EQ, iso.getUuid());
        ImageCacheVO isoCache = (ImageCacheVO) query.find();
        Assert.assertNotNull(isoCache);

        IscsiFileSystemBackendPrimaryStorageVO iscsi = dbf.listAll(IscsiFileSystemBackendPrimaryStorageVO.class).get(0);

        StartVmCmd scmd = kconfig.startVmCmd;
        Assert.assertNotNull(scmd);

        Assert.assertEquals(1, iconfig.createSubVolumeCmds.size());

        Assert.assertEquals(BootDev.cdrom.toString(), scmd.getBootDev().get(0));
        IscsiVolumePath path = new IscsiVolumePath(scmd.getBootIso().getPath());
        path.disassemble();
        Assert.assertEquals(iscsi.getHostname(), path.getHostname());
    }
}
