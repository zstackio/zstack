package org.zstack.test.multinodes;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.AbstractService;
import org.zstack.header.Service;
import org.zstack.header.message.Message;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
public class ManagementNodeTester {
    CLogger logger = Utils.getLogger(ManagementNodeTester.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBusIN bus;

    @Before
    public void setUp() throws Exception {
        boolean deployDB = Boolean.parseBoolean(System.getProperty("deployDB"));
        String deployConfig = System.getProperty("deployConfig");
        final String serviceId = System.getProperty("serviceId");
        boolean loadAll = Boolean.valueOf(System.getProperty("loadAll"));
        int port = Integer.valueOf(System.getProperty("port"));
        String springConfigs = System.getProperty("springConfigs");
        String dbPoolSize = System.getProperty("DbFacadeDataSource.maxPoolSize");
        if (dbPoolSize != null) {
            System.setProperty("DbFacadeDataSource.maxPoolSize", dbPoolSize);
        }

        List<String> springConfs = new ArrayList<String>();
        if (springConfigs != null) {
            springConfs.addAll(Arrays.asList(springConfigs.split(",")));
        }

        if (deployDB) {
            DBUtil.reDeployDB();
        }

        WebBeanConstructor con = new WebBeanConstructor();
        con.setPort(port);
        if (deployConfig != null) {
            deployer = new Deployer(deployConfig, con);
            for (String conf : springConfs) {
                deployer.addSpringConfig(conf);
            }
            deployer.build();
            api = deployer.getApi();
            loader = deployer.getComponentLoader();
        } else {
            if (loadAll) {
                con.addAllConfigInZstackXml();
            } else {
                for (String conf : springConfs) {
                    con.addXml(conf);
                }
            }

            loader = con.build();
            api = new Api();
            api.startServer();
        }

        api.setTimeout(300);
        bus = loader.getComponent(CloudBusIN.class);

        Service serv = new AbstractService() {
            @Override
            public void handleMessage(Message msg) {
                MultiNodeTestMsg nmsg = (MultiNodeTestMsg) msg;
                if (nmsg.isCode(nmsg.EXIT)) {
                    MultiNodeTestReply reply = new MultiNodeTestReply();
                    bus.reply(msg, reply);
                    System.exit(0);
                } else if (nmsg.isCode(nmsg.READY)) {
                    MultiNodeTestReply reply = new MultiNodeTestReply();
                    reply.setManagementNodeId(Platform.getManagementServerId());
                    bus.reply(msg, reply);
                } else {
                    bus.dealWithUnknownMessage(msg);
                }
            }

            @Override
            public String getId() {
                return serviceId;
            }

            @Override
            public boolean start() {
                return true;
            }

            @Override
            public boolean stop() {
                return true;
            }
        };

        bus.registerService(serv);
        bus.activeService(serv);
    }

    @Test
    public void test() throws InterruptedException {
        TimeUnit.DAYS.sleep(1);
    }
}
