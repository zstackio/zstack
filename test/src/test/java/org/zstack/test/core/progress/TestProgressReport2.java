package org.zstack.test.core.progress;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.progress.ProgressCommands.ProgressReportCmd;
import org.zstack.core.progress.ProgressCommands.ProgressReportResponse;
import org.zstack.core.progress.ProgressReportService;
import org.zstack.header.core.progress.ProgressConstants;
import org.zstack.header.core.progress.ProgressVO;
import org.zstack.header.core.progress.ProgressVO_;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorage;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 *  1. send ReconnectMe command
 *
 *  confirm the reconnect happens
 *
 */
public class TestProgressReport2 {
    CLogger logger = Utils.getLogger(TestProgressReport2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    RESTFacade restf;
    @Autowired
    ProgressReportService service;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestProgressReport.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("Progress.xml");
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        service = loader.getComponent(ProgressReportService.class);
        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
        Assert.assertNotNull(service);
    }
    
	@Test
	public void test() throws InterruptedException, ApiSenderException {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl());
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH);
        String url = ub.build().toUriString();
        Map<String, String> header = map(e(RESTConstant.COMMAND_PATH, ProgressConstants.PROGRESS_START_PATH));

        ImageInventory image = deployer.images.get("TestImage");
	    HostInventory host = deployer.hosts.get("host1");

	    String processType = "AddImage";
	    String resourceuuid = image.getUuid();
	    String serveruuid = host.getUuid();

	    for (int i=0; i<100; i++) {
            ProgressReportCmd cmd = new ProgressReportCmd();
            cmd.setResourceUuid(resourceuuid);
            cmd.setServerUuid(serveruuid);
            cmd.setProgress("0%");
            cmd.setProcessType(processType);
            cmd.setServerType(BackupStorage.class.getTypeName());
            restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, ProgressReportResponse.class);
            TimeUnit.MILLISECONDS.sleep(10);

            header = map(e(RESTConstant.COMMAND_PATH, ProgressConstants.PROGRESS_REPORT_PATH));
            ProgressReportCmd cmd1 = new ProgressReportCmd();
            cmd1.setResourceUuid(image.getUuid());
            cmd1.setServerUuid(host.getUuid());
            cmd1.setProgress("50%");
            cmd1.setProcessType("AddImage");
            cmd1.setServerType(BackupStorage.class.getTypeName());
            restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd1), header, ProgressReportResponse.class);

        }

        TimeUnit.MILLISECONDS.sleep(2000);
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, processType);
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, resourceuuid);
        ProgressVO vo = q.find();
        Assert.assertEquals("50%", vo.getProgress());


        /*header = map(e(RESTConstant.COMMAND_PATH, ProgressConstants.PROGRESS_FINISH_PATH));
        ProgressReportCmd cmd2 = new ProgressReportCmd();
        cmd2.setResourceUuid(image.getUuid());
        cmd2.setServerUuid(host.getUuid());
        cmd2.setProgress("100%");
        cmd2.setProcessType("AddImage");
        cmd2.setServerType(BackupStorage.class.getTypeName());
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd2), header, ProgressReportResponse.class);

        TimeUnit.MILLISECONDS.sleep(2000);
        SimpleQuery<ProgressVO> q1 = dbf.createQuery(ProgressVO.class);
        q1.add(ProgressVO_.processType, SimpleQuery.Op.EQ, processType);
        q1.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, resourceuuid);
        Assert.assertFalse(q1.isExists());*/

    }
}
