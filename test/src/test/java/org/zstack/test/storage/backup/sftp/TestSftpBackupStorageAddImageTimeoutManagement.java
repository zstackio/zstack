package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.timeout.ApiTimeout;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestSftpBackupStorageAddImageTimeoutManagement {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageAddImageTimeoutManagement.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    ApiTimeoutManager timeoutManager;
    RESTFacade restf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/sftpBackupStorage/TestAddSftpBackupStorage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        timeoutManager = loader.getComponent(ApiTimeoutManager.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        SftpBackupStorageTestHelper helper = new SftpBackupStorageTestHelper();
        SftpBackupStorageInventory sinv = helper.addSimpleHttpBackupStorage(api);
        config.downloadSuccess1 = true;
        config.downloadSuccess2 = true;
        config.imageMd5sum = Platform.getUuid();
        long size = SizeUnit.GIGABYTE.toByte(8);
        ImageInventory iinv = new ImageInventory();
        iinv.setUuid(Platform.getUuid());
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
        iinv.setGuestOsType("CentOS6.3");
        iinv.setName("TestImage");
        iinv.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        iinv.setUrl("http://zstack.org/download/testimage.qcow2");

        config.imageSizes.put(iinv.getUuid(), size);

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

        api.addImage(iinv, sinv.getUuid());

        ApiTimeout at = timeoutManager.getAllTimeout().get(APIAddImageMsg.class);

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
                logger.warn(String.format("API relative[%s] is not captured", clz.getName()));
            }
        }
    }
}