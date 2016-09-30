package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.timeout.ApiTimeout;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestCreateTemplateFromRootVolumeTimeoutManagement {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    RESTFacade restf;
    ApiTimeoutManager timeoutManager;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/image/TestCreateTemplateFromRootVolume.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);
        timeoutManager = loader.getComponent(ApiTimeoutManager.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        String rootVolumeUuid = vm.getRootVolumeUuid();
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");


        final Map<String, Message> msgs = new HashMap<String, Message>();
        final Map<String, Long> commands = new HashMap<String, Long>();
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                msgs.put(msg.getClass().getName(), msg);
            }
        });
        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                commands.put(body.getClass().getName(), TimeUnit.MILLISECONDS.convert(timeout, unit));
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
            }
        });

        api.createTemplateFromRootVolume("testImage", rootVolumeUuid, sftp.getUuid());
        ApiTimeout at = timeoutManager.getAllTimeout().get(APICreateRootVolumeTemplateFromRootVolumeMsg.class);


        List<String> relativeNames = CollectionUtils.transformToList(at.getRelatives(), new Function<String, Class>() {
            @Override
            public String call(Class arg) {
                return arg.getName();
            }
        });

        List<String> tested = new ArrayList<String>();

        for (Message msg : msgs.values()) {
            if (msg instanceof KVMHostAsyncHttpCallMsg) {
                final KVMHostAsyncHttpCallMsg kmsg = (KVMHostAsyncHttpCallMsg) msg;
                if (relativeNames.contains(kmsg.getCommandClassName())) {
                    long commandTimeout = kmsg.getCommandTimeout();
                    Assert.assertEquals(String.format("timeout mismatch for %s, expected: %s, actual: %s", kmsg.getCommandClassName(),
                            at.getTimeout(), commandTimeout), at.getTimeout(), commandTimeout);
                    tested.add(kmsg.getCommandClassName());
                }
            }
        }

        for (Class clz : at.getRelatives()) {
            Message msg = msgs.get(clz.getName());
            Long commandTimeout = commands.get(clz.getName());

            if (msg != null && msg instanceof NeedReplyMessage) {
                NeedReplyMessage nmsg = (NeedReplyMessage) msg;
                Assert.assertEquals(String.format("timeout mismatch for %s, expected: %s, actual: %s", msg.getClass().getName(),
                        at.getTimeout(), nmsg.getTimeout()), at.getTimeout(), nmsg.getTimeout());
            } else if (commandTimeout != null) {
                Assert.assertEquals(String.format("timeout mismatch for %s, expected: %s, actual: %s", clz.getName(),
                        at.getTimeout(), commandTimeout), at.getTimeout(), commandTimeout.longValue());
            } else {
                if (!tested.contains(clz.getName())) {
                    logger.warn(String.format("API relative[%s] is not captured", clz.getName()));
                }
            }
        }
    }
}
