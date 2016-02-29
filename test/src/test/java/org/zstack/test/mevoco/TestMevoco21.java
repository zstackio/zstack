package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.*;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * test predefined policies
 */
public class TestMevoco21 {
    CLogger logger = Utils.getLogger(TestMevoco21.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    private void validate(List<PolicyVO> ps, String policyName, String statementAction, AccountConstant.StatementEffect effect) {
        for (PolicyVO vo : ps) {
            if (vo.getName().equals(policyName)) {
                List<PolicyInventory.Statement> ss = JSONObjectUtil.toCollection(vo.getData(), ArrayList.class, PolicyInventory.Statement.class);
                for (PolicyInventory.Statement s : ss) {
                    for (String action : s.getActions()) {
                        if (action.equals(statementAction) && effect == s.getEffect()) {
                            return;
                        }
                    }
                }
            }
        }

        Assert.fail(String.format("cannot find policy[name: %s, action: %s, effect: %s\n\n %s", policyName, statementAction,
                effect, JSONObjectUtil.toJsonString(PolicyInventory.valueOf(ps))));
    }

    /*
    private void validate(List<PolicyVO> ps, String policyName, List<String> statementActions, AccountConstant.StatementEffect effect) {
        for (PolicyVO vo : ps) {
            if (vo.getName().equals(policyName)) {
                List<PolicyInventory.Statement> ss = JSONObjectUtil.toCollection(vo.getData(), ArrayList.class, PolicyInventory.Statement.class);
                for (PolicyInventory.Statement s : ss) {
                    if (s.getActions().containsAll(statementActions) && effect == s.getEffect()) {
                        return;
                    }
                }
            }
        }

        Assert.fail(String.format("cannot find policy[name: %s, action: %s, effect: %s\n\n %s", policyName, statementActions,
                effect, JSONObjectUtil.toJsonString(PolicyInventory.valueOf(ps))));
    }
    */
    
	@Test
	public void test() throws ApiSenderException {
        IdentityCreator creator = new IdentityCreator(api);
        AccountInventory test = creator.createAccount("test", "test");

        SimpleQuery<PolicyVO> q = dbf.createQuery(PolicyVO.class);
        q.add(PolicyVO_.accountUuid, SimpleQuery.Op.EQ, test.getUuid());
        List<PolicyVO> ps = q.list();

        validate(ps, "VM.CREATE", "instance:APICreateVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.UPDATE", "instance:APIUpdateVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.START", "instance:APIStartVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.STOP", "instance:APIStopVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.REBOOT", "instance:APIRebootVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.DESTROY", "instance:APIDestroyVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.RECOVER", "instance:APIRecoverVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.EXPUNGE", "instance:APIExpungeVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.CONSOLE", "instance:APIRequestConsoleAccessMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.ISO.ADD", "instance:APIAttachIsoToVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.ISO.REMOVE", "instance:APIDetachIsoFromVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.MIGRATE", "instance:APIMigrateVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.L3.ATTACH", "instance:APIAttachL3NetworkToVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.L3.DETACH", "instance:APIDetachL3NetworkFromVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.INSTANCE-OFFERING.CHANGE", "instance:APIChangeInstanceOfferingMsg", AccountConstant.StatementEffect.Allow);

        validate(ps, "VOLUME.CREATE", "volume:APICreateDataVolumeMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.UPDATE", "volume:APIUpdateVolumeMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.ATTACH", "volume:APIAttachDataVolumeToVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.DETACH", "volume:APIDetachDataVolumeFromVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.CHANGE-STATE", "volume:APIChangeVolumeStateMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.DELETE", "volume:APIDeleteDataVolumeMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.EXPUNGE", "volume:APIExpungeDataVolumeMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.SNAPSHOT.CREATE", "volumeSnapshot:APICreateVolumeSnapshotMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.SNAPSHOT.UPDATE", "volumeSnapshot:APIUpdateVolumeSnapshotMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.SNAPSHOT.DELETE", "volumeSnapshot:APIDeleteVolumeSnapshotMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VOLUME.SNAPSHOT.REVERT", "volumeSnapshot:APIRevertVolumeFromSnapshotMsg", AccountConstant.StatementEffect.Allow);

        validate(ps, "SECURITY-GROUP.CREATE", "securityGroup:APICreateSecurityGroupMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.UPDATE", "securityGroup:APIUpdateSecurityGroupMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.CHANGE-STATE", "securityGroup:APIChangeSecurityGroupStateMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.DELETE", "securityGroup:APIDeleteSecurityGroupMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.ADD-NIC", "securityGroup:APIAddVmNicToSecurityGroupMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.REMOVE-NIC", "securityGroup:APIDeleteVmNicFromSecurityGroupMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.ADD-RULE", "securityGroup:APIAddSecurityGroupRuleMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "SECURITY-GROUP.REMOVE-RULE", "securityGroup:APIDeleteSecurityGroupRuleMsg", AccountConstant.StatementEffect.Allow);

        APIQueryPolicyMsg qmsg = new APIQueryPolicyMsg();
        qmsg.addQueryCondition("accountUuid", QueryOp.EQ, test.getUuid());
        APIQueryPolicyReply r = api.query(qmsg, APIQueryPolicyReply.class, creator.getAccountSession());
        Assert.assertFalse(r.getInventories().isEmpty());
    }
}
