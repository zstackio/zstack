package org.zstack.test;

import org.apache.commons.io.FileUtils;
import org.zstack.cassandra.CassandraGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.StringDSL.ln;

public class DBUtil {
    private static final CLogger logger = Utils.getLogger(DBUtil.class);
    
    public static void reDeployDB() {
        logger.info("Deploying database ...");
        String home = System.getProperty("user.dir");
        String baseDir = Utils.getPathUtil().join(home, "../");
        Properties prop = new Properties();
        
        try {
            prop.load(DBUtil.class.getClassLoader().getResourceAsStream("zstack.properties"));

            String user = System.getProperty("DB.user");
            if (user == null) {
                user = prop.getProperty("DB.user");
                if (user == null) {
                    user = prop.getProperty("DbFacadeDataSource.user");
                }
                if (user == null) {
                    throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.user or DbFacadeDataSource.user");
                }
            }

            String password = System.getProperty("DB.password");
            if (password == null) {
                password = prop.getProperty("DB.password");
                if (password == null) {
                    password = prop.getProperty("DbFacadeDataSource.password");
                }
                if (password == null) {
                    throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.password or DbFacadeDataSource.password");
                }
            }

            String shellcmd = String.format("build/deploydb.sh %s %s",  user, password);
            ShellUtils.run(shellcmd, baseDir, false);
            logger.info("Deploying database successfully");
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy zstack database for testing", e);
        }
    }

    @Deprecated
    public static void reDeployCassandra(String...keyspaces) {
        // initializing platform causes zstack.properties to be load
        Platform.getUuid();

        CassandraGlobalProperty.NEED_CONNECT_FOR_UNIT_TEST = true;

        logger.info("Redeploying cassandra");
        String cqlsh = System.getProperty("Cassandra.cqlsh");
        if (cqlsh == null) {
            throw new RuntimeException("please set Cassandra.cqlsh in zstack.properties");
        }

        if (cqlsh.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            cqlsh = cqlsh.replaceAll("~", userHome);
        }

        if (!PathUtil.exists(cqlsh)) {
            throw new RuntimeException(String.format("cannot find %s", cqlsh));
        }

        String cqlbin = System.getProperty("Cassandra.bin");
        if (cqlbin == null) {
            throw new RuntimeException("please set Cassandra.bin in zstack.properties");
        }

        if (cqlbin.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            cqlbin = cqlbin.replaceAll("~", userHome);
        }

        File cassandraHome = new File(PathUtil.join(System.getProperty("user.home"), ".cassandra"));
        if (!cassandraHome.exists()) {
            cassandraHome.mkdirs();
        }

        File cqlshrc = new File(PathUtil.join(cassandraHome.getAbsolutePath(), "cqlshrc"));
        try {
            FileUtils.writeStringToFile(cqlshrc, ln(
                    "[connection]",
                    "client_timeout = 1800"
            ).toString());
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        if (!PathUtil.exists(cqlbin)) {
            throw new RuntimeException(String.format("cannot find %s", cqlbin));
        }

        ShellResult res = ShellUtils.runAndReturn(String.format("%s -e \"describe keyspaces\"", cqlsh), false);
        if (!res.isReturnCode(0)) {
            ShellUtils.run(String.format("bash -c %s &", cqlbin), false);
            final String finalCqlsh = cqlsh;
            TimeUtils.loopExecuteUntilTimeoutIgnoreException(120, 1, TimeUnit.SECONDS, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    ShellResult res = ShellUtils.runAndReturn(String.format("%s -e \"describe keyspaces\"", finalCqlsh), false);
                    return res.isReturnCode(0);
                }
            });
        }

        for (String keyspace : keyspaces) {
            ShellUtils.run(String.format("%s --connect-timeout=100 -e \"drop keyspace if exists %s\"", cqlsh, keyspace), false);
        }

        File schemaFolder = PathUtil.findFolderOnClassPath("mevoco/cassandra/db", true);
        File deployer = PathUtil.findFileOnClassPath("deploy_cassandra_db.py", true);
        ShellUtils.run(String.format("python %s --schema-folder %s --cqlsh %s --ip 127.0.0.1",
                deployer.getAbsolutePath(), schemaFolder.getAbsolutePath(), cqlsh));
    }
}

