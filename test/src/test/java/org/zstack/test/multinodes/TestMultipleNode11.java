package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * create node0 with some non-restartable jobs
 * create node1 with some restartable jobs
 * all jobs are in the same queue
 * stop node0
 * make sure all jobs on node0 are deleted
 * all jobs on node1 are restarted
 */
public class TestMultipleNode11 {
    CLogger logger = Utils.getLogger(TestMultipleNode11.class);
    ComponentLoader loader;
    DatabaseFacade dbf;
    NodeManager nodeMgr;
    CloudBus bus;
    Api api;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        con.addXml("AccountManager.xml");
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        nodeMgr = new NodeManager();
        api = new Api();
        api.startServer();
        api.setTimeout(300);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, IOException {
        try {
            nodeMgr.setProperties("-DbFacadeDataSource.maxPoolSize=50")
                    .createLightWeightNodeWithCore(3, 300, Arrays.asList("silentService.xml", "silentJobService.xml"));
            List<ManagementNodeInventory> nodes = api.listManagementNodes();

            Iterator<ManagementNodeInventory> it = nodes.iterator();
            while (it.hasNext()) {
                if (it.next().getUuid().equals(Platform.getManagementServerId())) {
                    it.remove();
                }
            }

            ManagementNodeInventory node0 = nodes.get(0);
            ManagementNodeInventory node1 = nodes.get(1);
            ManagementNodeInventory node2 = nodes.get(2);

            SilentJobMsg msg = new SilentJobMsg();
            msg.setJobNum(50);
            bus.makeServiceIdByManagementNodeId(msg, SilentJobService.SERVICE_ID, node0.getUuid());
            bus.call(msg);

            msg = new SilentJobMsg();
            msg.setJobNum(50);
            bus.makeServiceIdByManagementNodeId(msg, SilentJobService.SERVICE_ID, node1.getUuid());
            SilentJobReply reply = (SilentJobReply) bus.call(msg);
            List<String> retUuids = reply.getJobUuids();

            msg = new SilentJobMsg();
            msg.setJobNum(50);
            msg.setRestartable(true);
            bus.makeServiceIdByManagementNodeId(msg, SilentJobService.SERVICE_ID, node2.getUuid());
            reply = (SilentJobReply) bus.call(msg);
            retUuids.addAll(reply.getJobUuids());

            nodeMgr.stopNode(node0.getUuid(), 120);

            TimeUnit.SECONDS.sleep(5);

            File passFolder = new File(RestartableSilentJob.PASS_FOLDER);
            if (passFolder.exists()) {
                FileUtils.deleteDirectory(passFolder);
            }
            FileUtils.forceMkdir(passFolder);

            for (String uuid : retUuids) {
                File f = new File(PathUtil.join(passFolder.getAbsolutePath(), uuid));
                f.createNewFile();
                logger.debug(String.format("create pass file %s", f.getAbsolutePath()));
            }

            TimeUnit.SECONDS.sleep(15);

            File resFolder = new File(RestartableSilentJob.RESULT_FILE);
            Assert.assertEquals(retUuids.size(), resFolder.list().length);
            for (String uuid : retUuids) {
                File resFile = new File(PathUtil.join(RestartableSilentJob.RESULT_FILE, uuid));
                if (!resFile.exists()) {
                    Assert.fail(String.format("cannot find result file:%s", resFile.getAbsolutePath()));
                }
            }

        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
