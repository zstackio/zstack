package org.zstack.test.applianceVm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Map;

public class TestChangeOwnerOfEipOfVm {
    CLogger logger = Utils.getLogger(TestChangeOwnerOfEipOfVm.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestChangeOwnerOfEipOfVm.xml", con);
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.addSpringConfig("flatNetworkProvider.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    private String getBridgeName(String l3uuid) {
        L3NetworkVO l3 = dbf.findByUuid(l3uuid, L3NetworkVO.class);
        return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l3.getL2NetworkUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
    }

    @Test
    public void test() throws ApiSenderException {
        IdentityCreator identityCreator = new IdentityCreator(api);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        EipInventory eip = deployer.eips.get("eip");

        identityCreator.useAccount("test2");
        String targetAccountUuid = identityCreator.getAccountSession().getAccountUuid();
        api.changeResourceOwner(vm.getUuid(), targetAccountUuid);

        ArrayList<String> resUuids = new ArrayList<>();
        resUuids.add(eip.getUuid());
        Map<String, AccountInventory> resAccMap = api.getResourceAccount(resUuids);
        assert (resAccMap.size() == resUuids.size());

        for (AccountInventory ai : resAccMap.values()) {
            logger.debug(ai.getUuid());
            logger.debug(ai.getType());
            logger.debug("targetAccountUuid:" + targetAccountUuid);
            assert (ai.getUuid().equals(targetAccountUuid));
        }
    }
}
