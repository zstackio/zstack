package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.volume.APIGetVolumeFormatReply.VolumeFormatReplyStruct;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.kvm.KVMConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

public class TestGetVolumeFormat {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestQueryVolume.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        List<VolumeFormatReplyStruct> structs = api.getVolumeFormats();

        VolumeFormatReplyStruct qcow2 = CollectionUtils.find(structs, new Function<VolumeFormatReplyStruct, VolumeFormatReplyStruct>() {
            @Override
            public VolumeFormatReplyStruct call(VolumeFormatReplyStruct arg) {
                return arg.getFormat().equals(VolumeConstant.VOLUME_FORMAT_QCOW2) ? arg : null;
            }
        });
        Assert.assertNotNull(qcow2);
        Assert.assertEquals(VolumeConstant.VOLUME_FORMAT_QCOW2, qcow2.getFormat());
        Assert.assertEquals(KVMConstant.KVM_HYPERVISOR_TYPE, qcow2.getMasterHypervisorType());
        Assert.assertFalse(qcow2.getSupportingHypervisorTypes().isEmpty());

        VolumeFormatReplyStruct raw = CollectionUtils.find(structs, new Function<VolumeFormatReplyStruct, VolumeFormatReplyStruct>() {
            @Override
            public VolumeFormatReplyStruct call(VolumeFormatReplyStruct arg) {
                return arg.getFormat().equals(VolumeConstant.VOLUME_FORMAT_RAW) ? arg : null;
            }
        });
        Assert.assertNotNull(raw);
        Assert.assertEquals(VolumeConstant.VOLUME_FORMAT_RAW, raw.getFormat());
        Assert.assertEquals(KVMConstant.KVM_HYPERVISOR_TYPE, raw.getMasterHypervisorType());
        Assert.assertFalse(raw.getSupportingHypervisorTypes().isEmpty());
    }

}
