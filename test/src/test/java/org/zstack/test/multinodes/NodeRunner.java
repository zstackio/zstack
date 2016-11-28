package org.zstack.test.multinodes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NodeRunner {
    private static final CLogger logger = Utils.getLogger(NodeRunner.class);
    private Process process;

    @Autowired
    private CloudBus bus;

    private String serviceId;
    private String deployConf;
    private List<String> springConfigs;
    private Integer port;
    private Boolean deployDB;
    private String propertyString;
    private int uuid;
    private boolean loadAll;

    private String managementNodeId;

    private volatile boolean isRunning = false;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDeployConf() {
        return deployConf;
    }

    public void setDeployConf(String deployConf) {
        this.deployConf = deployConf;
    }

    public List<String> getSpringConfigs() {
        return springConfigs;
    }

    public void setSpringConfigs(List<String> springConfigs) {
        this.springConfigs = springConfigs;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getDeployDB() {
        return deployDB;
    }

    public void setDeployDB(Boolean deployDB) {
        this.deployDB = deployDB;
    }

    public String getManagementNodeId() {
        return managementNodeId;
    }

    private String createConfFromTemplate(String tmptName, String confFmt, Map<String, String> tokens) {
        File tmpt = PathUtil.findFileOnClassPath(tmptName);
        String conf = String.format(confFmt, serviceId);
        try {
            String tmptContent = FileUtils.readFileToString(tmpt);
            tmptContent = s(tmptContent).formatByMap(tokens);
            FileUtils.writeStringToFile(new File(PathUtil.join(tmpt.getParentFile().getAbsolutePath(), conf)), tmptContent);
            return conf;
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public String toString() {
        assert port != null;
        assert serviceId != null;

        String logname = String.format("management-%s.log", serviceId);
        String logconf = createConfFromTemplate("log4j-tmpt.xml", "log4j2-%s", map(e("managementLogName", logname)));
        String quartzJdbcConf = createConfFromTemplate("zstack-jdbc-quartz-tmpt.properties", "zstack-jdbc-quartz-%s.properties", map(e("instanceName", serviceId)));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("-Dport=%s ", port));
        sb.append(String.format("-DserviceId=%s ", serviceId));
        sb.append(String.format("-Dlog4j.configurationFile=%s ", logconf));
        sb.append(String.format("-DquartzJdbc=%s ", quartzJdbcConf));
        sb.append(String.format("-DquartzJdbc=%s ", quartzJdbcConf));
        if (loadAll) {
            sb.append(String.format("-DloadAll=true "));
        }
        if (deployDB != null) {
            sb.append(String.format("-DdeployDB=%s ", deployDB));
        }
        if (deployConf != null) {
            sb.append(String.format("-DdeployConfig=%s ", deployConf));
        }
        if (springConfigs != null) {
            String sconf = StringUtils.join(springConfigs, ",");
            sb.append(String.format("-DspringConfigs=%s ", sconf));
        }
        if (propertyString != null) {
            sb.append(propertyString);
        }
        return sb.toString();
    }


    public boolean waitNodeStart(int timeout) {
        isRunning = true;

        assert serviceId != null;
        MultiNodeTestMsg msg = new MultiNodeTestMsg();
        msg.setServiceId(serviceId);
        msg.setOpCode(msg.READY);
        msg.setTimeout(TimeUnit.SECONDS.toMillis(1));
        long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout);
        do {
            MessageReply r = bus.call(msg);
            if (!r.isSuccess()) {
                if (!r.getError().getCode().equals(SysErrors.NO_ROUTE_ERROR.toString()) &&
                        !r.getError().getCode().equals(SysErrors.TIMEOUT.toString())) {
                    throw new CloudRuntimeException(String.format("node[%s] fail to start, %s", serviceId, r.getError()));
                }
            } else {
                MultiNodeTestReply mr = (MultiNodeTestReply) r;
                managementNodeId = mr.getManagementNodeId();
                break;
            }

            if (System.currentTimeMillis() > endTime) {
                logger.debug(String.format("node[%s] fail to start, timeout after %s seconds", serviceId, timeout));
                return false;
            }

            logger.debug(String.format("waiting for node[%s] start ...", serviceId));
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }
        } while (true);

        return true;
    }

    public boolean waitNodeExit(int timeout) {
        if (!isRunning) {
            return true;
        }

        assert serviceId != null;
        MultiNodeTestMsg msg = new MultiNodeTestMsg();
        msg.setServiceId(serviceId);
        msg.setOpCode(msg.EXIT);
        msg.setTimeout(TimeUnit.SECONDS.toMillis(60));
        long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout);
        do {
            MessageReply r = bus.call(msg);
            if (!r.isSuccess()) {
                if (r.getError().getCode().equals(SysErrors.NO_ROUTE_ERROR.toString())) {
                    break;
                } else {
                    logger.warn(String.format("node[%s] fail to exit, %s", serviceId, r.getError()));
                    return false;
                }
            }

            if (System.currentTimeMillis() > endTime) {
                logger.debug(String.format("node[%s] fail to exit, timeout after %s seconds", serviceId, timeout));
                return false;
            }

            logger.debug(String.format("waiting for node[%s] exit ...", serviceId));
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
            }
        } while (true);

        logger.debug(String.format("node[%s] exits successfully", serviceId));
        isRunning = false;
        return true;
    }

    public boolean isLoadAll() {
        return loadAll;
    }

    public void setLoadAll(boolean loadAll) {
        this.loadAll = loadAll;
    }

    public int getUuid() {
        return uuid;
    }

    public void setUuid(int uuid) {
        this.uuid = uuid;
    }

    public String getPropertyString() {
        return propertyString;
    }

    public void setPropertyString(String propertyString) {
        this.propertyString = propertyString;
    }

    public NodeRunner() {
    }

    @AsyncThread
    public void run() {
        String cmd = String.format("mvn test -Dtest=%s %s", ManagementNodeTester.class.getSimpleName(), toString());
        ProcessBuilder pb = new ProcessBuilder(Arrays.asList("/bin/bash", "-c", cmd));

        try {
            String baseDir = System.getProperty("user.dir");
            pb.directory(new File(baseDir));
            process = pb.start();
            logger.debug(String.format("run node by command[%s]", cmd));
            process.waitFor();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public boolean waitFor(int timeout) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        });
        t.run();

        try {
            t.join(TimeUnit.SECONDS.toMillis(timeout));
            if (t.isAlive()) {
                logger.warn(String.format("node[%s] is still running after %s seconds for quit, kill it", getServiceId(), timeout));
                t.interrupt();
                return false;
            } else {
                return true;
            }
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void kill() {
        assert process != null;
        Process p = process;
        process = null;
        while (true) {
            p.destroy();

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
            }

            try {
                p.exitValue();
                return;
            } catch (IllegalThreadStateException ie) {
                logger.debug(String.format("node process[%s] is still running, kill it again", getServiceId()));

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
