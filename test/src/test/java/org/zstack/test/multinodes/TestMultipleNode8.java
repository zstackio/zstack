package org.zstack.test.multinodes;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * start jobs on one node0, jobs are set to not run
 * change jobs workNodeManagementId to current node
 * stop node0, allow job to run
 * check jobs are run
 */
public class TestMultipleNode8 {
    CLogger logger = Utils.getLogger(TestMultipleNode8.class);
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
                    .createLightWeightNodeWithCore(1, 300, Arrays.asList("silentService.xml", "silentJobService.xml"));
            List<ManagementNodeInventory> nodes = api.listManagementNodes();
            ManagementNodeInventory target = CollectionUtils.find(nodes, new Function<ManagementNodeInventory, ManagementNodeInventory>() {
                @Override
                public ManagementNodeInventory call(final ManagementNodeInventory arg) {
                    if (!arg.getUuid().equals(Platform.getManagementServerId())) {
                        return arg;
                    }
                    return null;
                }
            });

            SilentJobMsg msg = new SilentJobMsg();
            msg.setJobNum(50);
            bus.makeServiceIdByManagementNodeId(msg, SilentJobService.SERVICE_ID, target.getUuid());
            SilentJobReply reply = (SilentJobReply) bus.call(msg);
            List<String> retUuids = reply.getJobUuids();

            new Runnable() {
                @Override
                @Transactional
                public void run() {
                    String sql = "update JobQueueEntryVO e set e.issuerManagementNodeId = :uuid";
                    Query q = dbf.getEntityManager().createQuery(sql);
                    q.setParameter("uuid", Platform.getManagementServerId());
                    q.executeUpdate();
                }
            }.run();

            nodeMgr.stopNode(0, 120);

            TimeUnit.SECONDS.sleep(5);

            // the first one is skipped because it's not restartable and in Processing state in previous
            // execution
            String falseJobUuid = retUuids.get(0);
            retUuids.remove(0);

            File passFolder = new File(SilentJob.PASS_FOLDER);
            if (passFolder.exists()) {
                FileUtils.deleteDirectory(passFolder);
            }
            FileUtils.forceMkdir(passFolder);

            for (String uuid : retUuids) {
                File f = new File(PathUtil.join(passFolder.getAbsolutePath(), uuid));
                f.createNewFile();
                logger.debug(String.format("create pass file %s", f.getAbsolutePath()));
            }

            TimeUnit.SECONDS.sleep(5);

            File falseJobFile = new File(PathUtil.join(SilentJob.RESULT_FILE, falseJobUuid));
            Assert.assertFalse(falseJobFile.exists());
            for (String uuid : retUuids) {
                File resFile = new File(PathUtil.join(SilentJob.RESULT_FILE, uuid));
                if (!resFile.exists()) {
                    Assert.fail(String.format("cannot find result file:%s", resFile.getAbsolutePath()));
                }
            }

        } finally {
            nodeMgr.stopNodes(120);
        }
    }
}
