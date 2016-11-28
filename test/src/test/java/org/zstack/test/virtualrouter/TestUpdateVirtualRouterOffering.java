package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.data.SizeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. update the only vr offering to non-default by admin
 * <p>
 * confirm the offering is still the default
 * <p>
 * 2. create a default vr offering by a normal account
 * <p>
 * confirm failure
 * <p>
 * 3. create a non-default vr offering by a normal account
 * 4. update the non-default vr offering to the default one
 * <p>
 * confirm failure
 * <p>
 * 5. create another default vr offering by the admin
 * <p>
 * confirm success
 */
public class TestUpdateVirtualRouterOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterOffering.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        InstanceOfferingInventory iinv = deployer.instanceOfferings.get("virtualRouterOffering");

        VirtualRouterOfferingInventory vroffering = VirtualRouterOfferingInventory.valueOf(dbf.findByUuid(iinv.getUuid(), VirtualRouterOfferingVO.class));
        vroffering.setDefault(false);
        vroffering = api.updateVirtualRouterOffering(vroffering);
        Assert.assertFalse(vroffering.isDefault());

        vroffering.setDefault(true);
        vroffering = api.updateVirtualRouterOffering(vroffering);
        Assert.assertTrue(vroffering.isDefault());

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.createAccount("a1", "password");

        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        ImageInventory img = deployer.images.get("TestImage");
        ZoneInventory zone = deployer.zones.get("Zone1");

        api.shareResource(list(l31.getUuid(), img.getUuid()), null, true);

        VirtualRouterOfferingInventory vr = new VirtualRouterOfferingInventory();
        vr.setName("vr");
        vr.setCpuNum(1);
        vr.setCpuSpeed(1);
        vr.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        vr.setZoneUuid(zone.getUuid());
        vr.setPublicNetworkUuid(l31.getUuid());
        vr.setManagementNetworkUuid(l31.getUuid());
        vr.setImageUuid(img.getUuid());

        SessionInventory session = identityCreator.getAccountSession();

        boolean s = false;
        try {
            vr.setDefault(true);
            api.createVirtualRouterOffering(vr, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        vr.setDefault(null);
        vr = api.createVirtualRouterOffering(vr, session);

        s = false;
        try {
            vr.setDefault(true);
            api.updateVirtualRouterOffering(vr, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);

        vr.setDefault(true);
        vr = api.createVirtualRouterOffering(vr);
        Assert.assertTrue(vr.isDefault());

        VirtualRouterOfferingVO old = dbf.findByUuid(vroffering.getUuid(), VirtualRouterOfferingVO.class);
        Assert.assertFalse(old.isDefault());
    }
}
