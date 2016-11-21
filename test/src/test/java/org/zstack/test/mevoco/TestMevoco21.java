package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.ha.APISetVmInstanceHaLevelMsg;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
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
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        validate(ps, "VM.CONSOLE", "console:APIRequestConsoleAccessMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.ISO.ADD", "instance:APIAttachIsoToVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.ISO.REMOVE", "instance:APIDetachIsoFromVmInstanceMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.MIGRATE", "instance:APIMigrateVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.L3.ATTACH", "instance:APIAttachL3NetworkToVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.L3.DETACH", "instance:APIDetachL3NetworkFromVmMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.INSTANCE-OFFERING.CHANGE", "instance:APIChangeInstanceOfferingMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.STATIC-IP.SET", "instance:APISetVmStaticIpMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.STATIC-IP.DELETE", "instance:APIDeleteVmStaticIpMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.HOSTNAME.SET", "instance:APISetVmHostnameMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.HOSTNAME.DELETE", "instance:APIDeleteVmHostnameMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.HA-LEVEL.SET", "ha:APISetVmInstanceHaLevelMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.HA-LEVEL.DELETE", "ha:APIDeleteVmInstanceHaLevelMsg", AccountConstant.StatementEffect.Allow);

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

        validate(ps, "IMAGE.ADD", "image:APIAddImageMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "IMAGE.EXPUNGE", "image:APIExpungeImageMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "IMAGE.DELETE", "image:APIDeleteImageMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "IMAGE.RECOVER", "image:APIRecoverImageMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "IMAGE.CHANGE-STATE", "image:APIChangeImageStateMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "IMAGE.CREATE-FROM-ROOT-VOLUME", "image:APICreateRootVolumeTemplateFromRootVolumeMsg", AccountConstant.StatementEffect.Allow);

        validate(ps, "VIP.CREATE", "vip:APICreateVipMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "VIP.DELETE", "vip:APIDeleteVipMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "EIP.CREATE", "eip:APICreateEipMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "EIP.DELETE", "eip:APIDeleteEipMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "EIP.UPDATE", "eip:APIUpdateEipMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "EIP.ATTACH", "eip:APIAttachEipMsg", AccountConstant.StatementEffect.Allow);
        validate(ps, "EIP.DETACH", "eip:APIDetachEipMsg", AccountConstant.StatementEffect.Allow);

        validate(ps, "VM.SSH-KEY.SET", "instance:APISetVmSshKeyMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.SSH-KEY.DELETE", "instance:APIDeleteVmSshKeyMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.CONSOLE-PASSWORD.SET", "instance:APISetVmConsolePasswordMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "VM.CONSOLE-PASSWORD.DELETE", "instance:APIDeleteVmConsolePasswordMsg",
                AccountConstant.StatementEffect.Allow);
        
        validate(ps, "SCHEDULER.CREATE", "instance:APICreateRebootVmInstanceSchedulerMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "SCHEDULER.CREATE", "instance:APICreateStartVmInstanceSchedulerMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "SCHEDULER.CREATE", "instance:APICreateStopVmInstanceSchedulerMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "SCHEDULER.CREATE", "volume:APICreateVolumeSnapshotSchedulerMsg",
                AccountConstant.StatementEffect.Allow);

        validate(ps, "SCHEDULER.DELETE", "scheduler:APIDeleteSchedulerMsg",
                AccountConstant.StatementEffect.Allow);
        validate(ps, "SCHEDULER.UPDATE", "scheduler:APIUpdateSchedulerMsg",
                AccountConstant.StatementEffect.Allow);

        APIQueryPolicyMsg qmsg = new APIQueryPolicyMsg();
        qmsg.addQueryCondition("accountUuid", QueryOp.EQ, test.getUuid());
        APIQueryPolicyReply r = api.query(qmsg, APIQueryPolicyReply.class, creator.getAccountSession());
        Assert.assertFalse(r.getInventories().isEmpty());

        UserInventory user = creator.createUser("user", "password");
        qmsg = new APIQueryPolicyMsg();
        qmsg.addQueryCondition("user.uuid", QueryOp.EQ, user.getUuid());
        qmsg.addQueryCondition("name", QueryOp.EQ, "DEFAULT-READ");
        r = api.query(qmsg, APIQueryPolicyReply.class, creator.getAccountSession());
        Assert.assertEquals(1, r.getInventories().size());

        PolicyVO consolePolicy = CollectionUtils.find(ps, new Function<PolicyVO, PolicyVO>() {
            @Override
            public PolicyVO call(PolicyVO arg) {
                return "VM.CONSOLE".equals(arg.getName()) ? arg : null;
            }
        });

        Assert.assertNotNull(consolePolicy);

        api.attachPolicesToUser(user.getUuid(), list(consolePolicy.getUuid()), creator.getAccountSession());

        Map<String, String> pret = api.checkUserPolicy(list(APIRequestConsoleAccessMsg.class.getName()), user.getUuid(), creator.getAccountSession());

        Assert.assertEquals(1, pret.size());
        String decision = pret.get(APIRequestConsoleAccessMsg.class.getName());
        Assert.assertEquals("Allow", decision);

        PolicyVO setHa = CollectionUtils.find(ps, new Function<PolicyVO, PolicyVO>() {
            @Override
            public PolicyVO call(PolicyVO arg) {
                return "VM.HA-LEVEL.SET".equals(arg.getName()) ? arg : null;
            }
        });

        api.attachPolicesToUser(user.getUuid(), list(setHa.getUuid()), api.getAdminSession());
        pret = api.checkUserPolicy(list(APISetVmInstanceHaLevelMsg.class.getName()), user.getUuid(), creator.getAccountSession());
        decision = pret.values().iterator().next();
        Assert.assertEquals("Allow", decision);
    }
}
