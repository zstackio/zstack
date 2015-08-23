package org.zstack.test;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Properties;

public class DBUtil {
    private static final CLogger logger = Utils.getLogger(DBUtil.class);
    
    public static void reDeployDB() {
        logger.info("Deploying database ...");
        String home = System.getProperty("user.dir");
        String baseDir = Utils.getPathUtil().join(home, "../");
        Properties prop = new Properties();
        
        try {
            prop.load(DBUtil.class.getClassLoader().getResourceAsStream("zstack.properties"));
            String user = prop.getProperty("DB.user");
            if (user == null) {
                user = prop.getProperty("DbFacadeDataSource.user");
            }
            if (user == null) {
                throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.user or DbFacadeDataSource.user");
            }

            String password = prop.getProperty("DB.password");
            if (password == null) {
                password = prop.getProperty("DbFacadeDataSource.password");
            }
            if (password == null) {
                throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.password or DbFacadeDataSource.password");
            }

            String shellcmd = String.format("build/deploydb.sh %s %s",  user, password);
            ShellUtils.run(shellcmd, baseDir);
            logger.info("Deploying database successfully");
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy zstack database for testing", e);
        }
    }
}

